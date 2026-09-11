package com.logmonitoring.tool.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/logMonitoring").setViewName("forward:/index.html");
        registry.addViewController("/logMonitoring/").setViewName("forward:/index.html");
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}