package com.smartpark.common.annotation;

/**
 * 限流时间间隔单位枚举
 */
public enum RateIntervalUnit {

    SECONDS,
    MINUTES,
    HOURS;

    /**
     * 转换为 Redisson 的 RateIntervalUnit
     */
    public org.redisson.api.RateIntervalUnit toRedissonUnit() {
        return switch (this) {
            case SECONDS -> org.redisson.api.RateIntervalUnit.SECONDS;
            case MINUTES -> org.redisson.api.RateIntervalUnit.MINUTES;
            case HOURS -> org.redisson.api.RateIntervalUnit.HOURS;
        };
    }
}
