package com.logmonitoring.tool.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "logStreamExecutor")
    public Executor logStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);       // 10 eşzamanlı sunucu stream'i
        executor.setMaxPoolSize(20);        // Yoğunlukta 20 thread'e kadar çıkabilir
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("LogStream-");
        executor.initialize();
        return executor;
    }
}