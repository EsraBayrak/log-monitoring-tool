package com.logmonitoring.tool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class LogMonitoringToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogMonitoringToolApplication.class, args);
    }
}