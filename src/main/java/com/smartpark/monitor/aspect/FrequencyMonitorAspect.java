package com.smartpark.monitor.aspect;

import com.google.common.util.concurrent.RateLimiter;
import com.smartpark.common.context.BaseContext;
import com.smartpark.monitor.annotation.FrequencyMonitor;
import com.smartpark.monitor.config.MonitorProperties;
import com.smartpark.monitor.support.AlertCoordinator;
import com.smartpark.monitor.support.MonitorGuavaLimiterManager;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 敏感接口频率旁路监控切面
 * <p>
 * 旁路语义：只"观测 + 告警"，默认不拦截业务；监控自身任何异常只记日志，绝不污染业务。
 * <br>
 * 判定链路：Redisson RRateLimiter 令牌桶（主）→ 独立断路器熔断 → Guava 令牌桶（兜底，语义一致）。
 * </p>
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class FrequencyMonitorAspect {

    private final RedissonClient redissonClient;
    private final MonitorGuavaLimiterManager guavaManager;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final AlertCoordinator alertCoordinator;
    private final MonitorProperties properties;

    /** 缓存解析好的 SpEL 表达式（key = 表达式字符串），避免每次反射 + 重复 parse */
    private final Map<String, Expression> spElCache = new ConcurrentHashMap<>();
    /** 速率配置标记：trySetRate 仅首次执行，正常流量每请求仅 1 次 Redis 往返 */
    private final ConcurrentHashMap<String, Boolean> configured = new ConcurrentHashMap<>();

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private CircuitBreaker circuitBreaker;

    @PostConstruct
    public void init() {
        // 监控链路专用断路器实例（独立于其他断路器）
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("monitorRedisCircuitBreaker");
        log.info("monitor 断路器初始化 - name: {}, state: {}",
                circuitBreaker.getName(), circuitBreaker.getState());
    }

    @Around("@annotation(monitor)")
    public Object around(ProceedingJoinPoint pjp, FrequencyMonitor monitor) throws Throwable {
        if (!properties.isEnabled()) {
            return pjp.proceed();
        }
        try {
            // @Repeatable 多维度循环：账号维度 + 接口级全局水位
            for (FrequencyMonitor fm : getAllAnnotations(pjp)) {
                String key = resolveKey(fm, pjp);
                judgeAndAlert(fm, key, fm.limit(), fm.windowSeconds());
                if (fm.globalAlert()) {
                    // 全局水位：独立 key + 独立阈值，防多账号并发批量拉取绕过单账号水位
                    judgeAndAlert(fm, fm.prefix() + ":global", fm.globalLimit(), fm.globalWindowSeconds());
                }
            }
        } catch (Throwable t) {
            // 旁路铁律：仅吞监控自身异常，绝不污染业务
            log.warn("旁路监控异常: err={}", t.getMessage());
        }
        // 业务照常执行；业务自身抛出的异常原样向上抛出，绝不重放
        return pjp.proceed();
    }

    /** 判定 + 超线告警（rate 由调用方决定：账号维度或全局水位） */
    private void judgeAndAlert(FrequencyMonitor fm, String key, long limit, long windowSeconds) {
        if (judge(fm, key, limit, windowSeconds).overLimit) {
            alertCoordinator.onOverLimit(fm, key);
        }
    }

    /** 取出方法上叠加的全部监控注解（@Repeatable 多维度支持） */
    private FrequencyMonitor[] getAllAnnotations(ProceedingJoinPoint pjp) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        return method.getAnnotationsByType(FrequencyMonitor.class);
    }

    /** 主判定：Redisson 令牌桶；熔断/异常时 Guava 令牌桶兜底（语义一致） */
    private JudgeResult judge(FrequencyMonitor fm, String key, long limit, long windowSeconds) {
        if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
            log.warn("monitor 断路器已熔断 - key: {}, 0 延迟走 Guava 兜底", key);
            return judgeByGuava(fm, key, limit, windowSeconds);
        }
        try {
            RRateLimiter limiter = redissonClient.getRateLimiter(key);
            if (configured.putIfAbsent(key, Boolean.TRUE) == null) {
                // 仅首次配置速率（内部一次 Lua），避免每次请求多 1 次往返
                limiter.trySetRate(RateType.PER_CLIENT, limit, windowSeconds, RateIntervalUnit.SECONDS);
            }
            boolean overLimit = !limiter.tryAcquire();   // 权威判定：有令牌放行，无令牌即超线
            circuitBreaker.onSuccess(1, TimeUnit.MILLISECONDS);
            if (overLimit) {
                log.warn("监控超线 - key: {}", key);
            }
            return new JudgeResult(overLimit);
        } catch (Exception e) {
            // 宽泛捕获：Redis 故障（RedisException 系）+ 桶配置丢失（Redis 重启/清理后抛非 RedisException）；
            // 移除配置标记，下次请求重新 trySetRate 自愈
            log.warn("monitor Redis 判定异常 - key: {}, 走 Guava 兜底, err: {}", key, e.getMessage());
            circuitBreaker.onError(1, TimeUnit.MILLISECONDS, e);
            configured.remove(key);
            return judgeByGuava(fm, key, limit, windowSeconds);
        }
    }

    /** 兜底：Guava 令牌桶，速率换算 rate = limit / windowSeconds */
    private JudgeResult judgeByGuava(FrequencyMonitor fm, String key, long limit, long windowSeconds) {
        RateLimiter limiter = guavaManager.getLimiter(key, (double) limit / windowSeconds);
        return new JudgeResult(!limiter.tryAcquire());
    }

    /** 解析监控维度 key：prefix + 维度值（账号或自定义维度） */
    private String resolveKey(FrequencyMonitor fm, ProceedingJoinPoint pjp) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String value = parseKeyField(fm.keyField(), method, pjp.getArgs());
        return fm.prefix() + ":account:" + value;
    }

    /** SpEL 求值：上下文注册方法参数 + #userId（登录上下文，无需方法参数传账号） */
    private String parseKeyField(String keyField, Method method, Object[] args) {
        if (keyField == null || keyField.isBlank()) {
            return "unknown";
        }
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        context.setVariable("userId", BaseContext.getCurrentId());
        try {
            // 表达式缓存命中时仅求值（~500ns），避免重复 parse
            Expression expression = spElCache.computeIfAbsent(keyField, parser::parseExpression);
            Object value = expression.getValue(context);
            return value != null ? value.toString() : "unknown";
        } catch (Exception e) {
            log.warn("SpEL 解析失败: {}, 使用 unknown", keyField);
            return "unknown";
        }
    }

    /** 判定结果：是否超线 */
    @RequiredArgsConstructor
    private static class JudgeResult {
        private final boolean overLimit;
    }
}
