package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import lombok.Getter;

import java.util.UUID;

@Getter
public final class Person extends Client {

    private final PersonBirthDate birthDate;

    private Person(final UUID id, final ClientName name, final Email email, final PhoneNumber phone, final PersonBirthDate birthDate) {
        super(id, name, email, phone);
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date must not be null");
        }
        this.birthDate = birthDate;
    }


    public static PersonBuilder builder() {
        return new PersonBuilder();
    }

    public static class PersonBuilder {
        private UUID id;
        private ClientName name;
        private Email email;
        private PhoneNumber phone;
        private PersonBirthDate birthDate;

        public PersonBuilder id(final UUID id) {
            this.id = id;
            return this;
        }

        public PersonBuilder name(final ClientName name) {
            this.name = name;
            return this;
        }

        public PersonBuilder email(final Email email) {
            this.email = email;
            return this;
        }

        public PersonBuilder phone(final PhoneNumber phone) {
            this.phone = phone;
            return this;
        }

        public PersonBuilder birthDate(final PersonBirthDate birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        public Person build() {
            return new Person(id, name, email, phone, birthDate);
        }
    }
}
