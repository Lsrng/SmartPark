package com.smartpark.common.utils;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guava RateLimiter 管理器
 * <p>
 * 作为 Redisson RRateLimiter 的本地兜底方案，当 Redis 不可用时自动降级为本地限流。
 * 每 10 分钟清理一次长时间未使用的限流器实例，防止内存泄漏。
 */
@Component
@Slf4j
public class GuavaRateLimiterManager {

    /**
     * 设备限流器缓存：key = 设备 ID，value = Guava RateLimiter
     */
    private final Map<String, RateLimiter> limiterMap = new ConcurrentHashMap<>();

    /**
     * 限流速率：每秒 1/60 个令牌 ≈ 每分钟 1 个
     */
    private static final double PERMITS_PER_SECOND = 1.0 / 60;

    /**
     * 获取或创建设备对应的限流器
     * <p>
     * 每个设备独立限流，互不影响。
     *
     * @param deviceId 设备 ID
     * @return 该设备对应的 RateLimiter 实例
     */
    public RateLimiter getLimiter(String deviceId) {
        return limiterMap.computeIfAbsent(deviceId, key -> {
            log.debug("创建 Guava 限流器 - 设备ID: {}", key);
            return RateLimiter.create(PERMITS_PER_SECOND);
        });
    }

    /**
     * 定时清理长时间未使用的限流器实例
     * <p>
     * 每 10 分钟执行一次，清除限流器缓存，避免内存泄漏。
     * 若设备后续继续上报，会在 getLimiter() 时重新创建。
     */
    @Scheduled(fixedRate = 600_000)
    public void cleanupInactive() {
        int size = limiterMap.size();
        if (size > 0) {
            limiterMap.clear();
            log.info("Guava 限流器缓存已清理，清理前实例数: {}", size);
        }
    }
}
