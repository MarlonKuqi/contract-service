package com.mk.contractservice.domain.client.aggregate;

import com.mk.contractservice.domain.client.exception.InvalidClientException;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;

import java.util.UUID;


@Getter
@EqualsAndHashCode
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
@AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract sealed class Client permits Person, Company {

    @Nullable
    UUID id;
    ClientName name;
    ClientEmail email;
    ClientPhoneNumber phone;

    protected static void validateCommonFields(
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone
    ) {
        if (name == null) {
            throw InvalidClientException.forNullName();
        }
        if (email == null) {
            throw InvalidClientException.forNullEmail();
        }
        if (phone == null) {
            throw InvalidClientException.forNullPhone();
        }
    }

    public abstract Client withCommonFields(
            final ClientName name,
            final ClientEmail clientEmail,
            final ClientPhoneNumber phone);

    public abstract Client withName(final ClientName name);

    public abstract Client withEmail(final ClientEmail email);

    public abstract Client withPhone(final ClientPhoneNumber phone);
}
