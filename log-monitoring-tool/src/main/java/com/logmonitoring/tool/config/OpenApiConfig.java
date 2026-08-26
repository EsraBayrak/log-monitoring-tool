package com.logmonitoring.tool.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Oracle & SSH Log Monitoring Tool API")
                        .version("1.0.0")
                        .description("Sunucu log izleme, uzaktan SFTP dosya yönetimi ve sistem metrikleri REST API dokümantasyonu.")
                        .contact(new Contact()
                                .name("Esra Bayrak")
                                .url("https://github.com/EsraBayrak/log-monitoring-tool")));
    }
}