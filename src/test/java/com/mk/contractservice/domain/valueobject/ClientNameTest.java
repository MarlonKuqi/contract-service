package com.mk.contractservice.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

    @Test
    @DisplayName("GIVEN null name WHEN of() THEN throw exception")
    void shouldRejectNull() {
        assertThatThrownBy(() -> ClientName.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Client name must not be null or blank");
    }

    @Test
    @DisplayName("GIVEN blank name WHEN of() THEN throw exception")
    void shouldRejectBlank() {
        assertThatThrownBy(() -> ClientName.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Client name must not be null or blank");
    }

    @Test
    @DisplayName("GIVEN empty name WHEN of() THEN throw exception")
    void shouldRejectEmpty() {
        assertThatThrownBy(() -> ClientName.of(""))
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
}

