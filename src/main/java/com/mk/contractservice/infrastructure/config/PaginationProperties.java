package com.mk.contractservice.infrastructure.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.pagination")
@Validated
public record PaginationProperties(
        @Min(1)
        @Max(100)
        int defaultPageSize,

        @Min(1)
        @Max(1000)
        int maxPageSize
) {
    public PaginationProperties {
        if (defaultPageSize > maxPageSize) {
            throw new IllegalArgumentException(
                    "defaultPageSize (%d) cannot be greater than maxPageSize (%d)"
                            .formatted(defaultPageSize, maxPageSize)
            );
        }
    }
}

