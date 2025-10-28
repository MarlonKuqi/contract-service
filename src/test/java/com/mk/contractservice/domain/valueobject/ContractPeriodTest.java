package com.mk.contractservice.domain.valueobject;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ContractPeriod - Business Rules Tests")
class ContractPeriodTest {

    @Test
    @DisplayName("GIVEN valid start and end dates WHEN of() THEN create ContractPeriod")
    void shouldCreateWithValidDates() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusDays(30);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("GIVEN null start date WHEN of() THEN default to now")
    void shouldDefaultStartDateToNow() {
        OffsetDateTime before = OffsetDateTime.now();
        ContractPeriod period = ContractPeriod.of(null, null);
        OffsetDateTime after = OffsetDateTime.now();

        assertThat(period.startDate())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("GIVEN null end date WHEN of() THEN accept (active contract)")
    void shouldAcceptNullEndDate() {
        OffsetDateTime start = OffsetDateTime.now();

        ContractPeriod period = ContractPeriod.of(start, null);

        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isNull();
    }

    @Test
    @DisplayName("GIVEN end date before start date WHEN of() THEN throw exception")
    void shouldRejectEndDateBeforeStart() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.minusDays(1);

        assertThatThrownBy(() -> ContractPeriod.of(start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Contract end date must be after start date");
    }

    @Test
    @DisplayName("GIVEN end date equal to start date WHEN of() THEN throw exception")
    void shouldRejectEndDateEqualToStart() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start;

        assertThatThrownBy(() -> ContractPeriod.of(start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Contract end date must be after start date");
    }

    @Test
    @DisplayName("GIVEN end date after start date WHEN of() THEN accept")
    void shouldAcceptEndDateAfterStart() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusSeconds(1);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.endDate()).isAfter(period.startDate());
    }

    @Test
    @DisplayName("GIVEN contract with null end date WHEN isActiveAt() THEN return true")
    void shouldBeActiveWhenEndDateIsNull() {
        OffsetDateTime start = OffsetDateTime.now().minusDays(10);
        ContractPeriod period = ContractPeriod.of(start, null);

        boolean active = period.isActiveAt(OffsetDateTime.now());

        assertThat(active).isTrue();
    }

    @Test
    @DisplayName("GIVEN current date before end date WHEN isActiveAt() THEN return true")
    void shouldBeActiveWhenBeforeEndDate() {
        OffsetDateTime start = OffsetDateTime.now().minusDays(10);
        OffsetDateTime end = OffsetDateTime.now().plusDays(10);
        ContractPeriod period = ContractPeriod.of(start, end);

        boolean active = period.isActiveAt(OffsetDateTime.now());

        assertThat(active).isTrue();
    }

    @Test
    @DisplayName("GIVEN current date after end date WHEN isActiveAt() THEN return false")
    void shouldNotBeActiveWhenAfterEndDate() {
        OffsetDateTime start = OffsetDateTime.now().minusDays(20);
        OffsetDateTime end = OffsetDateTime.now().minusDays(10);
        ContractPeriod period = ContractPeriod.of(start, end);

        boolean active = period.isActiveAt(OffsetDateTime.now());

        assertThat(active).isFalse();
    }

    @Test
    @DisplayName("GIVEN current date equal to end date WHEN isActiveAt() THEN return false")
    void shouldNotBeActiveWhenEqualToEndDate() {
        OffsetDateTime start = OffsetDateTime.now().minusDays(10);
        OffsetDateTime end = OffsetDateTime.now();
        ContractPeriod period = ContractPeriod.of(start, end);

        boolean active = period.isActiveAt(end);

        assertThat(active).isFalse();
    }
}

