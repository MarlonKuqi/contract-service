package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import org.junit.jupiter.api.DisplayName;
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
        Email email = Email.of("john.doe@example.com");
        PhoneNumber phone = PhoneNumber.of("+33123456789");
        PersonBirthDate birthDate = PersonBirthDate.of(LocalDate.of(1990, 5, 15));
        Person person = Person.of(name, email, phone, birthDate);
        assertThat(person.getName()).isEqualTo(name);
        assertThat(person.getEmail()).isEqualTo(email);
        assertThat(person.getPhone()).isEqualTo(phone);
        assertThat(person.getBirthDate()).isEqualTo(birthDate);
    }

    @Test
    @DisplayName("Should reject null PersonBirthDate in constructor - domain protection")
    void shouldRejectNullPersonBirthDateInConstructor() {
        ClientName name = ClientName.of("John Doe");
        Email email = Email.of("john@example.com");
        PhoneNumber phone = PhoneNumber.of("+33123456789");

        assertThatThrownBy(() -> Person.of(name, email, phone, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Birth date must not be null");
    }

    @Test
    @DisplayName("Should keep birthdate immutable when updating common fields")
    void shouldKeepBirthdateImmutable() {
        PersonBirthDate originalBirthDate = PersonBirthDate.of(LocalDate.of(1990, 5, 15));
        Person person = Person.of(
                ClientName.of("John Doe"),
                Email.of("john.doe@example.com"),
                PhoneNumber.of("+33123456789"),
                originalBirthDate);

        Person updated = person.withCommonFields(
                ClientName.of("Jane Doe"),
                Email.of("jane.doe@example.com"),
                PhoneNumber.of("+33987654321")
        );

        // Updated instance has new common fields
        assertThat(updated.getName().value()).isEqualTo("Jane Doe");
        assertThat(updated.getEmail().value()).isEqualTo("jane.doe@example.com");
        assertThat(updated.getPhone().value()).isEqualTo("+33987654321");

        // But birthdate remains the same
        assertThat(updated.getBirthDate()).isEqualTo(originalBirthDate);

        // Original instance is unchanged
        assertThat(person.getName().value()).isEqualTo("John Doe");
        assertThat(person.getBirthDate()).isEqualTo(originalBirthDate);
    }

    @Test
    @DisplayName("Should be instance of Client")
    void shouldBeInstanceOfClient() {
        Person person = Person.of(
                ClientName.of("John Doe"),
                Email.of("john@example.com"),
                PhoneNumber.of("+33123456789"),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15)));

        assertThat(person).isInstanceOf(Client.class);
        assertThat(person).isInstanceOf(Person.class);
    }

    @Test
    @DisplayName("Should reconstitute Person with ID from database")
    void shouldReconstitutPersonWithId() {
        UUID id = UUID.randomUUID();
        ClientName name = ClientName.of("John Doe");
        Email email = Email.of("john@example.com");
        PhoneNumber phone = PhoneNumber.of("+33123456789");
        PersonBirthDate birthDate = PersonBirthDate.of(LocalDate.of(1990, 5, 15));

        Person person = Person.reconstitute(id, name, email, phone, birthDate);

        assertThat(person.getId()).isEqualTo(id);
        assertThat(person.getName()).isEqualTo(name);
        assertThat(person.getEmail()).isEqualTo(email);
        assertThat(person.getPhone()).isEqualTo(phone);
        assertThat(person.getBirthDate()).isEqualTo(birthDate);
    }

    @Test
    @DisplayName("Should reject null ID when reconstituting")
    void shouldRejectNullIdOnReconstitute() {
        assertThatThrownBy(() -> Person.reconstitute(
                null,
                ClientName.of("John Doe"),
                Email.of("john@example.com"),
                PhoneNumber.of("+33123456789"),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID must not be null when reconstituting");
    }

    @Test
    @DisplayName("Should update partial fields keeping others unchanged")
    void shouldUpdatePartialFields() {
        Person person = Person.of(
                ClientName.of("John Doe"),
                Email.of("john@example.com"),
                PhoneNumber.of("+33123456789"),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15))
        );

        Person updated = person.updatePartial(
                ClientName.of("Jane Doe"),
                null,
                null
        );

        assertThat(updated.getName().value()).isEqualTo("Jane Doe");
        assertThat(updated.getEmail().value()).isEqualTo("john@example.com");
        assertThat(updated.getPhone().value()).isEqualTo("+33123456789");
        assertThat(updated.getBirthDate().value()).isEqualTo(LocalDate.of(1990, 5, 15));
    }

    @Test
    @DisplayName("Should update all fields when all provided in updatePartial")
    void shouldUpdateAllFieldsWhenAllProvided() {
        Person person = Person.of(
                ClientName.of("John Doe"),
                Email.of("john@example.com"),
                PhoneNumber.of("+33123456789"),
                PersonBirthDate.of(LocalDate.of(1990, 5, 15))
        );

        Person updated = person.updatePartial(
                ClientName.of("Jane Smith"),
                Email.of("jane@example.com"),
                PhoneNumber.of("+33987654321")
        );

        assertThat(updated.getName().value()).isEqualTo("Jane Smith");
        assertThat(updated.getEmail().value()).isEqualTo("jane@example.com");
        assertThat(updated.getPhone().value()).isEqualTo("+33987654321");
        assertThat(updated.getBirthDate().value()).isEqualTo(LocalDate.of(1990, 5, 15));
    }

    @Test
    @DisplayName("Should keep birthdate immutable on updatePartial")
    void shouldKeepBirthdateImmutableOnUpdatePartial() {
        PersonBirthDate originalBirthDate = PersonBirthDate.of(LocalDate.of(1990, 5, 15));
        Person person = Person.of(
                ClientName.of("John Doe"),
                Email.of("john@example.com"),
                PhoneNumber.of("+33123456789"),
                originalBirthDate
        );

        Person updated = person.updatePartial(
                ClientName.of("Jane Doe"),
                Email.of("jane@example.com"),
                PhoneNumber.of("+33987654321")
        );

        assertThat(updated.getBirthDate()).isEqualTo(originalBirthDate);
    }
}

