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
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import static com.mk.contractservice.domain.client.ClientTestData.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Person - Domain Tests")
class PersonTest {
    static Stream<LocalDate> validBirthDates() {
        return Stream.of(
                TODAY,
                LocalDate.of(1990, 5, 15),
                LocalDate.of(1950, 1, 1),
                LocalDate.of(2000, 12, 31),
                TODAY.minusYears(18),
                TODAY.minusDays(1)
        );
    }

    static Stream<LocalDate> invalidBirthDates() {
        return Stream.of(
                null,
                TODAY.plusDays(1),
                TODAY.plusWeeks(1),
                TODAY.plusMonths(1),
                TODAY.plusYears(1),
                TODAY.plusYears(10),
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
                assertThatThrownBy(() -> ClientFixtures.newPerson(
                        name,
                        "john@example.com",
                        "+33123456789",
                        LocalDate.of(1990, 5, 15),
                        TODAY
                )).isInstanceOf(InvalidClientNameException.class);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.ClientTestData#invalidEmails")
            @DisplayName("GIVEN invalid email WHEN creating Person THEN throw exception")
            void shouldRejectInvalidEmail(String email) {
                assertThatThrownBy(() -> ClientFixtures.newPerson(
                        "John Doe",
                        email,
                        "+33123456789",
                        LocalDate.of(1990, 5, 15),
                        TODAY
                )).isInstanceOf(InvalidClientEmailException.class);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.ClientTestData#validEmails")
            @DisplayName("GIVEN valid email with subdomains, apostrophe or punycode WHEN creating Person THEN email is kept")
            void shouldAcceptValidEmail(String email) {
                final Person person = ClientFixtures.newPerson(
                        "John Doe",
                        email,
                        "+33123456789",
                        LocalDate.of(1990, 5, 15),
                        TODAY
                );

                assertThat(person.email().value()).isEqualTo(email);
            }

            @Test
            @DisplayName("GIVEN email with invalid format WHEN creating Person THEN error message does not disclose the address")
            void shouldNotDiscloseInvalidEmailInMessage() {
                assertThatThrownBy(() -> ClientFixtures.newPerson(
                        "John Doe",
                        "jean..dupont@example.ch",
                        "+33123456789",
                        LocalDate.of(1990, 5, 15),
                        TODAY
                )).isInstanceOf(InvalidClientEmailException.class)
                        .message()
                        .doesNotContain("jean..dupont");
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.ClientTestData#invalidPhones")
            @DisplayName("GIVEN invalid phone WHEN creating Person THEN throw exception")
            void shouldRejectInvalidPhone(String phone) {
                assertThatThrownBy(() -> ClientFixtures.newPerson(
                        "John Doe",
                        "john@example.com",
                        phone,
                        LocalDate.of(1990, 5, 15),
                        TODAY
                )).isInstanceOf(InvalidClientPhoneNumberException.class);
            }

            @ParameterizedTest
            @ValueSource(strings = {"", "   "})
            @DisplayName("GIVEN blank name WHEN creating Person THEN throw exception explaining the value is blank")
            void shouldExplainBlankName(String name) {
                assertThatThrownBy(() -> ClientFixtures.newPerson(
                        name,
                        "john@example.com",
                        "+33123456789",
                        LocalDate.of(1990, 5, 15),
                        TODAY
                )).isInstanceOf(InvalidClientNameException.class)
                        .hasMessageContaining("blank");
            }

            @ParameterizedTest
            @ValueSource(strings = {"", "   "})
            @DisplayName("GIVEN blank email WHEN creating Person THEN throw exception explaining the value is blank")
            void shouldExplainBlankEmail(String email) {
                assertThatThrownBy(() -> ClientFixtures.newPerson(
                        "John Doe",
                        email,
                        "+33123456789",
                        LocalDate.of(1990, 5, 15),
                        TODAY
                )).isInstanceOf(InvalidClientEmailException.class)
                        .hasMessageContaining("blank");
            }

            @ParameterizedTest
            @ValueSource(strings = {"", "   "})
            @DisplayName("GIVEN blank phone WHEN creating Person THEN throw exception explaining the value is blank")
            void shouldExplainBlankPhone(String phone) {
                assertThatThrownBy(() -> ClientFixtures.newPerson(
                        "John Doe",
                        "john@example.com",
                        phone,
                        LocalDate.of(1990, 5, 15),
                        TODAY
                )).isInstanceOf(InvalidClientPhoneNumberException.class)
                        .hasMessageContaining("blank");
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.ClientTestData#acceptedPhoneFormats")
            @DisplayName("GIVEN phone in accepted format WHEN creating Person THEN phone is normalized to E.164")
            void shouldNormalizePhoneToE164(ClientTestData.PhoneFormat format) {
                final Person person = ClientFixtures.newPerson(
                        "John Doe",
                        "john@example.com",
                        format.raw(),
                        LocalDate.of(1990, 5, 15),
                        TODAY
                );

                assertThat(person.phone().value()).isEqualTo(format.expectedE164());
            }

            @Test
            @DisplayName("GIVEN same phone in different formats WHEN creating Persons THEN phones are equal")
            void shouldConsiderDifferentlyFormattedPhonesEqual() {
                final Person first = ClientFixtures.newPerson("John Doe",
                        "john@example.com",
                        "+41 79 123 45 67",
                        LocalDate.of(1990, 5, 15),
                        TODAY);
                final Person second = ClientFixtures.newPerson("Jane Doe",
                        "jane@example.com",
                        "+41791234567",
                        LocalDate.of(1985, 1, 1),
                        TODAY);

                assertThat(first.phone()).isEqualTo(second.phone());
            }

            @Test
            @DisplayName("GIVEN null birthDate WHEN creating Person THEN throw InvalidPersonBirthDateException")
            void shouldRejectNullPersonBirthDate() {
                assertThatThrownBy(() -> ClientFixtures.newPerson(
                        "John Doe",
                        "john@example.com",
                        "+33123456789",
                        null,
                        TODAY
                )).isInstanceOf(InvalidPersonBirthDateException.class);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.ClientTestData#validPersons")
            @DisplayName("GIVEN various valid combinations of fields WHEN creating Person THEN person is created with all correct values")
            void shouldCreatePersonWithValidCombinations(ClientTestData.ValidPersonData data) {
                Person person = ClientFixtures.newPerson(
                        data.name(),
                        data.email(),
                        data.phone(),
                        data.birthDate(),
                        TODAY
                );

                assertThat(person.name().value()).isEqualTo(data.name());
                assertThat(person.email().value()).isEqualTo(data.email());
                assertThat(person.phone().value()).isEqualTo(data.expectedPhone());
                assertThat(person.birthDate().value()).isEqualTo(data.birthDate());
            }
        }

        @Nested
        @DisplayName("PersonBirthDate Validation")
        class PersonBirthDateValidation {

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.PersonTest#validBirthDates")
            @DisplayName("GIVEN various valid birth dates WHEN creating Person THEN person is created")
            void shouldAcceptValidBirthDates(LocalDate birthDate) {
                final Person person = ClientFixtures.newPerson(
                        "John Doe",
                        "john@example.com",
                        "+33123456789",
                        birthDate,
                        TODAY
                );

                assertThat(person.birthDate().value()).isEqualTo(birthDate);
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.PersonTest#invalidBirthDates")
            @DisplayName("GIVEN various valid birth dates WHEN creating Person THEN throw InvalidPersonBirthDateException")
            void shouldRejectInvalidBirthDates(LocalDate birthDate) {
                assertThatThrownBy(() -> ClientFixtures.newPerson(
                        "John Doe",
                        "john@example.com",
                        "+33123456789",
                        birthDate,
                        TODAY
                )).isInstanceOf(InvalidPersonBirthDateException.class);
            }
        }

        @Nested
        @DisplayName("From Database")
        class FromDatabase {

            @Test
            @DisplayName("GIVEN stored E.164 phone WHEN rebuilding Person THEN phone is kept as is")
            void shouldRebuildPersonWithStoredPhone() {
                final Person person = ClientFixtures.existingPerson(
                        UUID.randomUUID(),
                        "John Doe",
                        "john@example.com",
                        "+41791234567",
                        LocalDate.of(1990, 5, 15)
                );

                assertThat(person.phone().value()).isEqualTo("+41791234567");
            }

            @ParameterizedTest
            @MethodSource("com.mk.contractservice.domain.client.ClientTestData#corruptedStoredPhones")
            @DisplayName("GIVEN corrupted stored phone WHEN rebuilding Person THEN throw InvalidDomainObjectError")
            void shouldRejectCorruptedStoredPhone(String phone) {
                assertThatThrownBy(() -> ClientFixtures.existingPerson(
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
            PersonBirthDate originalBirthDate = PersonBirthDate.of(LocalDate.of(1990, 5, 15), TODAY);
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

            assertThat(updated.name().value()).isEqualTo("Jane Doe");
            assertThat(updated.email().value()).isEqualTo("jane.doe@example.com");
            assertThat(updated.phone().value()).isEqualTo("+33987654321");
            assertThat(updated.birthDate()).isEqualTo(originalBirthDate);

            assertThat(person.name().value()).isEqualTo("John Doe");
            assertThat(person.birthDate()).isEqualTo(originalBirthDate);
        }

        @Test
        @DisplayName("GIVEN person WHEN updating with same values THEN returns the same instance")
        void shouldReturnSameInstanceWhenValuesAreIdentical() {
            Person person = ClientFixtures.newPerson(
                    "John Doe",
                    "john@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 5, 15),
                    TODAY
            );

            Person updated = person.changeCoreFields(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33123456789")
            );

            assertThat(updated).isSameAs(person);
            assertThat(updated.name().value()).isEqualTo(person.name().value());
            assertThat(updated.email().value()).isEqualTo(person.email().value());
            assertThat(updated.phone().value()).isEqualTo(person.phone().value());
        }

        @Test
        @DisplayName("GIVEN person WHEN updating only name THEN only name changes")
        void shouldUpdateOnlyName() {
            Person person = ClientFixtures.newPerson(
                    "John Doe",
                    "john@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 5, 15),
                    TODAY
            );

            Person updated = person.changeCoreFields(
                    ClientName.of("Jane Doe"),
                    person.email(),
                    person.phone()
            );

            assertThat(updated.name().value()).isEqualTo("Jane Doe");
            assertThat(updated.email()).isEqualTo(person.email());
            assertThat(updated.phone()).isEqualTo(person.phone());
            assertThat(updated.birthDate()).isEqualTo(person.birthDate());
        }

        @Test
        @DisplayName("GIVEN person WHEN updating only email THEN only email changes")
        void shouldUpdateOnlyEmail() {
            Person person = ClientFixtures.newPerson(
                    "John Doe",
                    "john@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 5, 15),
                    TODAY
            );

            Person updated = person.changeCoreFields(
                    person.name(),
                    ClientEmail.of("newemail@example.com"),
                    person.phone()
            );

            assertThat(updated.name()).isEqualTo(person.name());
            assertThat(updated.email().value()).isEqualTo("newemail@example.com");
            assertThat(updated.phone()).isEqualTo(person.phone());
        }

        @Test
        @DisplayName("GIVEN person WHEN updating only phone THEN only phone changes")
        void shouldUpdateOnlyPhone() {
            Person person = ClientFixtures.newPerson(
                    "John Doe",
                    "john@example.com",
                    "+33123456789",
                    LocalDate.of(1990, 5, 15),
                    TODAY
            );

            Person updated = person.changeCoreFields(
                    person.name(),
                    person.email(),
                    ClientPhoneNumber.of("+33999999999")
            );

            assertThat(updated.name()).isEqualTo(person.name());
            assertThat(updated.email()).isEqualTo(person.email());
            assertThat(updated.phone().value()).isEqualTo("+33999999999");
        }
    }
}
