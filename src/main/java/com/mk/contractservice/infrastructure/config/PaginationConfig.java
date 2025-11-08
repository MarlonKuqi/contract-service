package com.mk.contractservice.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
@EnableConfigurationProperties(PaginationProperties.class)
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class PaginationConfig {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableResolverCustomizer(
            PaginationProperties paginationProperties) {
        return pageableResolver -> {
            pageableResolver.setFallbackPageable(
                    PageRequest.of(0, paginationProperties.defaultPageSize(), Sort.by(Sort.Direction.DESC, "lastModified"))
            );
            pageableResolver.setMaxPageSize(paginationProperties.maxPageSize());
        };
    }
}
