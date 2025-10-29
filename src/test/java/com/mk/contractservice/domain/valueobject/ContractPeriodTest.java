package com.mk.contractservice.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ContractPeriod - Business Rules Tests")
class ContractPeriodTest {

    @Test
    @DisplayName("GIVEN start and end dates WHEN of() THEN create ContractPeriod")
    void shouldCreateWithBothDates() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusDays(30);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("GIVEN null start date WHEN of() THEN use current date")
    void shouldUseCurrentDateWhenStartIsNull() {
        OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);
        OffsetDateTime end = OffsetDateTime.now().plusDays(30);

        ContractPeriod period = ContractPeriod.of(null, end);
        OffsetDateTime after = OffsetDateTime.now().plusSeconds(1);

        assertThat(period.startDate()).isNotNull();
        assertThat(period.startDate()).isBetween(before, after);
        assertThat(period.endDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("GIVEN null end date WHEN of() THEN create period with null end")
    void shouldAcceptNullEndDate() {
        OffsetDateTime start = OffsetDateTime.now();

        ContractPeriod period = ContractPeriod.of(start, null);

        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isNull();
    }

    @Test
    @DisplayName("GIVEN both null dates WHEN of() THEN create with current start and null end")
    void shouldAcceptBothNullDates() {
        OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);

        ContractPeriod period = ContractPeriod.of(null, null);
        OffsetDateTime after = OffsetDateTime.now().plusSeconds(1);

        assertThat(period.startDate()).isNotNull();
        assertThat(period.startDate()).isBetween(before, after);
        assertThat(period.endDate()).isNull();
    }

    @Test
    @DisplayName("GIVEN end before start WHEN of() THEN throw exception")
    void shouldRejectEndBeforeStart() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.minusDays(1);

        assertThatThrownBy(() -> ContractPeriod.of(start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Contract end date must be after start date");
    }

    @Test
    @DisplayName("GIVEN end equal to start WHEN of() THEN throw exception")
    void shouldRejectEndEqualToStart() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start;

        assertThatThrownBy(() -> ContractPeriod.of(start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Contract end date must be after start date");
    }

    @Test
    @DisplayName("GIVEN end 1 second after start WHEN of() THEN accept")
    void shouldAcceptEndJustAfterStart() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusSeconds(1);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("GIVEN period with null end WHEN isActive() THEN return true")
    void shouldBeActiveWhenEndIsNull() {
        OffsetDateTime start = OffsetDateTime.now().minusDays(1);

        ContractPeriod period = ContractPeriod.of(start, null);

        assertThat(period.isActive()).isTrue();
    }

    @Test
    @DisplayName("GIVEN period with future end WHEN isActive() THEN return true")
    void shouldBeActiveWhenEndInFuture() {
        OffsetDateTime start = OffsetDateTime.now().minusDays(1);
        OffsetDateTime end = OffsetDateTime.now().plusDays(1);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.isActive()).isTrue();
    }

    @Test
    @DisplayName("GIVEN period with past end WHEN isActive() THEN return false")
    void shouldNotBeActiveWhenEndInPast() {
        OffsetDateTime start = OffsetDateTime.now().minusDays(10);
        OffsetDateTime end = OffsetDateTime.now().minusDays(1);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.isActive()).isFalse();
    }

    @Test
    @DisplayName("GIVEN period WHEN isActiveAt(date before end) THEN return true")
    void shouldBeActiveAtDateBeforeEnd() {
        OffsetDateTime start = OffsetDateTime.now().minusDays(10);
        OffsetDateTime end = OffsetDateTime.now().plusDays(10);
        OffsetDateTime referenceDate = OffsetDateTime.now();

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.isActiveAt(referenceDate)).isTrue();
    }

    @Test
    @DisplayName("GIVEN period WHEN isActiveAt(date after end) THEN return false")
    void shouldNotBeActiveAtDateAfterEnd() {
        OffsetDateTime start = OffsetDateTime.now().minusDays(10);
        OffsetDateTime end = OffsetDateTime.now().minusDays(5);
        OffsetDateTime referenceDate = OffsetDateTime.now();

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.isActiveAt(referenceDate)).isFalse();
    }

    @Test
    @DisplayName("GIVEN period with null end WHEN isActiveAt(any date) THEN return true")
    void shouldBeActiveAtAnyDateWhenEndIsNull() {
        OffsetDateTime start = OffsetDateTime.now().minusDays(10);
        OffsetDateTime referenceDate = OffsetDateTime.now().plusYears(10);

        ContractPeriod period = ContractPeriod.of(start, null);

        assertThat(period.isActiveAt(referenceDate)).isTrue();
    }

    @Test
    @DisplayName("GIVEN period WHEN isActiveAt(exactly end date) THEN return false")
    void shouldNotBeActiveAtExactlyEndDate() {
        OffsetDateTime start = OffsetDateTime.now().minusDays(10);
        OffsetDateTime end = OffsetDateTime.now().plusDays(10);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.isActiveAt(end)).isFalse();
    }

    @Test
    @DisplayName("GIVEN two equal periods WHEN comparing THEN they are equal")
    void shouldBeEqualWhenSameValues() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusDays(30);

        ContractPeriod period1 = ContractPeriod.of(start, end);
        ContractPeriod period2 = ContractPeriod.of(start, end);

        assertThat(period1).isEqualTo(period2);
        assertThat(period1).hasSameHashCodeAs(period2);
    }

    @Test
    @DisplayName("GIVEN two different periods WHEN comparing THEN they are not equal")
    void shouldNotBeEqualWhenDifferentValues() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end1 = start.plusDays(30);
        OffsetDateTime end2 = start.plusDays(60);

        ContractPeriod period1 = ContractPeriod.of(start, end1);
        ContractPeriod period2 = ContractPeriod.of(start, end2);

        assertThat(period1).isNotEqualTo(period2);
    }

    @Test
    @DisplayName("GIVEN period compared to itself WHEN equals THEN return true")
    void shouldBeEqualToItself() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusDays(30);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period).isEqualTo(period);
    }

    @Test
    @DisplayName("GIVEN period compared to null WHEN equals THEN return false")
    void shouldNotBeEqualToNull() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusDays(30);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period).isNotEqualTo(null);
    }

    @Test
    @DisplayName("GIVEN periods with only null end WHEN comparing THEN equal if same start")
    void shouldBeEqualWhenBothHaveNullEnd() {
        OffsetDateTime start = OffsetDateTime.now();

        ContractPeriod period1 = ContractPeriod.of(start, null);
        ContractPeriod period2 = ContractPeriod.of(start, null);

        assertThat(period1).isEqualTo(period2);
    }

    @Test
    @DisplayName("GIVEN period WHEN toString THEN return formatted string")
    void shouldReturnFormattedString() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusDays(30);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.toString())
                .contains("ContractPeriod")
                .contains("startDate")
                .contains("endDate");
    }

    @Test
    @DisplayName("GIVEN very long period WHEN of() THEN accept")
    void shouldAcceptVeryLongPeriod() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusYears(100);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("GIVEN very short period WHEN of() THEN accept")
    void shouldAcceptVeryShortPeriod() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusMinutes(1);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("GIVEN period with past start WHEN of() THEN accept")
    void shouldAcceptPastStartDate() {
        OffsetDateTime start = OffsetDateTime.now().minusYears(1);
        OffsetDateTime end = OffsetDateTime.now().plusYears(1);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("GIVEN period with future start WHEN of() THEN accept")
    void shouldAcceptFutureStartDate() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(10);
        OffsetDateTime end = start.plusDays(20);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isEqualTo(end);
    }
}

