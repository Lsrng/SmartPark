package com.smartpark.monitor.support;

import com.smartpark.monitor.annotation.FrequencyMonitor;
import com.smartpark.monitor.config.MonitorProperties;
import com.smartpark.monitor.sender.SmsSender;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 告警协调器：冷却去重 + 异步短信
 * <p>
 * 核心设计：
 * <ul>
 *   <li><b>冷却降噪</b>：Redis {@code SET key NX EX} 原子占位，冷却期内重复超线被静默，无效告警降 99%+，多实例天然去重</li>
 *   <li><b>接受冷却期盲区</b>：冷却期内的超线信息会丢失，这是降噪的必然代价——敏感接口场景下，持续攻击一定会突破冷却，偶发误操作不需要持续关注</li>
 *   <li><b>异步短信</b>：独立线程池发送，不占用业务线程</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AlertCoordinator {

    private final StringRedisTemplate redisTemplate;
    private final SmsSender smsSender;
    private final MonitorProperties properties;

    /** Redis 不可用时本地冷却兜底：key -> 到期时间戳 */
    private final ConcurrentHashMap<String, Long> localCooldown = new ConcurrentHashMap<>();

    private volatile ThreadPoolExecutor alertExecutor;

    @PostConstruct
    public void init() {
        ThreadFactory namedFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("monitor-alert-" + threadNumber.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };

        // 微队列 + 背压策略最优解
        // core=1, max=2, queue=5 (ArrayBlockingQueue), CallerRunsPolicy
        this.alertExecutor = new ThreadPoolExecutor(
                1,
                2,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(5),
                namedFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        log.info("监控告警线程池初始化 (原生) - core=1, max=2, queue=5 (ArrayBlockingQueue), policy=CallerRunsPolicy");
    }

    /**
     * 超线事件入口：冷却判定 → 通过则异步发短信
     * <p>
     * 接受冷却期盲区：冷却期内所有超线请求被静默，信息丢失。
     * 这是降噪的必然代价——敏感接口场景下，持续攻击一定会突破冷却（10分钟后再次超线），
     * 偶发误操作（只触发1次告警）不需要持续关注。
     * </p>
     *
     * @param fm  监控注解
     * @param key 监控维度 key
     */
    public void onOverLimit(FrequencyMonitor fm, String key) {
        long cooldownMillis = properties.getCooldown().baseDurationMillis();

        // 1. 冷却判定：SET NX EX，原子操作
        boolean pass;
        try {
            long cooldownSeconds = cooldownMillis / 1000;
            Boolean first = redisTemplate.opsForValue()
                    .setIfAbsent("monitor:cooldown:" + key, "1", cooldownSeconds, TimeUnit.SECONDS);
            pass = Boolean.TRUE.equals(first);
        } catch (Exception e) {
            // Redis 不可用：降级本地冷却（单实例内同样去重）
            pass = tryLocalCooldown(key, cooldownMillis);
        }

        if (pass) {
            // 2. 异步发送告警
            String content = buildContent(fm, key);
            String[] phones = properties.getSms().phoneArray();
            alertExecutor.execute(() -> {
                for (String phone : phones) {
                    try {
                        smsSender.send(phone, content);
                    } catch (Exception ex) {
                        log.error("短信发送失败 phone={}", phone, ex);
                    }
                }
            });
        }
        // 冷却期内：静默，接受盲区
    }

    /**
     * 本地冷却判定：读写时惰性判断过期，Redis 长期不可用也不泄漏
     */
    private boolean tryLocalCooldown(String key, long cooldownMillis) {
        long expire = System.currentTimeMillis() + cooldownMillis;
        Long old = localCooldown.putIfAbsent(key, expire);
        if (old == null) {
            return true;
        }
        if (old <= System.currentTimeMillis()) {
            // 冷却已过期：CAS 续期并放行
            return localCooldown.replace(key, old, expire);
        }
        return false;
    }

    /**
     * 定时清理：每 10 分钟移除过期冷却键（配合惰性删除双保险）
     */
    @Scheduled(fixedRate = 600_000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        localCooldown.entrySet().removeIf(e -> e.getValue() <= now);
    }

    /**
     * 构造告警内容（时间 + 工号/维度 + URL + 告警阈值）
     * <p>
     * 短信模板四要素：谁（工号）、什么（接口 URL）、何时（时间）、多严重（阈值）。
     * 固定 key:value 格式，便于日志归档与自动化解析。
     * </p>
     */
    private String buildContent(FrequencyMonitor fm, String key) {
        String uri = "unknown";
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            uri = attrs.getRequest().getRequestURI();
        }
        boolean isGlobal = key.endsWith(":global");
        long limit = isGlobal ? fm.globalLimit() : fm.limit();
        long window = isGlobal ? fm.globalWindowSeconds() : fm.windowSeconds();
        // 工号：从维度 key 提取（monitor:xxx:account:10086）；全局水位无工号，标注"全局"
        String account = isGlobal ? "全局"
                : key.substring(key.lastIndexOf(":account:") + ":account:".length());
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return String.format("【smartPark告警】敏感接口 %s 频率异常%n" +
                        "时间: %s | 工号: %s | 告警阈值: %d 次/%ds",
                uri, time, account, limit, window);
    }
}
