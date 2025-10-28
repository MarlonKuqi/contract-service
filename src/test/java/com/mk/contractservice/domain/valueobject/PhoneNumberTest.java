package com.mk.contractservice.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PhoneNumber - Business Rules Tests")
class PhoneNumberTest {

    @Test
    @DisplayName("GIVEN valid phone number WHEN of() THEN create PhoneNumber")
    void shouldCreateWithValidPhoneNumber() {
        PhoneNumber phoneNumber = PhoneNumber.of("+33123456789");

        assertThat(phoneNumber.value()).isEqualTo("+33123456789");
    }

    @Test
    @DisplayName("GIVEN null phone number WHEN of() THEN throw exception")
    void shouldRejectNull() {
        assertThatThrownBy(() -> PhoneNumber.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone number must not be null or blank");
    }

    @Test
    @DisplayName("GIVEN blank phone number WHEN of() THEN throw exception")
    void shouldRejectBlank() {
        assertThatThrownBy(() -> PhoneNumber.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone number must not be null or blank");
    }

    @Test
    @DisplayName("GIVEN empty phone number WHEN of() THEN throw exception")
    void shouldRejectEmpty() {
        assertThatThrownBy(() -> PhoneNumber.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone number must not be null or blank");
    }

    @Test
    @DisplayName("GIVEN phone with whitespace WHEN of() THEN trim whitespace")
    void shouldTrimWhitespace() {
        PhoneNumber phoneNumber = PhoneNumber.of("  +33123456789  ");

        assertThat(phoneNumber.value()).isEqualTo("+33123456789");
    }

    @Test
    @DisplayName("GIVEN invalid format WHEN of() THEN throw exception")
    void shouldRejectInvalidFormat() {
        assertThatThrownBy(() -> PhoneNumber.of("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid phone number format");
    }

    @Test
    @DisplayName("GIVEN too short phone WHEN of() THEN throw exception")
    void shouldRejectTooShort() {
        assertThatThrownBy(() -> PhoneNumber.of("123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid phone number format");
    }

    @Test
    @DisplayName("GIVEN too long phone WHEN of() THEN throw exception")
    void shouldRejectTooLong() {
        assertThatThrownBy(() -> PhoneNumber.of("123456789012345678901"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid phone number format");
    }

    @Test
    @DisplayName("GIVEN international format WHEN of() THEN accept")
    void shouldAcceptInternationalFormat() {
        PhoneNumber phoneNumber = PhoneNumber.of("+33123456789");

        assertThat(phoneNumber.value()).isEqualTo("+33123456789");
    }

    @Test
    @DisplayName("GIVEN phone with spaces WHEN of() THEN accept")
    void shouldAcceptPhoneWithSpaces() {
        PhoneNumber phoneNumber = PhoneNumber.of("+33 1 23 45 67 89");

        assertThat(phoneNumber.value()).isEqualTo("+33 1 23 45 67 89");
    }

    @Test
    @DisplayName("GIVEN phone with parentheses WHEN of() THEN accept")
    void shouldAcceptPhoneWithParentheses() {
        PhoneNumber phoneNumber = PhoneNumber.of("+33(1)23456789");

        assertThat(phoneNumber.value()).isEqualTo("+33(1)23456789");
    }

    @Test
    @DisplayName("GIVEN phone with hyphens WHEN of() THEN accept")
    void shouldAcceptPhoneWithHyphens() {
        PhoneNumber phoneNumber = PhoneNumber.of("+33-1-23-45-67-89");

        assertThat(phoneNumber.value()).isEqualTo("+33-1-23-45-67-89");
    }

    @Test
    @DisplayName("GIVEN phone with dots WHEN of() THEN accept")
    void shouldAcceptPhoneWithDots() {
        PhoneNumber phoneNumber = PhoneNumber.of("+33.1.23.45.67.89");

        assertThat(phoneNumber.value()).isEqualTo("+33.1.23.45.67.89");
    }
}

