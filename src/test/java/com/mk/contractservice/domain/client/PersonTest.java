package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

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
        Person person = Person.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .birthDate(birthDate)
                .build();
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

        assertThatThrownBy(() -> Person.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .birthDate(null)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Birth date must not be null");
    }

    @Test
    @DisplayName("Should keep birthdate immutable after update")
    void shouldKeepBirthdateImmutable() {
        PersonBirthDate originalBirthDate = PersonBirthDate.of(LocalDate.of(1990, 5, 15));
        Person person = Person.builder()
                .name(ClientName.of("John Doe"))
                .email(Email.of("john.doe@example.com"))
                .phone(PhoneNumber.of("+33123456789"))
                .birthDate(originalBirthDate)
                .build();

        person.updateCommonFields(
                ClientName.of("Jane Doe"),
                Email.of("jane.doe@example.com"),
                PhoneNumber.of("+33987654321")
        );

        assertThat(person.getBirthDate()).isEqualTo(originalBirthDate);
    }

    @Test
    @DisplayName("Should be instance of Client")
    void shouldBeInstanceOfClient() {
        Person person = Person.builder()
                .name(ClientName.of("John Doe"))
                .email(Email.of("john@example.com"))
                .phone(PhoneNumber.of("+33123456789"))
                .birthDate(PersonBirthDate.of(LocalDate.of(1990, 5, 15)))
                .build();

        assertThat(person).isInstanceOf(Client.class);
        assertThat(person).isInstanceOf(Person.class);
    }
}

