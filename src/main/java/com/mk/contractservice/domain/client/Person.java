package com.mk.contractservice.domain.client;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Getter
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public final class Person extends Client {

    PersonBirthDate birthDate;

    private Person(
            @Nullable final UUID id,
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone,
            @Nullable final PersonBirthDate birthDate
    ) {
        super(id, name, email, phone);
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date must not be null");
        }
        this.birthDate = birthDate;
    }

    public static Person of(
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone,
            @Nullable final PersonBirthDate birthDate
    ) {
        return builder()
                .name(name)
                .email(email)
                .phone(phone)
                .birthDate(birthDate)
                .build();
    }

    public static Person reconstitute(
            @Nullable final UUID id,
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone,
            @Nullable final PersonBirthDate birthDate
    ) {
        return builder()
                .id(id)
                .name(name)
                .email(email)
                .phone(phone)
                .birthDate(birthDate)
                .build();
    }

    public Person withCommonFields(final ClientName name, final ClientEmail clientEmail, final ClientPhoneNumber phone) {
        return toBuilder()
                .name(name)
                .email(clientEmail)
                .phone(phone)
                .build();
    }

    @Override
    public Person updatePartial(
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone
    ) {
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
    @NullUnmarked
    public static class PersonBuilder {
        UUID id;
        ClientName name;
        ClientEmail email;
        ClientPhoneNumber phone;
        PersonBirthDate birthDate;

        public PersonBuilder id(final UUID id) {
            this.id = id;
            return this;
        }

        public PersonBuilder name(final ClientName name) {
            this.name = name;
            return this;
        }

        public PersonBuilder email(final ClientEmail email) {
            this.email = email;
            return this;
        }

        public PersonBuilder phone(final ClientPhoneNumber phone) {
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

