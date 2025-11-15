package com.mk.contractservice.infrastructure.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pagination")
@Getter
@Setter
@NoArgsConstructor
public class PaginationProperties {
    private int defaultPageSize;
    private int maxPageSize;
}

