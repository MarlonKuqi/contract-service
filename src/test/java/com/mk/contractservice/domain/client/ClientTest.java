package com.mk.contractservice.domain.client;


import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.exception.InvalidClientEmailException;
import com.mk.contractservice.domain.client.exception.InvalidClientException;
import com.mk.contractservice.domain.client.exception.InvalidClientNameException;
import com.mk.contractservice.domain.client.exception.InvalidClientPhoneNumberException;
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

            Person updated = original.changeCoreFields(
                    "Updated Name",
                    "updated@example.com",
                    "+33222222222"
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

            assertThatThrownBy(() -> person.changeCoreFields(null, "test@test.com", "+33123456789"))
                    .isInstanceOf(InvalidClientNameException.class);
            assertThatThrownBy(() -> person.changeCoreFields("Test", null, "+33123456789"))
                    .isInstanceOf(InvalidClientEmailException.class);
            assertThatThrownBy(() -> person.changeCoreFields("Test", "test@test.com", null))
                    .isInstanceOf(InvalidClientPhoneNumberException.class);
        }
    }

    @Nested
    @DisplayName("ClientName validation via Person.of()")
    class ClientNameValidation {

        @Test
        @DisplayName("GIVEN valid name WHEN creating Person THEN person is created")
        void shouldAcceptValidName() {
            Person person = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            assertThat(person.getName().getValue()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("GIVEN blank name WHEN creating Person THEN throw InvalidClientNameException")
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> Person.of(
                    ClientName.of("   "),
                    ClientEmail.of("test@example.com"),
                    ClientPhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            ))
                    .isInstanceOf(InvalidClientNameException.class);
        }

        @Test
        @DisplayName("GIVEN empty name WHEN creating Person THEN throw InvalidClientNameException")
        void shouldRejectEmptyName() {
            assertThatThrownBy(() -> Person.of(
                    ClientName.of(""),
                    ClientEmail.of("test@example.com"),
                    ClientPhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            ))
                    .isInstanceOf(InvalidClientNameException.class);
        }

        @Test
        @DisplayName("GIVEN null name value WHEN creating Person THEN throw InvalidClientNameException")
        void shouldRejectNullNameValue() {
            assertThatThrownBy(() -> Person.of(
                    ClientName.of(null),
                    ClientEmail.of("test@example.com"),
                    ClientPhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            ))
                    .isInstanceOf(InvalidClientNameException.class);
        }
    }

    @Nested
    @DisplayName("ClientEmail validation via Person.of()")
    class ClientEmailValidation {

        @Test
        @DisplayName("GIVEN valid email WHEN creating Person THEN person is created")
        void shouldAcceptValidEmail() {
            Person person = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john.doe@example.com"),
                    ClientPhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            assertThat(person.getEmail().getValue()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("GIVEN invalid email format WHEN creating Person THEN throw InvalidClientEmailException")
        void shouldRejectInvalidEmailFormat() {
            assertThatThrownBy(() -> Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("not-an-email"),
                    ClientPhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            ))
                    .isInstanceOf(InvalidClientEmailException.class);
        }

        @Test
        @DisplayName("GIVEN null email value WHEN creating Person THEN throw InvalidClientEmailException")
        void shouldRejectNullEmailValue() {
            assertThatThrownBy(() -> Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of(null),
                    ClientPhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            ))
                    .isInstanceOf(InvalidClientEmailException.class);
        }

        @Test
        @DisplayName("GIVEN blank email WHEN creating Person THEN throw InvalidClientEmailException")
        void shouldRejectBlankEmail() {
            assertThatThrownBy(() -> Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("   "),
                    ClientPhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            ))
                    .isInstanceOf(InvalidClientEmailException.class);
        }
    }

    @Nested
    @DisplayName("ClientPhoneNumber validation via Person.of()")
    class ClientPhoneNumberValidation {

        @Test
        @DisplayName("GIVEN valid phone WHEN creating Person THEN person is created")
        void shouldAcceptValidPhone() {
            Person person = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            );

            assertThat(person.getPhone().getValue()).isEqualTo("+33123456789");
        }

        @Test
        @DisplayName("GIVEN phone too short WHEN creating Person THEN throw InvalidClientPhoneNumberException")
        void shouldRejectPhoneTooShort() {
            assertThatThrownBy(() -> Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("123"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            ))
                    .isInstanceOf(InvalidClientPhoneNumberException.class);
        }

        @Test
        @DisplayName("GIVEN phone too long WHEN creating Person THEN throw InvalidClientPhoneNumberException")
        void shouldRejectPhoneTooLong() {
            assertThatThrownBy(() -> Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+331234567890123456789012345"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            ))
                    .isInstanceOf(InvalidClientPhoneNumberException.class);
        }

        @Test
        @DisplayName("GIVEN phone with invalid characters WHEN creating Person THEN throw InvalidClientPhoneNumberException")
        void shouldRejectPhoneWithInvalidCharacters() {
            assertThatThrownBy(() -> Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("abc123xyz"),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            ))
                    .isInstanceOf(InvalidClientPhoneNumberException.class);
        }

        @Test
        @DisplayName("GIVEN null phone value WHEN creating Person THEN throw InvalidClientPhoneNumberException")
        void shouldRejectNullPhoneValue() {
            assertThatThrownBy(() -> Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of(null),
                    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
            ))
                    .isInstanceOf(InvalidClientPhoneNumberException.class);
        }
    }

}

