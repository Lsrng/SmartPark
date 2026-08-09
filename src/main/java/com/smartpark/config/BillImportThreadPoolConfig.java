package com.smartpark.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 物业账单导入线程池配置
 * <p>
 * 使用独立的线程池隔离导入任务，与 Tomcat 线程池解耦。
 * 核心线程数 = 最大线程数 = 2 * CPU核数，队列容量 100，拒绝策略 CallerRunsPolicy。
 * </p>
 */
@Configuration
@EnableAsync
@Slf4j
public class BillImportThreadPoolConfig {

    @Bean("billImportExecutor")
    public org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor billImportExecutor() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor =
                new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();

        int cpuCores = Runtime.getRuntime().availableProcessors();
        int threadCount = cpuCores * 2;
        // 核心线程数 = 2 * CPU 核数
        executor.setCorePoolSize(threadCount);
        // 最大线程数 = 2 * CPU 核数（与核心线程数相同，全部为常驻线程）
        executor.setMaxPoolSize(threadCount);
        // 等待队列容量，最多 100 个任务排队
        executor.setQueueCapacity(100);
        // 空闲线程存活时间（秒），导入为 IO 密集型，设长一些
        executor.setKeepAliveSeconds(120);
        // 线程名前缀，方便日志排查
        executor.setThreadNamePrefix("bill-import-");
        // 拒绝策略：队列和线程池都满时，由提交线程（Tomcat 线程）自己执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅关闭：等待正在执行的任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 最多等待 30 秒
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("物业账单导入线程池已初始化 - corePoolSize={}, maxPoolSize={}, queueCapacity=100, cpuCores={}",
                threadCount, threadCount, cpuCores);
        return executor;
    }
}