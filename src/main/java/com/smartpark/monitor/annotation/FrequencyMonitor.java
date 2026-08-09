package com.smartpark.monitor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感接口频率旁路监控注解（可重复，支持账号 + 全局水位等多维度叠加）
 * <p>
 * 标注在敏感接口方法上，切面 {@code FrequencyMonitorAspect} 在业务执行前做一次
 * 频率判定（Redisson RRateLimiter 令牌桶），超线触发异步短信告警，默认不拦截业务。
 * </p>
 */
@Repeatable(FrequencyMonitors.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FrequencyMonitor {

    /** 监控组前缀（接口级标识），最终 Redis key 以 monitor: 开头 */
    String prefix() default "monitor";

    /** SpEL 提取监控维度，如 #userId（登录上下文），或方法参数如 #id */
    String keyField();

    /** 窗口内次数上限（异常告警水位，非业务限制频率），超线即告警 */
    long limit() default 30;

    /** 时间窗时长（秒） */
    long windowSeconds() default 60;

    /** 是否启用接口级全局水位（防多账号并发批量拉取绕过单账号水位） */
    boolean globalAlert() default false;

    /** 全局水位上限（globalAlert=true 时生效），独立于单账号水位 */
    long globalLimit() default 0;

    /** 全局水位时间窗（秒），与 globalLimit 成对 */
    long globalWindowSeconds() default 0;
}
