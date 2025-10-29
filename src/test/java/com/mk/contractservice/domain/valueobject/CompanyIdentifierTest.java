package com.mk.contractservice.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CompanyIdentifier - Value Object Tests")
class CompanyIdentifierTest {

    @Test
    @DisplayName("GIVEN valid identifier WHEN creating CompanyIdentifier THEN value object is created")
    void shouldCreateWithValidIdentifier() {
        CompanyIdentifier identifier = CompanyIdentifier.of("aaa-123");

        assertThat(identifier).isNotNull();
        assertThat(identifier.value()).isEqualTo("aaa-123");
    }

    @Test
    @DisplayName("GIVEN identifier from subject example WHEN creating CompanyIdentifier THEN accept")
    void shouldAcceptSubjectExample() {
        CompanyIdentifier identifier = CompanyIdentifier.of("aaa-123");

        assertThat(identifier.value()).isEqualTo("aaa-123");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("GIVEN null or blank identifier WHEN creating CompanyIdentifier THEN throw exception")
    void shouldRejectNullOrBlank(String invalidIdentifier) {
        assertThatThrownBy(() -> CompanyIdentifier.of(invalidIdentifier));
    }

    @Test
    @DisplayName("GIVEN blank identifier WHEN creating CompanyIdentifier THEN throw exception")
    void shouldRejectBlankIdentifier() {
        assertThatThrownBy(() -> CompanyIdentifier.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company identifier must not be null or blank");
    }

    @Test
    @DisplayName("GIVEN empty identifier WHEN creating CompanyIdentifier THEN throw exception")
    void shouldRejectEmptyIdentifier() {
        assertThatThrownBy(() -> CompanyIdentifier.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company identifier must not be null or blank");
    }

    @Test
    @DisplayName("GIVEN identifier too long WHEN creating CompanyIdentifier THEN throw exception")
    void shouldRejectIdentifierTooLong() {
        String tooLong = "a".repeat(65);

        assertThatThrownBy(() -> CompanyIdentifier.of(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company identifier too long");
    }

    @Test
    @DisplayName("GIVEN identifier with max length WHEN creating CompanyIdentifier THEN accept")
    void shouldAcceptMaxLengthIdentifier() {
        String maxLength = "a".repeat(64);

        CompanyIdentifier identifier = CompanyIdentifier.of(maxLength);

        assertThat(identifier.value()).hasSize(64);
    }

    @Test
    @DisplayName("GIVEN identifier with whitespace WHEN creating CompanyIdentifier THEN trim")
    void shouldTrimWhitespace() {
        CompanyIdentifier identifier = CompanyIdentifier.of("  acme-123  ");

        assertThat(identifier.value()).isEqualTo("acme-123");
    }

    @Test
    @DisplayName("GIVEN identifier with special chars WHEN creating CompanyIdentifier THEN accept")
    void shouldAcceptSpecialCharacters() {
        CompanyIdentifier identifier = CompanyIdentifier.of("abc-xyz_123");

        assertThat(identifier.value()).isEqualTo("abc-xyz_123");
    }

    @Test
    @DisplayName("GIVEN two identical identifiers WHEN comparing THEN they are equal")
    void shouldBeEqualWhenSameValue() {
        CompanyIdentifier id1 = CompanyIdentifier.of("acme-123");
        CompanyIdentifier id2 = CompanyIdentifier.of("acme-123");

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    @DisplayName("GIVEN two different identifiers WHEN comparing THEN they are not equal")
    void shouldNotBeEqualWhenDifferentValue() {
        CompanyIdentifier id1 = CompanyIdentifier.of("acme-123");
        CompanyIdentifier id2 = CompanyIdentifier.of("acme-456");

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("GIVEN identifier with numbers WHEN creating CompanyIdentifier THEN accept")
    void shouldAcceptNumbers() {
        CompanyIdentifier identifier = CompanyIdentifier.of("123456789");

        assertThat(identifier.value()).isEqualTo("123456789");
    }

    @Test
    @DisplayName("GIVEN identifier with mixed case WHEN creating CompanyIdentifier THEN preserve case")
    void shouldPreserveCase() {
        CompanyIdentifier identifier = CompanyIdentifier.of("AcMe-123");

        assertThat(identifier.value()).isEqualTo("AcMe-123");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "aaa-123",
            "abc-xyz-123",
            "company_identifier",
            "comp-123_456",
            "123-456",
            "a",
            "A-B-C"
    })
    @DisplayName("GIVEN valid identifier formats WHEN creating CompanyIdentifier THEN accept")
    void shouldAcceptValidFormats(String validIdentifier) {
        assertThatNoException().isThrownBy(() -> CompanyIdentifier.of(validIdentifier));
    }

    @Test
    @DisplayName("GIVEN identifiers differing only in case WHEN comparing THEN not equal")
    void shouldBeCaseSensitive() {
        CompanyIdentifier id1 = CompanyIdentifier.of("acme-123");
        CompanyIdentifier id2 = CompanyIdentifier.of("ACME-123");

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("GIVEN identifier with leading/trailing spaces WHEN comparing THEN equal after trim")
    void shouldBeEqualAfterTrim() {
        CompanyIdentifier id1 = CompanyIdentifier.of("acme-123");
        CompanyIdentifier id2 = CompanyIdentifier.of("  acme-123  ");

        assertThat(id1).isEqualTo(id2);
    }

    @Test
    @DisplayName("GIVEN identifier at boundary (64 chars) WHEN creating THEN accept")
    void shouldAcceptExactlyMaxLength() {
        String exactly64 = "a".repeat(64);

        CompanyIdentifier identifier = CompanyIdentifier.of(exactly64);

        assertThat(identifier.value()).hasSize(64);
    }

    @Test
    @DisplayName("GIVEN identifier beyond boundary (65 chars) WHEN creating THEN reject")
    void shouldRejectJustOverMaxLength() {
        String exactly65 = "a".repeat(65);

        assertThatThrownBy(() -> CompanyIdentifier.of(exactly65))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company identifier too long");
    }
}

