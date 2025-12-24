package com.mk.contractservice.domain.client;


import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.exception.InvalidClientException;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
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
                    ClientEmail.of("test@example.com"),
                    ClientPhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            assertThat(person.getName().getValue()).isEqualTo("Test User");
            assertThat(person.getEmail().getValue()).isEqualTo("test@example.com");
            assertThat(person.getPhone().getValue()).isEqualTo("+33123456789");
        }

        @Test
        @DisplayName("GIVEN null name WHEN creating Client THEN throw exception")
        void shouldRejectNullName() {
            final ClientEmail clientEmail = ClientEmail.of("test@example.com");
            final ClientPhoneNumber phoneNumber = ClientPhoneNumber.of("+33123456789");
            final PersonBirthDate birthDate = PersonBirthDate.of(LocalDate.of(1990, 1, 1));

            assertThatThrownBy(() -> Person.of(
                    null,
                    clientEmail,
                    phoneNumber,
                    birthDate
            ))
                    .isInstanceOf(InvalidClientException.class)
                    .hasMessage(InvalidClientException.forNullName().getMessage());
        }

        @Test
        @DisplayName("GIVEN null email WHEN creating Client THEN throw exception")
        void shouldRejectNullEmail() {
            final ClientName clientName = ClientName.of("Test User");
            final ClientPhoneNumber phoneNumber = ClientPhoneNumber.of("+33123456789");
            final PersonBirthDate birthDate = PersonBirthDate.of(LocalDate.of(1990, 1, 1));

            assertThatThrownBy(() -> Person.of(
                    clientName,
                    null,
                    phoneNumber,
                    birthDate
            ))
                    .isInstanceOf(InvalidClientException.class)
                    .hasMessage(InvalidClientException.forNullEmail().getMessage());
        }

        @Test
        @DisplayName("GIVEN null phone WHEN creating Client THEN throw exception")
        void shouldRejectNullPhone() {
            final ClientName clientName = ClientName.of("Test User");
            final ClientEmail clientEmail = ClientEmail.of("test@example.com");
            final PersonBirthDate birthDate = PersonBirthDate.of(LocalDate.of(1990, 1, 1));

            assertThatThrownBy(() -> Person.of(
                    clientName,
                    clientEmail,
                    null,
                    birthDate
            ))
                    .isInstanceOf(InvalidClientException.class)
                    .hasMessage(InvalidClientException.forNullPhone().getMessage());
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
                    ClientEmail.of("original@example.com"),
                    ClientPhoneNumber.of("+33111111111"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            Person updated = original.withCommonFields(
                    ClientName.of("Updated Name"),
                    ClientEmail.of("updated@example.com"),
                    ClientPhoneNumber.of("+33222222222")
            );

            assertThat(updated.getName().getValue()).isEqualTo("Updated Name");
            assertThat(updated.getEmail().getValue()).isEqualTo("updated@example.com");
            assertThat(updated.getPhone().getValue()).isEqualTo("+33222222222");

            assertThat(original.getName().getValue()).isEqualTo("Original Name");
            assertThat(original.getEmail().getValue()).isEqualTo("original@example.com");
            assertThat(original.getPhone().getValue()).isEqualTo("+33111111111");

            assertThat(updated.getId()).isEqualTo(original.getId());
        }

        @Test
        @DisplayName("GIVEN null fields WHEN creating updated instance THEN throw exception")
        void shouldRejectNullFieldsOnUpdate() {
            Person person = Person.of(
                    ClientName.of("Test User"),
                    ClientEmail.of("test@example.com"),
                    ClientPhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            final ClientName name = ClientName.of("Test");
            final ClientEmail email = ClientEmail.of("test@test.com");
            final ClientPhoneNumber phoneNumber = ClientPhoneNumber.of("+33123456789");
            assertThatThrownBy(() -> person.withCommonFields(null, email, phoneNumber))
                    .isInstanceOf(InvalidClientException.class)
                    .hasMessage(InvalidClientException.forNullName().getMessage());
            assertThatThrownBy(() -> person.withCommonFields(name, null, phoneNumber))
                    .isInstanceOf(InvalidClientException.class)
                    .hasMessage(InvalidClientException.forNullEmail().getMessage());
            assertThatThrownBy(() -> person.withCommonFields(name, email, null))
                    .isInstanceOf(InvalidClientException.class)
                    .hasMessage(InvalidClientException.forNullPhone().getMessage());
        }
    }

}

