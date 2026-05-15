package com.mk.contractservice.domain.client;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Getter
@EqualsAndHashCode
public abstract sealed class Client permits Person, Company {

    @Nullable
    private final UUID id;
    private final ClientName name;
    private final ClientEmail email;
    private final ClientPhoneNumber phone;

    protected Client(
            @Nullable final UUID id,
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone
    ) {
        this.id = id;
        this.name = notNull(name);
        this.email = notNull(email);
        this.phone = notNull(phone);
    }

    public abstract Client changeCoreFields(
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phoneNumber);

    public abstract Client changeName(@Nullable final ClientName name);

    public abstract Client changeEmail(@Nullable final ClientEmail email);

    public abstract Client changePhone(@Nullable final ClientPhoneNumber phoneNumber);
}
