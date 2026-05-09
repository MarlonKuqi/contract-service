package com.mk.contractservice.domain.client;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Getter
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
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
        this.birthDate = notNull(birthDate);
    }

    public static Person of(
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone,
            @Nullable final PersonBirthDate birthDate
    ) {
        return builder()
                .name(notNull(name))
                .email(notNull(email))
                .phone(notNull(phone))
                .birthDate(notNull(birthDate))
                .build();
    }

    public static Person reconstituteFromDatabase(
            @Nullable final UUID id,
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone,
            @Nullable final PersonBirthDate birthDate
    ) {
        return builder()
                .id(notNull(id))
                .name(notNull(name))
                .email(notNull(email))
                .phone(notNull(phone))
                .birthDate(notNull(birthDate))
                .build();
    }

    @Override
    public Person changeCoreFields(
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phoneNumber) {
        return toBuilder()
                .name(notNull(name))
                .email(notNull(email))
                .phone(notNull(phoneNumber))
                .build();
    }

    @Override
    public Person changeName(@Nullable final ClientName name) {
        return toBuilder().name(notNull(name)).build();
    }

    @Override
    public Person changeEmail(@Nullable final ClientEmail email) {
        return toBuilder().email(notNull(email)).build();
    }

    @Override
    public Person changePhone(@Nullable final ClientPhoneNumber phoneNumber) {
        return toBuilder().phone(notNull(phoneNumber)).build();
    }

    public static PersonBuilder builder() {
        return new PersonBuilder();
    }

    private PersonBuilder toBuilder() {
        return builder()
                .id(getId())
                .name(getName())
                .email(getEmail())
                .phone(getPhone())
                .birthDate(getBirthDate());
    }

    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
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

