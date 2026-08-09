package com.smartpark.monitor.support;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监控链路专用 Guava 令牌桶兜底限流器（Redis 不可用 / 断路器熔断期间使用）
 * <p>
 * 与主方案 Redisson RRateLimiter 同为令牌桶语义，降级前后判定行为一致。
 * </p>
 */
@Component
@Slf4j
public class MonitorGuavaLimiterManager {

    private final Map<String, RateLimiter> limiterMap = new ConcurrentHashMap<>();

    /**
     * 获取（或创建）key 对应的本地限流器
     *
     * @param key              监控维度 key
     * @param permitsPerSecond 每秒令牌速率（rate = limit / windowSeconds）
     */
    public RateLimiter getLimiter(String key, double permitsPerSecond) {
        return limiterMap.computeIfAbsent(key, k -> RateLimiter.create(permitsPerSecond));
    }

    /**
     * 每 10 分钟清理不活跃 limiter，防止长时间无 Redis 时实例累积
     * （兜底窗口数量有限，达到上限时整体重建即可）
     */
    @Scheduled(fixedRate = 600_000)
    public void cleanup() {
        if (limiterMap.size() > 1000) {
            limiterMap.clear();
            log.info("monitor Guava 兜底限流器实例超上限，已清理重建");
        }
    }
}
