package com.mk.contractservice.domain.client;


import com.mk.contractservice.domain.client.exception.InvalidClientEmailException;
import com.mk.contractservice.domain.client.exception.InvalidClientNameException;
import com.mk.contractservice.domain.client.exception.InvalidClientPhoneNumberException;
import com.mk.contractservice.domain.client.exception.InvalidPersonBirthDateException;
import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.UUID;
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
                        LocalDate.of(1990, 5, 15),
                        LocalDate.now()
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
                        LocalDate.of(1990, 5, 15),
                        LocalDate.now()
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
                        LocalDate.of(1990, 5, 15),
                        LocalDate.now()
                )).isInstanceOf(InvalidClientPhoneNumberException.class);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.ClientTestData#acceptedPhoneFormats")
            @DisplayName("GIVEN phone in accepted format WHEN creating Person THEN phone is normalized to E.164")
            void shouldNormalizePhoneToE164(ClientTestData.PhoneFormat format) {
                final Person person = PersonFactory.createFromCommand(
                        "John Doe",
                        "john@example.com",
                        format.raw(),
                        LocalDate.of(1990, 5, 15),
                        LocalDate.now()
                );

                assertThat(person.getPhone().getValue()).isEqualTo(format.expectedE164());
            }

            @Test
            @DisplayName("GIVEN same phone in different formats WHEN creating Persons THEN phones are equal")
            void shouldConsiderDifferentlyFormattedPhonesEqual() {
                final Person first = PersonFactory.createFromCommand("John Doe", "john@example.com", "+41 79 123 45 67", LocalDate.of(1990, 5, 15), LocalDate.now());
                final Person second = PersonFactory.createFromCommand("Jane Doe", "jane@example.com", "+41791234567", LocalDate.of(1985, 1, 1), LocalDate.now());

                assertThat(first.getPhone()).isEqualTo(second.getPhone());
            }

            @Test
            @DisplayName("GIVEN null birthDate WHEN creating Person THEN throw InvalidClientException")
            void shouldRejectNullPersonBirthDate() {
                assertThatThrownBy(() -> PersonFactory.createFromCommand(
                        "John Doe",
                        "john@example.com",
                        "+33123456789",
                        null,
                        LocalDate.now()
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
                        data.birthDate(),
                        LocalDate.now()
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
                        birthDate,
                        LocalDate.now()
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
                        birthDate,
                        LocalDate.now()
                )).isInstanceOf(InvalidPersonBirthDateException.class);
            }
        }

        @Nested
        @DisplayName("From Database")
        class FromDatabase {

            @Test
            @DisplayName("GIVEN stored E.164 phone WHEN rebuilding Person THEN phone is kept as is")
            void shouldRebuildPersonWithStoredPhone() {
                final Person person = PersonFactory.buildFromDatabase(
                        UUID.randomUUID(),
                        "John Doe",
                        "john@example.com",
                        "+41791234567",
                        LocalDate.of(1990, 5, 15)
                );

                assertThat(person.getPhone().getValue()).isEqualTo("+41791234567");
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.ClientTestData#corruptedStoredPhones")
            @DisplayName("GIVEN corrupted stored phone WHEN rebuilding Person THEN throw InvalidDomainObjectError")
            void shouldRejectCorruptedStoredPhone(String phone) {
                assertThatThrownBy(() -> PersonFactory.buildFromDatabase(
                        UUID.randomUUID(),
                        "John Doe",
                        "john@example.com",
                        phone,
                        LocalDate.of(1990, 5, 15)
                )).isInstanceOf(InvalidDomainObjectError.class);
            }
        }
    }

    @Nested
    @DisplayName("Update")
    class Update {

        @Test
        @DisplayName("GIVEN person WHEN updating common fields THEN birthdate remains unchanged")
        void shouldKeepBirthdateImmutableWhenUpdating() {
            PersonBirthDate originalBirthDate = PersonBirthDate.of(LocalDate.of(1990, 5, 15), LocalDate.now());
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
        @DisplayName("GIVEN person WHEN updating with same values THEN returns the same instance")
        void shouldReturnSameInstanceWhenValuesAreIdentical() {
            Person person = PersonFactory.createFromCommand(
                    "John Doe",
                    "john@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 5, 15),
                    LocalDate.now()
            );

            Person updated = person.changeCoreFields(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33123456789")
            );

            assertThat(updated).isSameAs(person);
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
                    LocalDate.of(1990, 5, 15),
                    LocalDate.now()
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
                    LocalDate.of(1990, 5, 15),
                    LocalDate.now()
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
                    LocalDate.of(1990, 5, 15),
                    LocalDate.now()
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
