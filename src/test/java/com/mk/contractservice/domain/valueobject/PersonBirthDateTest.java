package com.mk.contractservice.domain.valueobject;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("GIVEN null date WHEN of() THEN throw IllegalArgumentException")
    void shouldRejectNullDate() {
        assertThatThrownBy(() -> PersonBirthDate.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Birth date must not be null");
    }

    @Test
    @DisplayName("GIVEN future date WHEN of() THEN throw IllegalArgumentException")
    void shouldRejectFutureDate() {
        LocalDate futureDate = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> PersonBirthDate.of(futureDate))
                .isInstanceOf(IllegalArgumentException.class)
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
}

