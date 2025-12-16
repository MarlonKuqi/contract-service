package com.mk.contractservice.domain.client.aggregate;

import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;

import java.util.UUID;


@Getter
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public abstract sealed class Client permits Person, Company {

    public static final String NULL_NAME_MSG = "ClientName must not be null";
    public static final String NULL_EMAIL_MSG = "Email must not be null";
    public static final String NULL_PHONE_MSG = "ClientPhoneNumber must not be null";

    @Nullable
    UUID id;
    ClientName name;
    ClientEmail email;
    ClientPhoneNumber phone;

    protected Client(@Nullable final UUID id, @Nullable final ClientName name, @Nullable final ClientEmail email, @Nullable final ClientPhoneNumber phone) {
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

    public abstract Client updatePartial(
            @Nullable final ClientName name,
            @Nullable final ClientEmail clientEmail,
            @Nullable final ClientPhoneNumber phone);
}
