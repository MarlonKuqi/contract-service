package com.mk.contractservice.domain.valueobject;

import com.mk.contractservice.domain.exception.InvalidPhoneNumberException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("GIVEN null or blank phone number WHEN of() THEN throw exception")
    void shouldRejectNullOrBlank(String invalidPhone) {
        assertThatThrownBy(() -> PhoneNumber.of(invalidPhone))
                .isInstanceOf(InvalidPhoneNumberException.class)
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
                .isInstanceOf(InvalidPhoneNumberException.class)
                .hasMessageContaining("Invalid phone number format");
    }

    @Test
    @DisplayName("GIVEN too short phone WHEN of() THEN throw exception")
    void shouldRejectTooShort() {
        assertThatThrownBy(() -> PhoneNumber.of("123"))
                .isInstanceOf(InvalidPhoneNumberException.class)
                .hasMessageContaining("Invalid phone number format");
    }

    @Test
    @DisplayName("GIVEN too long phone WHEN of() THEN throw exception")
    void shouldRejectTooLong() {
        assertThatThrownBy(() -> PhoneNumber.of("123456789012345678901"))
                .isInstanceOf(InvalidPhoneNumberException.class)
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

    @ParameterizedTest
    @ValueSource(strings = {
            "+33123456789",
            "+33 1 23 45 67 89",
            "+33(1)23456789",
            "+33-1-23-45-67-89",
            "+33.1.23.45.67.89",
            "+1234567890",
            "1234567",
            "12345678901234567890"
    })
    @DisplayName("GIVEN valid phone formats WHEN of() THEN accept")
    void shouldAcceptValidPhoneFormats(String validPhone) {
        PhoneNumber phoneNumber = PhoneNumber.of(validPhone);

        assertThat(phoneNumber.value()).isEqualTo(validPhone.trim());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc",
            "12",
            "123456",
            "123456789012345678901",
            "not-a-phone",
            "++33123456789",
            "33123456789a"
    })
    @DisplayName("GIVEN invalid phone formats WHEN of() THEN throw exception")
    void shouldRejectInvalidPhoneFormats(String invalidPhone) {
        assertThatThrownBy(() -> PhoneNumber.of(invalidPhone))
                .isInstanceOf(InvalidPhoneNumberException.class)
                .hasMessageContaining("Invalid phone number format");
    }

    @Test
    @DisplayName("GIVEN two equal phone numbers WHEN comparing THEN they are equal")
    void shouldBeEqualWhenSameValue() {
        PhoneNumber phone1 = PhoneNumber.of("+33123456789");
        PhoneNumber phone2 = PhoneNumber.of("+33123456789");

        assertThat(phone1).isEqualTo(phone2);
        assertThat(phone1).hasSameHashCodeAs(phone2);
    }

    @Test
    @DisplayName("GIVEN two different phone numbers WHEN comparing THEN they are not equal")
    void shouldNotBeEqualWhenDifferentValue() {
        PhoneNumber phone1 = PhoneNumber.of("+33123456789");
        PhoneNumber phone2 = PhoneNumber.of("+33987654321");

        assertThat(phone1).isNotEqualTo(phone2);
    }

    @Test
    @DisplayName("GIVEN phone with leading/trailing spaces WHEN comparing THEN equal after trim")
    void shouldBeEqualAfterTrim() {
        PhoneNumber phone1 = PhoneNumber.of("+33123456789");
        PhoneNumber phone2 = PhoneNumber.of("  +33123456789  ");

        assertThat(phone1).isEqualTo(phone2);
    }

    @Test
    @DisplayName("GIVEN phone compared to itself WHEN equals THEN return true")
    void shouldBeEqualToItself() {
        PhoneNumber phone = PhoneNumber.of("+33123456789");

        assertThat(phone).isEqualTo(phone);
    }

    @Test
    @DisplayName("GIVEN phone compared to null WHEN equals THEN return false")
    void shouldNotBeEqualToNull() {
        PhoneNumber phone = PhoneNumber.of("+33123456789");

        assertThat(phone).isNotEqualTo(null);
    }

    @Test
    @DisplayName("GIVEN minimum length phone WHEN of() THEN accept")
    void shouldAcceptMinimumLengthPhone() {
        PhoneNumber phone = PhoneNumber.of("1234567");

        assertThat(phone.value()).isEqualTo("1234567");
    }

    @Test
    @DisplayName("GIVEN maximum length phone WHEN of() THEN accept")
    void shouldAcceptMaximumLengthPhone() {
        String maxLengthPhone = "12345678901234567890";

        PhoneNumber phone = PhoneNumber.of(maxLengthPhone);

        assertThat(phone.value()).isEqualTo(maxLengthPhone);
    }
}

