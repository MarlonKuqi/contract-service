package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Client - Domain Entity Tests")
class ClientTest {

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("GIVEN all required fields WHEN creating Client THEN client is created")
        void shouldCreateClientWithAllFields() {
            Person person = new Person(
                    ClientName.of("Test User"),
                    Email.of("test@example.com"),
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            assertThat(person.getName().value()).isEqualTo("Test User");
            assertThat(person.getEmail().value()).isEqualTo("test@example.com");
            assertThat(person.getPhone().value()).isEqualTo("+33123456789");
        }

        @Test
        @DisplayName("GIVEN null name WHEN creating Client THEN throw exception")
        void shouldRejectNullName() {
            assertThatThrownBy(() -> new Person(
                    null,
                    Email.of("test@example.com"),
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Name must not be null");
        }

        @Test
        @DisplayName("GIVEN null email WHEN creating Client THEN throw exception")
        void shouldRejectNullEmail() {
            assertThatThrownBy(() -> new Person(
                    ClientName.of("Test User"),
                    null,
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email must not be null");
        }

        @Test
        @DisplayName("GIVEN null phone WHEN creating Client THEN throw exception")
        void shouldRejectNullPhone() {
            assertThatThrownBy(() -> new Person(
                    ClientName.of("Test User"),
                    Email.of("test@example.com"),
                    null,
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Phone must not be null");
        }
    }

    @Nested
    @DisplayName("updateCommonFields - Subject requirement: Update all fields except birthdate/companyIdentifier")
    class UpdateCommonFieldsValidation {

        @Test
        @DisplayName("GIVEN valid fields WHEN updating THEN all common fields are updated")
        void shouldUpdateAllCommonFields() {
            Person person = new Person(
                    ClientName.of("Original Name"),
                    Email.of("original@example.com"),
                    PhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            person.updateCommonFields(
                    ClientName.of("Updated Name"),
                    Email.of("updated@example.com"),
                    PhoneNumber.of("+33222222222")
            );

            assertThat(person.getName().value()).isEqualTo("Updated Name");
            assertThat(person.getEmail().value()).isEqualTo("updated@example.com");
            assertThat(person.getPhone().value()).isEqualTo("+33222222222");
        }

        @Test
        @DisplayName("GIVEN null fields WHEN updating THEN throw exception with all null fields listed")
        void shouldRejectAllNullFieldsOnUpdate() {
            Person person = new Person(
                    ClientName.of("Test User"),
                    Email.of("test@example.com"),
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            assertThatThrownBy(() -> person.updateCommonFields(null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name")
                    .hasMessageContaining("email")
                    .hasMessageContaining("phone");
        }
    }
}

