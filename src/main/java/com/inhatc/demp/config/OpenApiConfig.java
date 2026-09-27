package com.inhatc.demp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI dempOpenApi() {
        return new OpenAPI().info(new Info().title("DEMP API").description("DEMP API Docs").version("1"));
    }
}
