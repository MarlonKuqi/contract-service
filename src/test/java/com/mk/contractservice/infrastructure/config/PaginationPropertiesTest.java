package com.mk.contractservice.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pagination Properties Tests")
class PaginationPropertiesTest {

    @Test
    @DisplayName("Should create valid pagination properties")
    void shouldCreateValidPaginationProperties() {
        PaginationProperties props = new PaginationProperties(20, 100);

        assertThat(props.defaultPageSize()).isEqualTo(20);
        assertThat(props.maxPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should reject defaultPageSize greater than maxPageSize")
    void shouldRejectDefaultGreaterThanMax() {
        assertThatThrownBy(() -> new PaginationProperties(100, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultPageSize (100) cannot be greater than maxPageSize (50)");
    }

    @Test
    @DisplayName("Should accept defaultPageSize equal to maxPageSize")
    void shouldAcceptDefaultEqualToMax() {
        PaginationProperties props = new PaginationProperties(50, 50);

        assertThat(props.defaultPageSize()).isEqualTo(50);
        assertThat(props.maxPageSize()).isEqualTo(50);
    }
}

