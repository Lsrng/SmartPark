package com.smartpark.common.aspect;

import com.google.common.util.concurrent.RateLimiter;
import com.smartpark.common.annotation.RateLimit;
import com.smartpark.common.exception.RateLimitException;
import com.smartpark.common.utils.GuavaRateLimiterManager;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 限流切面，拦截带有 @RateLimit 注解的方法
 * <p>
 * 限流策略分为三层：
 * <ol>
 *   <li><b>Redisson RRateLimiter</b>（分布式限流，首选）：</li>
 *   <li><b>断路器</b>（熔断保护）：当 Redis 连续失败达到阈值后，自动熔断，后续请求 0 延迟降级</li>
 *   <li><b>Guava RateLimiter</b>（本地限流，兜底）：Redis 不可用或熔断时，使用本地限流器</li>
 * </ol>
 * Redis 恢复后，断路器自动进入半开状态试探，成功则闭合恢复正常。
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedissonClient redissonClient;
    private final GuavaRateLimiterManager guavaRateLimiterManager;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private CircuitBreaker circuitBreaker;

    @PostConstruct
    public void init() {
        // 从注册表中获取或创建断路器实例
        circuitBreaker = circuitBreakerRegistry
                .circuitBreaker("redisCircuitBreaker");
        log.info("断路器初始化完成 - 名称: {}, 状态: {}",
                circuitBreaker.getName(), circuitBreaker.getState());
    }

    @Before("@annotation(rateLimit)")
    public void checkRateLimit(JoinPoint joinPoint, RateLimit rateLimit) {
        // 获取目标方法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 解析 SpEL 表达式，提取设备 ID
        String deviceId = parseKeyField(rateLimit.keyField(), method, joinPoint.getArgs());

        // 1. 检查断路器状态，已熔断则直接降级，0 延迟
        if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
            log.warn("断路器已熔断 - 设备ID: {}, 跳过 Redisson 直接降级 Guava", deviceId);
            fallbackToGuava(deviceId, rateLimit.rate(), rateLimit.rateInterval());
            return;
        }

        // 构造 Redis key：rate_limiter:device:{deviceId}
        String redisKey = rateLimit.prefix() + ":" + deviceId;

        // 2. 尝试使用 Redisson 分布式限流
        try {
            // 获取或创建限流器
            RRateLimiter limiter = redissonClient.getRateLimiter(redisKey);

            // 设置限流规则：首次创建时生效，已存在则不覆盖
            limiter.trySetRate(
                    org.redisson.api.RateType.PER_CLIENT,
                    rateLimit.rate(),
                    rateLimit.rateInterval(),
                    rateLimit.timeUnit().toRedissonUnit()
            );

            // 尝试获取令牌
            boolean acquired = limiter.tryAcquire();

            if (!acquired) {
                log.warn("限流命中 - 设备ID: {}, key: {} (Redisson)", deviceId, redisKey);
                throw new RateLimitException(deviceId, rateLimit.rate(), rateLimit.rateInterval());
            }

            // Redisson 调用成功，通知断路器记录成功
            circuitBreaker.onSuccess(1, TimeUnit.MILLISECONDS);
            log.debug("限流放行 - 设备ID: {}, key: {} (Redisson)", deviceId, redisKey);

        } catch (RateLimitException e) {
            // 限流异常直接抛出，不记录到断路器（属于正常业务逻辑）
            throw e;
        } catch (RedisException e) {
            // 3. Redis 连接异常，通知断路器记录失败
            log.warn("Redis 不可用 - 设备ID: {}, 原因: {}", deviceId, e.getMessage());
            circuitBreaker.onError(1, TimeUnit.MILLISECONDS, e);
            // 降级为 Guava 本地限流
            fallbackToGuava(deviceId, rateLimit.rate(), rateLimit.rateInterval());
        }
    }

    /**
     * 降级为 Guava RateLimiter 本地限流
     *
     * @param deviceId     设备 ID
     * @param rate         限流速率
     * @param rateInterval 时间间隔（秒）
     */
    private void fallbackToGuava(String deviceId, long rate, long rateInterval) {
        // 获取该设备的本地限流器
        RateLimiter guavaLimiter = guavaRateLimiterManager.getLimiter(deviceId);

        // 尝试非阻塞获取令牌
        boolean acquired = guavaLimiter.tryAcquire();

        if (!acquired) {
            log.warn("限流命中 - 设备ID: {} (Guava 降级)", deviceId);
            throw new RateLimitException(deviceId, rate, rateInterval);
        }

        log.debug("限流放行 - 设备ID: {} (Guava 降级)", deviceId);
    }

    /**
     * 解析 SpEL 表达式，从方法参数中提取设备 ID
     */
    private String parseKeyField(String keyField, Method method, Object[] args) {
        if (keyField == null || keyField.isBlank()) {
            // 默认使用第一个参数（通常是 DTO）的 deviceId 字段
            return extractDefaultDeviceId(args);
        }

        // 使用 SpEL 解析
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        if (paramNames == null) {
            return extractDefaultDeviceId(args);
        }

        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        try {
            Object value = parser.parseExpression(keyField).getValue(context);
            return value != null ? value.toString() : extractDefaultDeviceId(args);
        } catch (Exception e) {
            log.warn("SpEL 解析失败: {}, 使用默认提取方式", keyField);
            return extractDefaultDeviceId(args);
        }
    }

    /**
     * 默认提取方式：从第一个参数中反射获取 deviceId 字段
     */
    private String extractDefaultDeviceId(Object[] args) {
        if (args == null || args.length == 0) {
            return "unknown";
        }

        Object firstArg = args[0];
        if (firstArg instanceof String) {
            return (String) firstArg;
        }

        try {
            // 尝试通过反射获取 deviceId 字段
            java.lang.reflect.Field field = firstArg.getClass().getDeclaredField("deviceId");
            field.setAccessible(true);
            Object value = field.get(firstArg);
            return value != null ? value.toString() : "unknown";
        } catch (Exception e) {
            log.warn("从方法参数中提取设备ID失败，使用默认值", e);
            return "unknown";
        }
    }
}
