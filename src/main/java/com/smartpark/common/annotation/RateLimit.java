package com.smartpark.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解，标注在需要进行限流的 Service 方法上
 * <p>
 * 通过 AOP 切面配合 Redisson RRateLimiter 实现分布式限流。
 * 默认限流策略为：每个设备每分钟最多 1 次请求。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流 key 的前缀
     */
    String prefix() default "rate_limiter:device";

    /**
     * 限流速率，单位时间内最多允许的请求次数
     */
    long rate() default 1;

    /**
     * 时间间隔
     */
    long rateInterval() default 60;

    /**
     * 时间间隔单位，默认秒
     */
    RateIntervalUnit timeUnit() default RateIntervalUnit.SECONDS;

    /**
     * 从方法参数中提取设备 ID 的 SpEL 表达式
     * 例如：#dto.deviceId
     */
    String keyField() default "";
}
