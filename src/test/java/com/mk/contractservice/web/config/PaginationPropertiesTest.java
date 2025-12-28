package com.mk.contractservice.web.config;

import com.mk.contractservice.infrastructure.web.contract.pagination.PaginationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pagination Config Tests")
class PaginationPropertiesTest {

    @Test
    @DisplayName("Should create valid pagination configuration")
    void shouldCreateValidPaginationProperties() {
        PaginationConfig config = new PaginationConfig();
        config.setDefaultPageSize(20);
        config.setMaxPageSize(100);

        assertThat(config.getDefaultPageSize()).isEqualTo(20);
        assertThat(config.getMaxPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should allow any configuration values (validation happens at request time)")
    void shouldAllowAnyConfigurationValues() {
        PaginationConfig config = new PaginationConfig();
        config.setDefaultPageSize(500);
        config.setMaxPageSize(1000);

        assertThat(config.getDefaultPageSize()).isEqualTo(500);
        assertThat(config.getMaxPageSize()).isEqualTo(1000);
    }
}
