package com.mk.contractservice.domain.valueobject;

import com.mk.contractservice.domain.exception.InvalidContractPeriodException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ContractPeriod - Business Rules Tests")
class ContractPeriodTest {

    @Test
    @DisplayName("GIVEN start and end dates WHEN of() THEN create ContractPeriod")
    void shouldCreateWithBothDates() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(30);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("GIVEN null start date WHEN of() THEN use current date")
    void shouldUseCurrentDateWhenStartIsNull() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        LocalDateTime end = LocalDateTime.now().plusDays(30);

        ContractPeriod period = ContractPeriod.of(null, end);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(period.startDate()).isNotNull();
        assertThat(period.startDate()).isBetween(before, after);
        assertThat(period.endDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("GIVEN null end date WHEN of() THEN create period with null end")
    void shouldAcceptNullEndDate() {
        LocalDateTime start = LocalDateTime.now();

        ContractPeriod period = ContractPeriod.of(start, null);

        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isNull();
    }

    @Test
    @DisplayName("GIVEN both null dates WHEN of() THEN create with current start and null end")
    void shouldAcceptBothNullDates() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        ContractPeriod period = ContractPeriod.of(null, null);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(period.startDate()).isNotNull();
        assertThat(period.startDate()).isBetween(before, after);
        assertThat(period.endDate()).isNull();
    }

    @Test
    @DisplayName("GIVEN end before start WHEN of() THEN throw exception")
    void shouldRejectEndBeforeStart() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.minusDays(1);

        assertThatThrownBy(() -> ContractPeriod.of(start, end))
                .isInstanceOf(InvalidContractPeriodException.class)
                .hasMessageContaining("Contract end date must be after start date");
    }

    @Test
    @DisplayName("GIVEN end equal to start WHEN of() THEN throw exception")
    void shouldRejectEndEqualToStart() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start;

        assertThatThrownBy(() -> ContractPeriod.of(start, end))
                .isInstanceOf(InvalidContractPeriodException.class)
                .hasMessageContaining("Contract end date must be after start date");
    }

    @Test
    @DisplayName("GIVEN end 1 second after start WHEN of() THEN accept")
    void shouldAcceptEndJustAfterStart() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusSeconds(1);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("GIVEN period with null end WHEN isActive() THEN return true")
    void shouldBeActiveWhenEndIsNull() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);

        ContractPeriod period = ContractPeriod.of(start, null);

        assertThat(period.isActive()).isTrue();
    }

    @Test
    @DisplayName("GIVEN period with future end WHEN isActive() THEN return true")
    void shouldBeActiveWhenEndInFuture() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.isActive()).isTrue();
    }

    @Test
    @DisplayName("GIVEN period with past end WHEN isActive() THEN return false")
    void shouldNotBeActiveWhenEndInPast() {
        LocalDateTime start = LocalDateTime.now().minusDays(10);
        LocalDateTime end = LocalDateTime.now().minusDays(1);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.isActive()).isFalse();
    }

    @Test
    @DisplayName("GIVEN two equal periods WHEN comparing THEN they are equal")
    void shouldBeEqualWhenSameValues() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(30);

        ContractPeriod period1 = ContractPeriod.of(start, end);
        ContractPeriod period2 = ContractPeriod.of(start, end);

        assertThat(period1).isEqualTo(period2);
        assertThat(period1).hasSameHashCodeAs(period2);
    }

    @Test
    @DisplayName("GIVEN two different periods WHEN comparing THEN they are not equal")
    void shouldNotBeEqualWhenDifferentValues() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end1 = start.plusDays(30);
        LocalDateTime end2 = start.plusDays(60);

        ContractPeriod period1 = ContractPeriod.of(start, end1);
        ContractPeriod period2 = ContractPeriod.of(start, end2);

        assertThat(period1).isNotEqualTo(period2);
    }

    @Test
    @DisplayName("GIVEN period compared to itself WHEN equals THEN return true")
    void shouldBeEqualToItself() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(30);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period).isEqualTo(period);
    }

    @Test
    @DisplayName("GIVEN period compared to null WHEN equals THEN return false")
    void shouldNotBeEqualToNull() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(30);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period).isNotEqualTo(null);
    }

    @Test
    @DisplayName("GIVEN periods with only null end WHEN comparing THEN equal if same start")
    void shouldBeEqualWhenBothHaveNullEnd() {
        LocalDateTime start = LocalDateTime.now();

        ContractPeriod period1 = ContractPeriod.of(start, null);
        ContractPeriod period2 = ContractPeriod.of(start, null);

        assertThat(period1).isEqualTo(period2);
    }

    @Test
    @DisplayName("GIVEN period WHEN toString THEN return formatted string")
    void shouldReturnFormattedString() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(30);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.toString())
                .contains("ContractPeriod")
                .contains("startDate")
                .contains("endDate");
    }

    @Test
    @DisplayName("GIVEN very long period WHEN of() THEN accept")
    void shouldAcceptVeryLongPeriod() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusYears(100);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("GIVEN very short period WHEN of() THEN accept")
    void shouldAcceptVeryShortPeriod() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMinutes(1);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("GIVEN period with past start WHEN of() THEN accept")
    void shouldAcceptPastStartDate() {
        LocalDateTime start = LocalDateTime.now().minusYears(1);
        LocalDateTime end = LocalDateTime.now().plusYears(1);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("GIVEN period with future start WHEN of() THEN accept")
    void shouldAcceptFutureStartDate() {
        LocalDateTime start = LocalDateTime.now().plusDays(10);
        LocalDateTime end = start.plusDays(20);

        ContractPeriod period = ContractPeriod.of(start, end);

        assertThat(period.startDate()).isEqualTo(start);
        assertThat(period.endDate()).isEqualTo(end);
    }
}



