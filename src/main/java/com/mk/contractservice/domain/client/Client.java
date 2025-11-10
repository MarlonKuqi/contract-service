package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public abstract sealed class Client permits Person, Company {

    public static final String NULL_NAME_MSG = "ClientName must not be null";
    public static final String NULL_EMAIL_MSG = "Email must not be null";
    public static final String NULL_PHONE_MSG = "PhoneNumber must not be null";

    private UUID id;

    private ClientName name;

    private Email email;

    private PhoneNumber phone;

    protected Client(final UUID id, final ClientName name, final Email email, final PhoneNumber phone) {
        if (name == null) {
            throw new IllegalArgumentException(NULL_NAME_MSG);
        }
        if (email == null) {
            throw new IllegalArgumentException(NULL_EMAIL_MSG);
        }
        if (phone == null) {
            throw new IllegalArgumentException(NULL_PHONE_MSG);
        }
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public void updateCommonFields(final ClientName name, final Email email, final PhoneNumber phone) {
        if (name == null || email == null || phone == null) {
            final String nullFields = Stream.of(
                    name == null ? "name" : null,
                    email == null ? "email" : null,
                    phone == null ? "phone" : null
            ).filter(Objects::nonNull).collect(Collectors.joining(", "));

            throw new IllegalArgumentException(
                    "Cannot update client: the following required fields are null: " + nullFields
            );
        }

        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public void changeName(final ClientName name) {
        if (name == null) {
            throw new IllegalArgumentException(NULL_NAME_MSG);
        }
        this.name = name;
    }

    public void changeEmail(final Email email) {
        if (email == null) {
            throw new IllegalArgumentException(NULL_EMAIL_MSG);
        }
        this.email = email;
    }

    public void changePhone(final PhoneNumber phone) {
        if (phone == null) {
            throw new IllegalArgumentException(NULL_PHONE_MSG);
        }
        this.phone = phone;
    }
}
