package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

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
            Person person = Person.of(
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
            assertThatThrownBy(() -> Person.of(
                    null,
                    Email.of("test@example.com"),
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(Client.NULL_NAME_MSG);
        }

        @Test
        @DisplayName("GIVEN null email WHEN creating Client THEN throw exception")
        void shouldRejectNullEmail() {
            assertThatThrownBy(() -> Person.of(
                    ClientName.of("Test User"),
                    null,
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(Client.NULL_EMAIL_MSG);
        }

        @Test
        @DisplayName("GIVEN null phone WHEN creating Client THEN throw exception")
        void shouldRejectNullPhone() {
            assertThatThrownBy(() -> Person.of(
                    ClientName.of("Test User"),
                    Email.of("test@example.com"),
                    null,
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(Client.NULL_PHONE_MSG);
        }
    }

    @Nested
    @DisplayName("withCommonFields - Immutable update pattern: Creates new instance with updated fields")
    class WithCommonFieldsValidation {

        @Test
        @DisplayName("GIVEN valid fields WHEN creating updated instance THEN new instance has updated fields and original is unchanged")
        void shouldCreateNewInstanceWithUpdatedFields() {
            Person original = Person.of(
                    ClientName.of("Original Name"),
                    Email.of("original@example.com"),
                    PhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            Person updated = original.withCommonFields(
                    ClientName.of("Updated Name"),
                    Email.of("updated@example.com"),
                    PhoneNumber.of("+33222222222")
            );

            assertThat(updated.getName().value()).isEqualTo("Updated Name");
            assertThat(updated.getEmail().value()).isEqualTo("updated@example.com");
            assertThat(updated.getPhone().value()).isEqualTo("+33222222222");

            assertThat(original.getName().value()).isEqualTo("Original Name");
            assertThat(original.getEmail().value()).isEqualTo("original@example.com");
            assertThat(original.getPhone().value()).isEqualTo("+33111111111");

            assertThat(updated.getId()).isEqualTo(original.getId());
        }

        @Test
        @DisplayName("GIVEN null fields WHEN creating updated instance THEN throw exception")
        void shouldRejectNullFieldsOnUpdate() {
            Person person = Person.of(
                    ClientName.of("Test User"),
                    Email.of("test@example.com"),
                    PhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            assertThatThrownBy(() -> person.withCommonFields(null, Email.of("test@test.com"), PhoneNumber.of("+33123456789")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(Client.NULL_NAME_MSG);

            assertThatThrownBy(() -> person.withCommonFields(ClientName.of("Test"), null, PhoneNumber.of("+33123456789")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(Client.NULL_EMAIL_MSG);

            assertThatThrownBy(() -> person.withCommonFields(ClientName.of("Test"), Email.of("test@test.com"), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(Client.NULL_PHONE_MSG);
        }
    }

}

