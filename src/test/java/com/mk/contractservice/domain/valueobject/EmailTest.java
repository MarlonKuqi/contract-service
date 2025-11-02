package com.mk.contractservice.domain.valueobject;

import com.mk.contractservice.domain.exception.InvalidEmailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Email Value Object")
class EmailTest {

    @Test
    @DisplayName("Should create valid email with normalization")
    void shouldCreateValidEmail() {
        String rawEmail = "  John.Doe@Example.COM  ";

        Email email = Email.of(rawEmail);

        assertThat(email.value())
                .isEqualTo("john.doe@example.com")
                .isLowerCase();
    }

    @Test
    @DisplayName("Should normalize email to lowercase")
    void shouldNormalizeEmailToLowerCase() {
        String rawEmail = "USER@DOMAIN.COM";

        Email email = Email.of(rawEmail);

        assertThat(email.value()).isEqualTo("user@domain.com");
    }

    @Test
    @DisplayName("Should trim whitespace from email")
    void shouldTrimWhitespace() {
        String rawEmail = "   user@domain.com   ";

        Email email = Email.of(rawEmail);

        assertThat(email.value()).isEqualTo("user@domain.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should reject null or blank email")
    void shouldRejectNullOrBlank(String invalidEmail) {
        assertThatThrownBy(() -> Email.of(invalidEmail))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessageContaining("Email must not be");
    }

    @Test
    @DisplayName("Should reject email exceeding 254 characters")
    void shouldRejectEmailTooLong() {
        String tooLongEmail = "a".repeat(246) + "@test.com";

        assertThatThrownBy(() -> Email.of(tooLongEmail))
                .isInstanceOf(InvalidEmailException.class)
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
        assertThatThrownBy(() -> Email.of(invalidEmail))
                .isInstanceOf(InvalidEmailException.class)
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
        assertThatNoException().isThrownBy(() -> Email.of(validEmail));
    }

    @Test
    @DisplayName("Should be equal when values are equal (case-insensitive)")
    void shouldBeEqualWhenValuesAreEqual() {
        Email email1 = Email.of("user@domain.com");
        Email email2 = Email.of("USER@DOMAIN.COM");

        assertThat(email1).isEqualTo(email2);
        assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
    }

    @Test
    @DisplayName("Compact constructor should normalize value")
    void compactConstructorShouldAlsoNormalize() {
        String rawEmail = "TEST@EXAMPLE.COM";

        Email email = Email.of(rawEmail);

        assertThat(email.value()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Compact constructor should reject null")
    void compactConstructorShouldRejectNull() {
        assertThatThrownBy(() -> Email.of(null))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessageContaining("Email must not be");
    }

    @Test
    @DisplayName("Should be idempotent - applying .of() twice gives same result")
    void shouldBeIdempotent() {
        String originalEmail = "USER@EXAMPLE.COM";

        Email first = Email.of(originalEmail);
        Email second = Email.of(first.value());

        assertThat(first).isEqualTo(second);
        assertThat(first.value()).isEqualTo(second.value());
    }
}

