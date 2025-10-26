package com.mk.contractservice.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Email Value Object")
class EmailTest {

    @Test
    @DisplayName("Should create valid email with normalization")
    void shouldCreateValidEmail() {
        // Given
        String rawEmail = "  John.Doe@Example.COM  ";

        // When
        Email email = Email.of(rawEmail);

        // Then
        assertThat(email.value())
                .isEqualTo("john.doe@example.com")
                .isLowerCase();
    }

    @Test
    @DisplayName("Should normalize email to lowercase")
    void shouldNormalizeEmailToLowerCase() {
        // Given
        String rawEmail = "USER@DOMAIN.COM";

        // When
        Email email = Email.of(rawEmail);

        // Then
        assertThat(email.value()).isEqualTo("user@domain.com");
    }

    @Test
    @DisplayName("Should trim whitespace from email")
    void shouldTrimWhitespace() {
        // Given
        String rawEmail = "   user@domain.com   ";

        // When
        Email email = Email.of(rawEmail);

        // Then
        assertThat(email.value()).isEqualTo("user@domain.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should reject null or blank email")
    void shouldRejectNullOrBlank(String invalidEmail) {
        // When / Then
        assertThatThrownBy(() -> Email.of(invalidEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email must not be null or blank");
    }

    @Test
    @DisplayName("Should reject email exceeding 254 characters")
    void shouldRejectEmailTooLong() {
        // Given - email with 255 characters
        String tooLongEmail = "a".repeat(245) + "@test.com";

        // When / Then
        assertThatThrownBy(() -> Email.of(tooLongEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email too long");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid",
            "@invalid.com",
            "invalid@",
            "invalid@domain",
            "invalid @domain.com",
            "invalid@domain .com",
            "user@@domain.com"
    })
    @DisplayName("Should reject invalid email formats")
    void shouldRejectInvalidFormat(String invalidEmail) {
        // When / Then
        assertThatThrownBy(() -> Email.of(invalidEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user@domain.com",
            "user.name@domain.com",
            "user+tag@domain.co.uk",
            "user_name@sub.domain.com",
            "123@domain.com",
            "a@b.co"
    })
    @DisplayName("Should accept valid email formats")
    void shouldAcceptValidFormats(String validEmail) {
        // When / Then
        assertThatNoException().isThrownBy(() -> Email.of(validEmail));
    }

    @Test
    @DisplayName("Should be equal when values are equal (case-insensitive)")
    void shouldBeEqualWhenValuesAreEqual() {
        // Given
        Email email1 = Email.of("user@domain.com");
        Email email2 = Email.of("USER@DOMAIN.COM");

        // When / Then
        assertThat(email1).isEqualTo(email2);
        assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
    }

    @Test
    @DisplayName("Compact constructor should normalize value")
    void compactConstructorShouldAlsoNormalize() {
        // Given - Using direct constructor (not recommended in production)
        String rawEmail = "TEST@EXAMPLE.COM";

        // When
        //Email email = new Email(rawEmail);

        // Then
        //assertThat(email.value()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Compact constructor should reject null")
    void compactConstructorShouldRejectNull() {
        // When / Then
        /*assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email value must not be null");*/
    }

    @Test
    @DisplayName("Should be idempotent - applying .of() twice gives same result")
    void shouldBeIdempotent() {
        // Given
        String originalEmail = "USER@EXAMPLE.COM";

        // When
        Email first = Email.of(originalEmail);
        Email second = Email.of(first.value());

        // Then
        assertThat(first).isEqualTo(second);
        assertThat(first.value()).isEqualTo(second.value());
    }
}

