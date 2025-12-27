package com.mk.contractservice.domain.client.aggregate;

import com.mk.contractservice.domain.client.exception.InvalidClientException;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.shared.Entity;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;

import java.util.UUID;


@Getter
@EqualsAndHashCode(callSuper = false)
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
@AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract sealed class Client extends Entity permits Person, Company {

    @Nullable
    UUID id;
    ClientName name;
    ClientEmail email;
    ClientPhoneNumber phone;

    protected static ClientName guardName(@Nullable final ClientName name) {
        return guardNotNull(name, InvalidClientException::forNullName);
    }

    protected static ClientEmail guardEmail(@Nullable final ClientEmail email) {
        return guardNotNull(email, InvalidClientException::forNullEmail);
    }

    protected static ClientPhoneNumber guardPhone(@Nullable final ClientPhoneNumber phone) {
        return guardNotNull(phone, InvalidClientException::forNullPhone);
    }

    public abstract Client withCommonFields(
            final ClientName name,
            final ClientEmail clientEmail,
            final ClientPhoneNumber phone);

    public abstract Client withName(final ClientName name);

    public abstract Client withEmail(final ClientEmail email);

    public abstract Client withPhone(final ClientPhoneNumber phone);
}
