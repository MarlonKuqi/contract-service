package com.mk.contractservice.controllers.shared.documentation;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI contractServiceOpenAPI(@Value("${info.app.version}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("Contract Service API")
                        .version(version)
                        .description("REST API for managing clients and their contracts"));
    }
}

