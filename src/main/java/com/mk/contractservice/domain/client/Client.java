package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;


@Getter
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public abstract sealed class Client permits Person, Company {

    public static String NULL_NAME_MSG = "ClientName must not be null";
    public static String NULL_EMAIL_MSG = "Email must not be null";
    public static String NULL_PHONE_MSG = "PhoneNumber must not be null";

    UUID id;
    ClientName name;
    Email email;
    PhoneNumber phone;

    protected Client(final UUID id, final ClientName name, final Email email, final PhoneNumber phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    protected void checkInvariants() {
        if (name == null) {
            throw new IllegalArgumentException(NULL_NAME_MSG);
        }
        if (email == null) {
            throw new IllegalArgumentException(NULL_EMAIL_MSG);
        }
        if (phone == null) {
            throw new IllegalArgumentException(NULL_PHONE_MSG);
        }
    }

    public abstract Client updatePartial(final ClientName name, final Email email, final PhoneNumber phone);
}
