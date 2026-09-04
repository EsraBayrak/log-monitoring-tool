package com.logmonitoring.tool;

import com.logmonitoring.tool.model.ServerEnvironment;
import com.logmonitoring.tool.repository.ServerEnvironmentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableCaching
public class LogMonitoringToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogMonitoringToolApplication.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(ServerEnvironmentRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(ServerEnvironment.builder()
                        .name("Test 1")
                        .host("10.248.67.233")
                        .port(22)
                        .username("testuser")
                        .password("password123")
                        .logDirectoryPath("/appdata/15c/Oracle/Middleware/Oracle_Home/user_projects/domains/oim_fonk/servers/oim_m1/logs")
                        .logFilePath("/appdata/15c/Oracle/Middleware/Oracle_Home/user_projects/domains/oim_fonk/servers/oim_m1/logs/oim_m1.out")
                        .configFilePath("/appdata/15c/Oracle/Middleware/Oracle_Home/user_projects/domains/oim_fonk/servers/oim_m1/logs/logback.xml")
                        .build());

                repository.save(ServerEnvironment.builder()
                        .name("Test 2")
                        .host("10.248.67.234")
                        .port(22)
                        .username("testuser")
                        .password("password123")
                        .logDirectoryPath("/appdata/15c/Oracle/Middleware/Oracle_Home/user_projects/domains/oim_fonk/servers/oim_m1/logs")
                        .logFilePath("/appdata/15c/Oracle/Middleware/Oracle_Home/user_projects/domains/oim_fonk/servers/oim_m1/logs/oim_m1.out")
                        .configFilePath("/appdata/15c/Oracle/Middleware/Oracle_Home/user_projects/domains/oim_fonk/servers/oim_m1/logs/logback.xml")
                        .build());
            }
        };
    }
}