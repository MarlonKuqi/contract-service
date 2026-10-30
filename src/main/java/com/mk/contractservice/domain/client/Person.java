package com.mk.contractservice.domain.client;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Getter
@EqualsAndHashCode(callSuper = true)
public final class Person extends Client {

    private final PersonBirthDate birthDate;

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
        final ClientName newName = notNull(name);
        final ClientEmail newEmail = notNull(email);
        final ClientPhoneNumber newPhone = notNull(phoneNumber);
        if (hasCoreFields(newName, newEmail, newPhone)) {
            return this;
        }
        return toBuilder()
                .name(newName)
                .email(newEmail)
                .phone(newPhone)
                .build();
    }

    @Override
    public Person changeName(@Nullable final ClientName name) {
        final ClientName newName = notNull(name);
        if (hasName(newName)) {
            return this;
        }
        return toBuilder().name(newName).build();
    }

    @Override
    public Person changeEmail(@Nullable final ClientEmail email) {
        final ClientEmail newEmail = notNull(email);
        if (hasEmail(newEmail)) {
            return this;
        }
        return toBuilder().email(newEmail).build();
    }

    @Override
    public Person changePhone(@Nullable final ClientPhoneNumber phoneNumber) {
        final ClientPhoneNumber newPhone = notNull(phoneNumber);
        if (hasPhone(newPhone)) {
            return this;
        }
        return toBuilder().phone(newPhone).build();
    }

    private static PersonBuilder builder() {
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
    @NullUnmarked
    private static final class PersonBuilder {
        private UUID id;
        private ClientName name;
        private ClientEmail email;
        private ClientPhoneNumber phone;
        private PersonBirthDate birthDate;

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

