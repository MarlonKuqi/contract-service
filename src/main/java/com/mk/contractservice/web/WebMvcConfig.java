package com.mk.contractservice.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(final ApiVersionConfigurer configurer) {
        configurer
                .usePathSegment(0)
                .addSupportedVersions("2")
                .setDefaultVersion("2");
    }

}

