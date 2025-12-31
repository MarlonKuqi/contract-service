package com.mk.contractservice.domain.client;


import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.aggregate.Person;
import com.mk.contractservice.domain.client.exception.InvalidClientException;
import com.mk.contractservice.domain.client.exception.InvalidPersonBirthDateException;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Person - Domain Entity Tests")
class PersonTest {

    @Test
    @DisplayName("Should create Person with all required fields")
    void shouldCreatePersonWithAllRequiredFields() {
        ClientName name = ClientName.of("John Doe");
        ClientEmail clientEmail = ClientEmail.of("john.doe@example.com");
        ClientPhoneNumber phone = ClientPhoneNumber.of("+33123456789");
        PersonBirthDate birthDate = PersonBirthDate.of(LocalDate.of(1990, 5, 15));
        Person person = Person.of(name, clientEmail, phone, birthDate);
        assertThat(person.getName()).isEqualTo(name);
        assertThat(person.getEmail()).isEqualTo(clientEmail);
        assertThat(person.getPhone()).isEqualTo(phone);
        assertThat(person.getBirthDate()).isEqualTo(birthDate);
    }

    @Test
    @DisplayName("Should reject null PersonBirthDate in constructor - domain protection")
    void shouldRejectNullPersonBirthDateInConstructor() {
        ClientName name = ClientName.of("John Doe");
        ClientEmail clientEmail = ClientEmail.of("john@example.com");
        ClientPhoneNumber phone = ClientPhoneNumber.of("+33123456789");

        assertThatThrownBy(() -> Person.of(name, clientEmail, phone, null))
                .isInstanceOf(InvalidClientException.class)
                .hasMessage(InvalidClientException.forNullBirthDate().getMessage());
    }

    @Test
    @DisplayName("Should keep birthdate immutable when updating common fields")
    void shouldKeepBirthdateImmutable() {
        PersonBirthDate originalBirthDate = PersonBirthDate.of(LocalDate.of(1990, 5, 15));
        Person person = Person.of(
                ClientName.of("John Doe"),
                ClientEmail.of("john.doe@example.com"),
                ClientPhoneNumber.of("+33123456789"),
                originalBirthDate);

        Person updated = person.changeCoreFields(
                "Jane Doe",
                "jane.doe@example.com",
                "+33987654321"
        );

        assertThat(updated.getName().getValue()).isEqualTo("Jane Doe");
        assertThat(updated.getEmail().getValue()).isEqualTo("jane.doe@example.com");
        assertThat(updated.getPhone().getValue()).isEqualTo("+33987654321");

        assertThat(updated.getBirthDate()).isEqualTo(originalBirthDate);

        assertThat(person.getName().getValue()).isEqualTo("John Doe");
        assertThat(person.getBirthDate()).isEqualTo(originalBirthDate);
    }

    @Test
    @DisplayName("Should be instance of Client")
    void shouldBeInstanceOfClient() {
        Person person = Person.of(
                ClientName.of("John Doe"),
                ClientEmail.of("john@example.com"),
                ClientPhoneNumber.of("+33123456789"),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15)));

        assertThat(person)
                .isInstanceOf(Client.class)
                .isInstanceOf(Person.class);
    }

    @Test
    @DisplayName("Should reconstitute Person with ID from database")
    void shouldReconstitutPersonWithId() {
        UUID id = UUID.randomUUID();
        ClientName name = ClientName.of("John Doe");
        ClientEmail clientEmail = ClientEmail.of("john@example.com");
        ClientPhoneNumber phone = ClientPhoneNumber.of("+33123456789");
        PersonBirthDate birthDate = PersonBirthDate.of(LocalDate.of(1990, 5, 15));

        Person person = Person.reconstituteFromDatabase(id, name, clientEmail, phone, birthDate);

        assertThat(person.getId()).isEqualTo(id);
        assertThat(person.getName()).isEqualTo(name);
        assertThat(person.getEmail()).isEqualTo(clientEmail);
        assertThat(person.getPhone()).isEqualTo(phone);
        assertThat(person.getBirthDate()).isEqualTo(birthDate);
    }

    @Nested
    @DisplayName("PersonBirthDate validation via Person.of()")
    class PersonBirthDateValidation {

        @Test
        @DisplayName("GIVEN valid birth date in past WHEN creating Person THEN person is created")
        void shouldAcceptValidBirthDateInPast() {
            Person person = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(LocalDate.of(1990, 5, 15))
            );

            assertThat(person.getBirthDate().getValue()).isEqualTo(LocalDate.of(1990, 5, 15));
        }

        @Test
        @DisplayName("GIVEN birth date today WHEN creating Person THEN person is created")
        void shouldAcceptBirthDateToday() {
            LocalDate today = LocalDate.now();
            Person person = Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(today)
            );

            assertThat(person.getBirthDate().getValue()).isEqualTo(today);
        }

        @Test
        @DisplayName("GIVEN future birth date WHEN creating Person THEN throw InvalidPersonBirthDateException")
        void shouldRejectFutureBirthDate() {
            LocalDate futureDate = LocalDate.now().plusDays(1);

            assertThatThrownBy(() -> Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(futureDate)
            ))
                    .isInstanceOf(InvalidPersonBirthDateException.class);
        }

        @Test
        @DisplayName("GIVEN null birth date value WHEN creating Person THEN throw InvalidPersonBirthDateException")
        void shouldRejectNullBirthDateValue() {
            assertThatThrownBy(() -> Person.of(
                    ClientName.of("John Doe"),
                    ClientEmail.of("john@example.com"),
                    ClientPhoneNumber.of("+33123456789"),
                    PersonBirthDate.of(null)
            ))
                    .isInstanceOf(InvalidPersonBirthDateException.class);
        }
    }
}

