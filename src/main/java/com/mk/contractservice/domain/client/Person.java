package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public final class Person extends Client {

    PersonBirthDate birthDate;

    private Person(final UUID id, final ClientName name, final Email email, final PhoneNumber phone, final PersonBirthDate birthDate) {
        super(id, name, email, phone);
        this.birthDate = birthDate;
        checkInvariants();
    }

    @Override
    protected void checkInvariants() {
        super.checkInvariants();
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date must not be null");
        }
    }

    public static Person of(final ClientName name, final Email email, final PhoneNumber phone, final PersonBirthDate birthDate) {
        return builder()
                .name(name)
                .email(email)
                .phone(phone)
                .birthDate(birthDate)
                .build();
    }

    public static Person reconstitute(final UUID id, final ClientName name, final Email email, final PhoneNumber phone, final PersonBirthDate birthDate) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null when reconstituting a Person");
        }
        return builder()
                .id(id)
                .name(name)
                .email(email)
                .phone(phone)
                .birthDate(birthDate)
                .build();
    }

    public Person withCommonFields(final ClientName name, final Email email, final PhoneNumber phone) {
        return toBuilder()
                .name(name)
                .email(email)
                .phone(phone)
                .build();
    }

    @Override
    public Person updatePartial(final ClientName name, final Email email, final PhoneNumber phone) {
        return toBuilder()
                .name(name != null ? name : this.getName())
                .email(email != null ? email : this.getEmail())
                .phone(phone != null ? phone : this.getPhone())
                .build();
    }

    public static PersonBuilder builder() {
        return new PersonBuilder();
    }

    public PersonBuilder toBuilder() {
        return builder()
                .id(this.getId())
                .name(this.getName())
                .email(this.getEmail())
                .phone(this.getPhone())
                .birthDate(this.birthDate);
    }

    @NoArgsConstructor
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE)
    public static class PersonBuilder {
        UUID id;
        ClientName name;
        Email email;
        PhoneNumber phone;
        PersonBirthDate birthDate;

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

