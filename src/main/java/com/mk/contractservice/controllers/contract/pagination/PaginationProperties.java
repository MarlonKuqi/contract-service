package com.mk.contractservice.controllers.contract.pagination;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pagination")
public record PaginationProperties(int defaultPageSize, int maxPageSize) {
}

