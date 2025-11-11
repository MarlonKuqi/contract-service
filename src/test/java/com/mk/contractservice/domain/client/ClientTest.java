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
            Person person = Person.builder()
                    .name(ClientName.of("Test User"))
                    .email(Email.of("test@example.com"))
                    .phone(PhoneNumber.of("+33123456789"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build();

            assertThat(person.getName().value()).isEqualTo("Test User");
            assertThat(person.getEmail().value()).isEqualTo("test@example.com");
            assertThat(person.getPhone().value()).isEqualTo("+33123456789");
        }

        @Test
        @DisplayName("GIVEN null name WHEN creating Client THEN throw exception")
        void shouldRejectNullName() {
            assertThatThrownBy(() -> Person.builder()
                    .name(null)
                    .email(Email.of("test@example.com"))
                    .phone(PhoneNumber.of("+33123456789"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(Client.NULL_NAME_MSG);
        }

        @Test
        @DisplayName("GIVEN null email WHEN creating Client THEN throw exception")
        void shouldRejectNullEmail() {
            assertThatThrownBy(() -> Person.builder()
                    .name(ClientName.of("Test User"))
                    .email(null)
                    .phone(PhoneNumber.of("+33123456789"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(Client.NULL_EMAIL_MSG);
        }

        @Test
        @DisplayName("GIVEN null phone WHEN creating Client THEN throw exception")
        void shouldRejectNullPhone() {
            assertThatThrownBy(() -> Person.builder()
                    .name(ClientName.of("Test User"))
                    .email(Email.of("test@example.com"))
                    .phone(null)
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(Client.NULL_PHONE_MSG);
        }
    }

    @Nested
    @DisplayName("updateCommonFields - Subject requirement: Update all fields except birthdate/companyIdentifier")
    class UpdateCommonFieldsValidation {

        @Test
        @DisplayName("GIVEN valid fields WHEN updating THEN all common fields are updated")
        void shouldUpdateAllCommonFields() {
            Person person = Person.builder()
                    .name(ClientName.of("Original Name"))
                    .email(Email.of("original@example.com"))
                    .phone(PhoneNumber.of("+33111111111"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build();

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
            Person person = Person.builder()
                    .name(ClientName.of("Test User"))
                    .email(Email.of("test@example.com"))
                    .phone(PhoneNumber.of("+33123456789"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build();

            assertThatThrownBy(() -> person.updateCommonFields(null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name")
                    .hasMessageContaining("email")
                    .hasMessageContaining("phone");
        }
    }

    @Nested
    @DisplayName("changeName - Partial update support")
    class ChangeNameValidation {

        @Test
        @DisplayName("GIVEN valid name WHEN updating name only THEN only name is changed")
        void shouldUpdateOnlyName() {
            Person person = Person.builder()
                    .name(ClientName.of("Original Name"))
                    .email(Email.of("original@example.com"))
                    .phone(PhoneNumber.of("+33111111111"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build();

            person.changeName(ClientName.of("New Name"));

            assertThat(person.getName().value()).isEqualTo("New Name");
            assertThat(person.getEmail().value()).isEqualTo("original@example.com");
            assertThat(person.getPhone().value()).isEqualTo("+33111111111");
        }

        @Test
        @DisplayName("GIVEN null name WHEN updating name THEN throw exception")
        void shouldRejectNullName() {
            Person person = Person.builder()
                    .name(ClientName.of("Test User"))
                    .email(Email.of("test@example.com"))
                    .phone(PhoneNumber.of("+33123456789"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build();

            assertThatThrownBy(() -> person.changeName(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(Client.NULL_NAME_MSG);
        }
    }

    @Nested
    @DisplayName("changeEmail - Partial update support")
    class ChangeEmailValidation {

        @Test
        @DisplayName("GIVEN valid email WHEN updating email only THEN only email is changed")
        void shouldUpdateOnlyEmail() {
            Person person = Person.builder()
                    .name(ClientName.of("Original Name"))
                    .email(Email.of("original@example.com"))
                    .phone(PhoneNumber.of("+33111111111"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build();

            person.changeEmail(Email.of("new@example.com"));

            assertThat(person.getName().value()).isEqualTo("Original Name");
            assertThat(person.getEmail().value()).isEqualTo("new@example.com");
            assertThat(person.getPhone().value()).isEqualTo("+33111111111");
        }

        @Test
        @DisplayName("GIVEN null email WHEN updating email THEN throw exception")
        void shouldRejectNullEmail() {
            Person person = Person.builder()
                    .name(ClientName.of("Test User"))
                    .email(Email.of("test@example.com"))
                    .phone(PhoneNumber.of("+33123456789"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build();

            assertThatThrownBy(() -> person.changeEmail(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(Client.NULL_EMAIL_MSG);
        }
    }

    @Nested
    @DisplayName("changePhone - Partial update support")
    class ChangePhoneValidation {

        @Test
        @DisplayName("GIVEN valid phone WHEN updating phone only THEN only phone is changed")
        void shouldUpdateOnlyPhone() {
            Person person = Person.builder()
                    .name(ClientName.of("Original Name"))
                    .email(Email.of("original@example.com"))
                    .phone(PhoneNumber.of("+33111111111"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build();

            person.changePhone(PhoneNumber.of("+33999999999"));

            assertThat(person.getName().value()).isEqualTo("Original Name");
            assertThat(person.getEmail().value()).isEqualTo("original@example.com");
            assertThat(person.getPhone().value()).isEqualTo("+33999999999");
        }

        @Test
        @DisplayName("GIVEN null phone WHEN updating phone THEN throw exception")
        void shouldRejectNullPhone() {
            Person person = Person.builder()
                    .name(ClientName.of("Test User"))
                    .email(Email.of("test@example.com"))
                    .phone(PhoneNumber.of("+33123456789"))
                    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
                    .build();

            assertThatThrownBy(() -> person.changePhone(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(Client.NULL_PHONE_MSG);
        }
    }
}

