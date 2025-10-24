package com.mk.contractservice.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI contractServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Contract Service API")
                        .version("v1")
                        .description("REST API for managing clients and their contracts"));
    }
}