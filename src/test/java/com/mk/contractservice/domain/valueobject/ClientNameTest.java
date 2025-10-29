package com.mk.contractservice.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ClientName - Business Rules Tests")
class ClientNameTest {

    @Test
    @DisplayName("GIVEN valid name WHEN of() THEN create ClientName")
    void shouldCreateWithValidName() {
        ClientName name = ClientName.of("John Doe");

        assertThat(name.value()).isEqualTo("John Doe");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n", "  \t\n  "})
    @DisplayName("GIVEN null or blank name WHEN of() THEN throw exception")
    void shouldRejectNullOrBlank(String invalidName) {
        assertThatThrownBy(() -> ClientName.of(invalidName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Client name must not be null or blank");
    }

    @Test
    @DisplayName("GIVEN name with whitespace WHEN of() THEN trim whitespace")
    void shouldTrimWhitespace() {
        ClientName name = ClientName.of("  John Doe  ");

        assertThat(name.value()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("GIVEN name too long WHEN of() THEN throw exception")
    void shouldRejectTooLong() {
        String tooLongName = "a".repeat(201);

        assertThatThrownBy(() -> ClientName.of(tooLongName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Client name too long (max 200 characters)");
    }

    @Test
    @DisplayName("GIVEN name with 200 chars WHEN of() THEN accept")
    void shouldAcceptMaxLength() {
        String maxLengthName = "a".repeat(200);

        ClientName name = ClientName.of(maxLengthName);

        assertThat(name.value()).hasSize(200);
    }

    @Test
    @DisplayName("GIVEN name with special characters WHEN of() THEN accept")
    void shouldAcceptSpecialCharacters() {
        ClientName name = ClientName.of("Jean-François O'Connor");

        assertThat(name.value()).isEqualTo("Jean-François O'Connor");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Jean-François O'Connor",
            "María José García",
            "Company & Co.",
            "A B C D E F G",
            "John Doe Jr.",
            "Dr. Smith"
    })
    @DisplayName("GIVEN valid international names WHEN of() THEN accept")
    void shouldAcceptInternationalNames(String validName) {
        ClientName name = ClientName.of(validName);

        assertThat(name.value()).isEqualTo(validName);
    }

    @Test
    @DisplayName("GIVEN two equal names WHEN comparing THEN they are equal")
    void shouldBeEqualWhenSameValue() {
        ClientName name1 = ClientName.of("John Doe");
        ClientName name2 = ClientName.of("John Doe");

        assertThat(name1).isEqualTo(name2);
        assertThat(name1).hasSameHashCodeAs(name2);
    }

    @Test
    @DisplayName("GIVEN two different names WHEN comparing THEN they are not equal")
    void shouldNotBeEqualWhenDifferentValue() {
        ClientName name1 = ClientName.of("John Doe");
        ClientName name2 = ClientName.of("Jane Doe");

        assertThat(name1).isNotEqualTo(name2);
    }

    @Test
    @DisplayName("GIVEN name with leading/trailing spaces WHEN comparing THEN equal after trim")
    void shouldBeEqualAfterTrim() {
        ClientName name1 = ClientName.of("John Doe");
        ClientName name2 = ClientName.of("  John Doe  ");

        assertThat(name1).isEqualTo(name2);
    }

    @Test
    @DisplayName("GIVEN name compared to itself WHEN equals THEN return true")
    void shouldBeEqualToItself() {
        ClientName name = ClientName.of("John Doe");

        assertThat(name).isEqualTo(name);
    }

    @Test
    @DisplayName("GIVEN name compared to null WHEN equals THEN return false")
    void shouldNotBeEqualToNull() {
        ClientName name = ClientName.of("John Doe");

        assertThat(name).isNotEqualTo(null);
    }

    @Test
    @DisplayName("GIVEN single character name WHEN of() THEN accept")
    void shouldAcceptSingleCharacter() {
        ClientName name = ClientName.of("A");

        assertThat(name.value()).isEqualTo("A");
    }

    @Test
    @DisplayName("GIVEN name with numbers WHEN of() THEN accept")
    void shouldAcceptNamesWithNumbers() {
        ClientName name = ClientName.of("Company 123");

        assertThat(name.value()).isEqualTo("Company 123");
    }
}

