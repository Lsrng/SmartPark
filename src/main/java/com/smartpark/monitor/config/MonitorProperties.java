package com.smartpark.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 敏感接口频率旁路监控配置（对应 yaml 前缀 smartpark.monitor）
 * <p>
 * 包含：功能开关、冷却机制（时长）、短信通道（收件人 + 异步线程池）。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "smartpark.monitor")
public class MonitorProperties {

    /** 总开关：false 时切面直接放行，监控整体下线 */
    private boolean enabled = true;

    /** 冷却机制配置 */
    private Cooldown cooldown = new Cooldown();

    /** 短信通道配置 */
    private Sms sms = new Sms();

    @Data
    public static class Cooldown {
        /** 冷却策略：fixed（固定时长）/ exponential（指数退避） */
        private String strategy = "fixed";
        /** 基础冷却时长（默认 10 分钟） */
        private Duration baseDuration = Duration.ofMinutes(10);
        /** 指数退避上限（默认 60 分钟） */
        private Duration maxDuration = Duration.ofMinutes(60);

        public long baseDurationSeconds() {
            return baseDuration.toSeconds();
        }

        public long baseDurationMillis() {
            return baseDuration.toMillis();
        }
    }

    @Data
    public static class Sms {
        /** 告警短信收件人 */
        private List<String> phones = new ArrayList<>();
        /** 短信异步发送线程池 */
        private ThreadPool threadPool = new ThreadPool();

        public String[] phoneArray() {
            return phones.toArray(new String[0]);
        }
    }

    @Data
    public static class ThreadPool {
        private int core = 2;
        private int max = 4;
        private int queueCapacity = 1000;
        /** 拒绝策略：caller-runs（队列满时由提交线程执行，不丢告警） */
        private String rejectionPolicy = "caller-runs";
    }
}
