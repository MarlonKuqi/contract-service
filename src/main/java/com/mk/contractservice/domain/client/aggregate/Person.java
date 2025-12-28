package com.mk.contractservice.domain.client.aggregate;

import com.mk.contractservice.domain.client.exception.InvalidClientException;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.PersonBirthDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Getter
@EqualsAndHashCode(callSuper = true)
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
        this.birthDate = birthDate;
    }

    private static PersonBirthDate guardPersonFields(@Nullable final PersonBirthDate birthDate) {
        return guardNotNull(birthDate, InvalidClientException::forNullBirthDate);
    }

    public static Person of(
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone,
            @Nullable final PersonBirthDate birthDate
    ) {
        return builder()
                .name(guardName(name))
                .email(guardEmail(email))
                .phone(guardPhone(phone))
                .birthDate(guardPersonFields(birthDate))
                .build();
    }

    public static Person reconstituteFromDatabase(
            @Nullable final UUID id,
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone,
            @Nullable final PersonBirthDate birthDate
    ) {
        final Class<Person> currentClass = Person.class;
        return builder()
                .id(guardNotNull(id, "id", currentClass))
                .name(guardNotNull(name, "name", currentClass))
                .email(guardNotNull(email, "email", currentClass))
                .phone(guardNotNull(phone, "phone", currentClass))
                .birthDate(guardNotNull(birthDate, "birthDate", currentClass))
                .build();
    }

    @Override
    public Person withCommonFields(final ClientName name, final ClientEmail clientEmail, final ClientPhoneNumber phone) {
        return toBuilder()
                .name(guardName(name))
                .email(guardEmail(clientEmail))
                .phone(guardPhone(phone))
                .build();
    }

    @Override
    public Person withName(final ClientName name) {
        return toBuilder().name(guardName(name)).build();
    }

    @Override
    public Person withEmail(final ClientEmail email) {
        return toBuilder().email(guardEmail(email)).build();
    }

    @Override
    public Person withPhone(final ClientPhoneNumber phone) {
        return toBuilder().phone(guardPhone(phone)).build();
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
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE)
    @NullUnmarked
    private static class PersonBuilder {
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

