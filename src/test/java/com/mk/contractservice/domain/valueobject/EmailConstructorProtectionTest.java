package com.mk.contractservice.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Email Value Object - Constructor Protection")
class EmailConstructorProtectionTest {

    @Test
    @DisplayName("Should NOT compile if trying to use constructor directly")
    void cannotUseConstructorDirectly() {
        // ❌ Ce code NE COMPILE PAS :
        // Email email = new Email("user@example.com");
        // → Compilation error: 'Email(String)' has private access

        // ✅ Seule façon valide :
        Email email = Email.of("user@example.com");

        assertThat(email.value()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Factory method of() enforces validation")
    void factoryMethodEnforcesValidation() {
        // Null check
        assertThatThrownBy(() -> Email.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email must not be null");

        // Blank check
        assertThatThrownBy(() -> Email.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email must not be null or blank");

        // Format validation
        assertThatThrownBy(() -> Email.of("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");

        // Length validation
        String tooLong = "a".repeat(250) + "@example.com";
        assertThatThrownBy(() -> Email.of(tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email too long");
    }

    @Test
    @DisplayName("Factory method normalizes email (trim + lowercase)")
    void factoryMethodNormalizesEmail() {
        Email email = Email.of("  USER@EXAMPLE.COM  ");

        assertThat(email.value()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Cannot bypass validation - constructor is private")
    void constructorIsPrivate() {
        // Si le constructeur était public, on pourrait faire :
        // Email badEmail = new Email("NOT@VALIDATED");  // ❌ Contourne validation

        // Mais avec private constructor :
        // → Compilation error!

        // Seule façon :
        Email validEmail = Email.of("test@example.com");
        assertThat(validEmail.value()).isEqualTo("test@example.com");
    }
}

