package com.mk.contractservice.domain.client;


import com.mk.contractservice.domain.client.exception.InvalidClientEmailException;
import com.mk.contractservice.domain.client.exception.InvalidClientNameException;
import com.mk.contractservice.domain.client.exception.InvalidClientPhoneNumberException;
import com.mk.contractservice.domain.client.exception.InvalidPersonBirthDateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Person - Domain Tests")
class PersonTest {
    static Stream<LocalDate> validBirthDates() {
        return Stream.of(
                LocalDate.now(),
                LocalDate.of(1990, 5, 15),
                LocalDate.of(1950, 1, 1),
                LocalDate.of(2000, 12, 31),
                LocalDate.now().minusYears(18),
                LocalDate.now().minusDays(1)
        );
    }

    static Stream<LocalDate> invalidBirthDates() {
        return Stream.of(
                null,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusWeeks(1),
                LocalDate.now().plusMonths(1),
                LocalDate.now().plusYears(1),
                LocalDate.now().plusYears(10),
                LocalDate.of(2100, 1, 1),
                LocalDate.MAX
        );
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Nested
        @DisplayName("From Command")
        class FromCommand {

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.ClientTestData#invalidNames")
            @DisplayName("GIVEN invalid name WHEN creating Person THEN throw exception")
            void shouldRejectInvalidName(String name) {
                assertThatThrownBy(() -> PersonFactory.createFromCommand(
                        name,
                        "john@example.com",
                        "+33123456789",
                        LocalDate.of(1990, 5, 15)
                )).isInstanceOf(InvalidClientNameException.class);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.ClientTestData#invalidEmails")
            @DisplayName("GIVEN invalid email WHEN creating Person THEN throw exception")
            void shouldRejectInvalidEmail(String email) {
                assertThatThrownBy(() -> PersonFactory.createFromCommand(
                        "John Doe",
                        email,
                        "+33123456789",
                        LocalDate.of(1990, 5, 15)
                )).isInstanceOf(InvalidClientEmailException.class);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.ClientTestData#invalidPhones")
            @DisplayName("GIVEN invalid phone WHEN creating Person THEN throw exception")
            void shouldRejectInvalidPhone(String phone) {
                assertThatThrownBy(() -> PersonFactory.createFromCommand(
                        "John Doe",
                        "john@example.com",
                        phone,
                        LocalDate.of(1990, 5, 15)
                )).isInstanceOf(InvalidClientPhoneNumberException.class);
            }


            @Test
            @DisplayName("GIVEN null birthDate WHEN creating Person THEN throw InvalidClientException")
            void shouldRejectNullPersonBirthDate() {
                assertThatThrownBy(() -> PersonFactory.createFromCommand(
                        "John Doe",
                        "john@example.com",
                        "+33123456789",
                        null
                )).isInstanceOf(InvalidPersonBirthDateException.class);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.ClientTestData#validPersons")
            @DisplayName("GIVEN various valid combinations of fields WHEN creating Person THEN person is created with all correct values")
            void shouldCreatePersonWithValidCombinations(ClientTestData.ValidPersonData data) {
                Person person = PersonFactory.createFromCommand(
                        data.name(),
                        data.email(),
                        data.phone(),
                        data.birthDate()
                );

                assertThat(person.getName().getValue()).isEqualTo(data.name());
                assertThat(person.getEmail().getValue()).isEqualTo(data.email());
                assertThat(person.getPhone().getValue()).isEqualTo(data.expectedPhone());
                assertThat(person.getBirthDate().getValue()).isEqualTo(data.birthDate());
            }
        }

        @Nested
        @DisplayName("PersonBirthDate Validation")
        class PersonBirthDateValidation {

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.PersonTest#validBirthDates")
            @DisplayName("GIVEN various valid birth dates WHEN creating Person THEN person is created")
            void shouldAcceptValidBirthDates(LocalDate birthDate) {
                final Person person = PersonFactory.createFromCommand(
                        "John Doe",
                        "john@example.com",
                        "+33123456789",
                        birthDate
                );

                assertThat(person.getBirthDate().getValue()).isEqualTo(birthDate);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.PersonTest#invalidBirthDates")
            @DisplayName("GIVEN various valid birth dates WHEN creating Person THEN throw InvalidPersonBirthDateException")
            void shouldRejectInvalidBirthDates(LocalDate birthDate) {
                assertThatThrownBy(() -> PersonFactory.createFromCommand(
                        "John Doe",
                        "john@example.com",
                        "+33123456789",
                        birthDate
                )).isInstanceOf(InvalidPersonBirthDateException.class);
            }
        }
    }

    @Nested
    @DisplayName("Update")
    class Update {

        @Test
        @DisplayName("GIVEN person WHEN updating common fields THEN birthdate remains unchanged")
        void shouldKeepBirthdateImmutableWhenUpdating() {
            PersonBirthDate originalBirthDate = PersonBirthDate.of(LocalDate.of(1990, 5, 15));
            Person person = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john.doe@example.com"),
                    ClientPhoneNumber.of("+33123456789"),
                    originalBirthDate
            );

            Person updated = person.changeCoreFields(
                    ClientName.of("Jane Doe"),
                    ClientEmail.of("jane.doe@example.com"),
                    ClientPhoneNumber.of("+33987654321")
            );

            assertThat(updated.getName().getValue()).isEqualTo("Jane Doe");
            assertThat(updated.getEmail().getValue()).isEqualTo("jane.doe@example.com");
            assertThat(updated.getPhone().getValue()).isEqualTo("+33987654321");
            assertThat(updated.getBirthDate()).isEqualTo(originalBirthDate);

            assertThat(person.getName().getValue()).isEqualTo("John Doe");
            assertThat(person.getBirthDate()).isEqualTo(originalBirthDate);
        }

        @Test
        @DisplayName("GIVEN person WHEN updating with same values THEN returns new instance with same data")
        void shouldReturnNewInstanceWithSameData() {
            Person person = PersonFactory.createFromCommand(
                    "John Doe",
                    "john@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 5, 15)
            );

            Person updated = person.changeCoreFields(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33123456789")
            );

            assertThat(updated).isNotSameAs(person);
            assertThat(updated.getName().getValue()).isEqualTo(person.getName().getValue());
            assertThat(updated.getEmail().getValue()).isEqualTo(person.getEmail().getValue());
            assertThat(updated.getPhone().getValue()).isEqualTo(person.getPhone().getValue());
        }

        @Test
        @DisplayName("GIVEN person WHEN updating only name THEN only name changes")
        void shouldUpdateOnlyName() {
            Person person = PersonFactory.createFromCommand(
                    "John Doe",
                    "john@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 5, 15)
            );

            Person updated = person.changeCoreFields(
                    ClientName.of("Jane Doe"),
                    person.getEmail(),
                    person.getPhone()
            );

            assertThat(updated.getName().getValue()).isEqualTo("Jane Doe");
            assertThat(updated.getEmail()).isEqualTo(person.getEmail());
            assertThat(updated.getPhone()).isEqualTo(person.getPhone());
            assertThat(updated.getBirthDate()).isEqualTo(person.getBirthDate());
        }

        @Test
        @DisplayName("GIVEN person WHEN updating only email THEN only email changes")
        void shouldUpdateOnlyEmail() {
            Person person = PersonFactory.createFromCommand(
                    "John Doe",
                    "john@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 5, 15)
            );

            Person updated = person.changeCoreFields(
                    person.getName(),
                    ClientEmail.of("newemail@example.com"),
                    person.getPhone()
            );

            assertThat(updated.getName()).isEqualTo(person.getName());
            assertThat(updated.getEmail().getValue()).isEqualTo("newemail@example.com");
            assertThat(updated.getPhone()).isEqualTo(person.getPhone());
        }

        @Test
        @DisplayName("GIVEN person WHEN updating only phone THEN only phone changes")
        void shouldUpdateOnlyPhone() {
            Person person = PersonFactory.createFromCommand(
                    "John Doe",
                    "john@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 5, 15)
            );

            Person updated = person.changeCoreFields(
                    person.getName(),
                    person.getEmail(),
                    ClientPhoneNumber.of("+33999999999")
            );

            assertThat(updated.getName()).isEqualTo(person.getName());
            assertThat(updated.getEmail()).isEqualTo(person.getEmail());
            assertThat(updated.getPhone().getValue()).isEqualTo("+33999999999");
        }
    }
}
