package com.mk.contractservice.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pagination Properties Tests")
class PaginationPropertiesTest {

    @Test
    @DisplayName("Should create valid pagination properties")
    void shouldCreateValidPaginationProperties() {
        PaginationProperties props = new PaginationProperties();
        props.setDefaultPageSize(20);
        props.setMaxPageSize(100);

        assertThat(props.getDefaultPageSize()).isEqualTo(20);
        assertThat(props.getMaxPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should allow any configuration values (validation happens at request time)")
    void shouldAllowAnyConfigurationValues() {
        PaginationProperties props = new PaginationProperties();
        props.setDefaultPageSize(500);
        props.setMaxPageSize(1000);

        assertThat(props.getDefaultPageSize()).isEqualTo(500);
        assertThat(props.getMaxPageSize()).isEqualTo(1000);
    }
}
