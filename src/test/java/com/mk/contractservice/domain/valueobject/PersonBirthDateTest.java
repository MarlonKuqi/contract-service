package com.mk.contractservice.domain.valueobject;

import com.mk.contractservice.domain.exception.InvalidPersonBirthDateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PersonBirthDate - Value Object Tests")
class PersonBirthDateTest {

    @Test
    @DisplayName("GIVEN valid past date WHEN of() THEN create PersonBirthDate")
    void shouldCreateWithValidPastDate() {
        LocalDate validDate = LocalDate.of(1990, 5, 15);

        PersonBirthDate birthDate = PersonBirthDate.of(validDate);

        assertThat(birthDate.value()).isEqualTo(validDate);
    }

    @Test
    @DisplayName("GIVEN null date WHEN of() THEN throw InvalidPersonBirthDateException")
    void shouldRejectNullDate() {
        assertThatThrownBy(() -> PersonBirthDate.of(null))
                .isInstanceOf(InvalidPersonBirthDateException.class)
                .hasMessageContaining("Birth date must not be null");
    }

    @Test
    @DisplayName("GIVEN future date WHEN of() THEN throw InvalidPersonBirthDateException")
    void shouldRejectFutureDate() {
        LocalDate futureDate = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> PersonBirthDate.of(futureDate))
                .isInstanceOf(InvalidPersonBirthDateException.class)
                .hasMessageContaining("Birth date cannot be in the future");
    }

    @Test
    @DisplayName("GIVEN today's date WHEN of() THEN accept (person born today)")
    void shouldAcceptTodayDate() {
        LocalDate today = LocalDate.now();

        PersonBirthDate birthDate = PersonBirthDate.of(today);

        assertThat(birthDate.value()).isEqualTo(today);
    }

    @Test
    @DisplayName("GIVEN ISO 8601 format WHEN of() THEN preserve format")
    void shouldPreserveISO8601Format() {
        LocalDate iso8601Date = LocalDate.parse("1990-05-15");

        PersonBirthDate birthDate = PersonBirthDate.of(iso8601Date);

        assertThat(birthDate.value().toString()).isEqualTo("1990-05-15");
        assertThat(birthDate.toString()).isEqualTo("1990-05-15");
    }

    @Test
    @DisplayName("GIVEN same dates WHEN equals() THEN return true")
    void shouldBeEqualForSameDates() {
        LocalDate date = LocalDate.of(1990, 5, 15);
        PersonBirthDate birthDate1 = PersonBirthDate.of(date);
        PersonBirthDate birthDate2 = PersonBirthDate.of(date);

        assertThat(birthDate1).isEqualTo(birthDate2);
        assertThat(birthDate1.hashCode()).isEqualTo(birthDate2.hashCode());
    }

    @Test
    @DisplayName("GIVEN different dates WHEN equals() THEN return false")
    void shouldNotBeEqualForDifferentDates() {
        PersonBirthDate birthDate1 = PersonBirthDate.of(LocalDate.of(1990, 5, 15));
        PersonBirthDate birthDate2 = PersonBirthDate.of(LocalDate.of(1991, 6, 20));

        assertThat(birthDate1).isNotEqualTo(birthDate2);
    }

    @Test
    @DisplayName("GIVEN very old date WHEN of() THEN accept (historical data)")
    void shouldAcceptVeryOldDate() {
        LocalDate oldDate = LocalDate.of(1900, 1, 1);

        PersonBirthDate birthDate = PersonBirthDate.of(oldDate);

        assertThat(birthDate.value()).isEqualTo(oldDate);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 7, 30, 365, 1000})
    @DisplayName("GIVEN various future dates WHEN of() THEN throw exception")
    void shouldRejectVariousFutureDates(int daysInFuture) {
        LocalDate futureDate = LocalDate.now().plusDays(daysInFuture);

        assertThatThrownBy(() -> PersonBirthDate.of(futureDate))
                .isInstanceOf(InvalidPersonBirthDateException.class)
                .hasMessageContaining("Birth date cannot be in the future");
    }

    @Test
    @DisplayName("GIVEN leap year date (Feb 29) WHEN of() THEN accept")
    void shouldAcceptLeapYearDate() {
        LocalDate leapYearDate = LocalDate.of(2000, 2, 29);

        PersonBirthDate birthDate = PersonBirthDate.of(leapYearDate);

        assertThat(birthDate.value()).isEqualTo(leapYearDate);
    }

    @Test
    @DisplayName("GIVEN first day of year WHEN of() THEN accept")
    void shouldAcceptFirstDayOfYear() {
        LocalDate firstDay = LocalDate.of(2000, 1, 1);

        PersonBirthDate birthDate = PersonBirthDate.of(firstDay);

        assertThat(birthDate.value()).isEqualTo(firstDay);
    }

    @Test
    @DisplayName("GIVEN last day of year WHEN of() THEN accept")
    void shouldAcceptLastDayOfYear() {
        LocalDate lastDay = LocalDate.of(2000, 12, 31);

        PersonBirthDate birthDate = PersonBirthDate.of(lastDay);

        assertThat(birthDate.value()).isEqualTo(lastDay);
    }

    @Test
    @DisplayName("GIVEN century boundaries WHEN of() THEN accept")
    void shouldAcceptCenturyBoundaries() {
        assertThatNoException().isThrownBy(() -> PersonBirthDate.of(LocalDate.of(1900, 1, 1)));
        assertThatNoException().isThrownBy(() -> PersonBirthDate.of(LocalDate.of(2000, 1, 1)));
    }

    @Test
    @DisplayName("GIVEN yesterday WHEN of() THEN accept")
    void shouldAcceptYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        PersonBirthDate birthDate = PersonBirthDate.of(yesterday);

        assertThat(birthDate.value()).isEqualTo(yesterday);
    }

    @Test
    @DisplayName("GIVEN 150 years ago WHEN of() THEN accept (edge case for very old person)")
    void shouldAcceptVeryOldPerson() {
        LocalDate veryOld = LocalDate.now().minusYears(150);

        PersonBirthDate birthDate = PersonBirthDate.of(veryOld);

        assertThat(birthDate.value()).isEqualTo(veryOld);
    }

    @Test
    @DisplayName("GIVEN edge case: exactly today WHEN of() THEN accept (person born today)")
    void shouldAcceptExactlyToday() {
        LocalDate today = LocalDate.now();

        PersonBirthDate birthDate = PersonBirthDate.of(today);

        assertThat(birthDate.value()).isEqualTo(today);
    }

    @Test
    @DisplayName("GIVEN edge case: tomorrow WHEN of() THEN reject")
    void shouldRejectTomorrow() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> PersonBirthDate.of(tomorrow))
                .isInstanceOf(InvalidPersonBirthDateException.class)
                .hasMessageContaining("Birth date cannot be in the future");
    }
}
