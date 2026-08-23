package com.diabetes.monitor.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * 异步任务配置类
 * 配置 AI 调用和通用异步任务的线程池
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * AI HTTP调用线程池（I/O 密集型）
     * 核心线程：4，最大线程，队列容量
     */
    @Bean(name = "aiExecutor")
    public ThreadPoolTaskExecutor aiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        //核心线程数
        executor.setCorePoolSize(4);
        //最大线程数
        executor.setMaxPoolSize(10);
        //队列容量
        executor.setQueueCapacity(50);
        //线程名前缀
        executor.setThreadNamePrefix("ai-exec-");
        //等待任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        //等待时间
        executor.setAwaitTerminationSeconds(60);
        //拒绝策略:队列满时由调用线程执行
        ThreadPoolExecutor.CallerRunsPolicy rejectionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        executor.setRejectedExecutionHandler(rejectionHandler);
        //初始化线程池
        executor.initialize();
        return executor;
    }


    /**
     * 通用异步任务线程池（PDF 导出等）
     * 核心线程：2，最大线程：5，队列容量：20
     */
    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(2);
        // 最大线程数
        executor.setMaxPoolSize(5);
        // 队列容量
        executor.setQueueCapacity(50);
        // 线程名前缀
        executor.setThreadNamePrefix("task-exec-");
        // 优雅关闭：等待任务完成后再关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间（可选，单位：秒）
        executor.setAwaitTerminationSeconds(60);

        // 拒绝策略：队列满时由调用线程执行，不丢任务、不抛异常
        ThreadPoolExecutor.CallerRunsPolicy callerRunsPolicy = new ThreadPoolExecutor.CallerRunsPolicy();
        executor.setRejectedExecutionHandler(callerRunsPolicy);

        // 初始化线程池
        executor.initialize();
        return executor;
    }

    /**
     * AI 对话持久化线程池（Redis / MySQL 保存）
     * 队列满时静默丢弃，保证持久化失败不影响 AI 主流程
     */
    @Bean(name = "persistenceExecutor")
    public ThreadPoolTaskExecutor persistenceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("persist-exec-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // 拒绝策略：队列满时静默丢弃，不抛异常
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }

}
