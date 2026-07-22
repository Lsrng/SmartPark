package com.smartpark.common.exception;

import lombok.Getter;

/**
 * 限流异常，当设备请求频率超过限制时抛出
 */
@Getter
public class RateLimitException extends RuntimeException {

    /**
     * 被限流的设备 ID
     */
    private final String deviceId;

    /**
     * 限流速率
     */
    private final long rate;

    /**
     * 限流时间间隔（秒）
     */
    private final long rateInterval;

    public RateLimitException(String deviceId, long rate, long rateInterval) {
        super(String.format("设备 %s 上报过于频繁，请稍后重试（限制：每分钟 %d 次）", deviceId, rate));
        this.deviceId = deviceId;
        this.rate = rate;
        this.rateInterval = rateInterval;
    }
}
