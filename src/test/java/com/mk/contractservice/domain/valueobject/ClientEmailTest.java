package com.mk.contractservice.domain.valueobject;

import com.mk.contractservice.domain.client.ClientEmail;
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
class ClientEmailTest {

    @Test
    @DisplayName("Should create valid email with normalization")
    void shouldCreateValidEmail() {
        String rawEmail = "  John.Doe@Example.COM  ";

        ClientEmail clientEmail = ClientEmail.of(rawEmail);

        assertThat(clientEmail.value())
                .isEqualTo("john.doe@example.com")
                .isLowerCase();
    }

    @Test
    @DisplayName("Should normalize email to lowercase")
    void shouldNormalizeEmailToLowerCase() {
        String rawEmail = "USER@DOMAIN.COM";

        ClientEmail clientEmail = ClientEmail.of(rawEmail);

        assertThat(clientEmail.value()).isEqualTo("user@domain.com");
    }

    @Test
    @DisplayName("Should trim whitespace from email")
    void shouldTrimWhitespace() {
        String rawEmail = "   user@domain.com   ";

        ClientEmail clientEmail = ClientEmail.of(rawEmail);

        assertThat(clientEmail.value()).isEqualTo("user@domain.com");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should reject null or blank email")
    void shouldRejectNullOrBlank(String invalidEmail) {
        assertThatThrownBy(() -> ClientEmail.of(invalidEmail))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessageContaining("Email must not be");
    }

    @Test
    @DisplayName("Should reject email exceeding 254 characters")
    void shouldRejectEmailTooLong() {
        String tooLongEmail = "a".repeat(246) + "@test.com";

        assertThatThrownBy(() -> ClientEmail.of(tooLongEmail))
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
        assertThatThrownBy(() -> ClientEmail.of(invalidEmail))
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
        assertThatNoException().isThrownBy(() -> ClientEmail.of(validEmail));
    }

    @Test
    @DisplayName("Should be equal when values are equal (case-insensitive)")
    void shouldBeEqualWhenValuesAreEqual() {
        ClientEmail clientEmail1 = ClientEmail.of("user@domain.com");
        ClientEmail clientEmail2 = ClientEmail.of("USER@DOMAIN.COM");

        assertThat(clientEmail1).isEqualTo(clientEmail2);
        assertThat(clientEmail1.hashCode()).isEqualTo(clientEmail2.hashCode());
    }

    @Test
    @DisplayName("Compact constructor should normalize value")
    void compactConstructorShouldAlsoNormalize() {
        String rawEmail = "TEST@EXAMPLE.COM";

        ClientEmail clientEmail = ClientEmail.of(rawEmail);

        assertThat(clientEmail.value()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Compact constructor should reject null")
    void compactConstructorShouldRejectNull() {
        assertThatThrownBy(() -> ClientEmail.of(null))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessageContaining("Email must not be");
    }

    @Test
    @DisplayName("Should be idempotent - applying .of() twice gives same result")
    void shouldBeIdempotent() {
        String originalEmail = "USER@EXAMPLE.COM";

        ClientEmail first = ClientEmail.of(originalEmail);
        ClientEmail second = ClientEmail.of(first.value());

        assertThat(first).isEqualTo(second);
        assertThat(first.value()).isEqualTo(second.value());
    }

    @Test
    @DisplayName("Predicate IS_INVALID_LENGTH should be exact inverse of IS_VALID_LENGTH")
    void predicateIsInvalidLength() {
        assertThat(ClientEmail.IS_INVALID_LENGTH.test("a@b.c")).isFalse();
        assertThat(ClientEmail.IS_INVALID_LENGTH.test("x".repeat(254))).isFalse();
        assertThat(ClientEmail.IS_INVALID_LENGTH.test("x".repeat(255))).isTrue();
        assertThat(ClientEmail.IS_INVALID_LENGTH.test("")).isTrue();
    }

    @Test
    @DisplayName("Predicate HAS_INVALID_FORMAT should validate invalid email formats")
    void predicateHasInvalidFormat() {
        assertThat(ClientEmail.HAS_INVALID_FORMAT.test("invalid-email")).isTrue();
        assertThat(ClientEmail.HAS_INVALID_FORMAT.test("@invalid.com")).isTrue();
        assertThat(ClientEmail.HAS_INVALID_FORMAT.test("invalid@")).isTrue();
        assertThat(ClientEmail.HAS_INVALID_FORMAT.test("test@example.com")).isFalse();
        assertThat(ClientEmail.HAS_INVALID_FORMAT.test("user.name+tag@domain.co.uk")).isFalse();
    }
}
