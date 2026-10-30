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
public final class Company extends Client {

    private final CompanyIdentifier companyIdentifier;

    private Company(
            @Nullable final UUID id,
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone,
            @Nullable final CompanyIdentifier companyIdentifier
    ) {
        super(id, name, email, phone);
        this.companyIdentifier = notNull(companyIdentifier);
    }

    public static Company of(
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone,
            @Nullable final CompanyIdentifier companyIdentifier
    ) {
        return builder()
                .name(notNull(name))
                .email(notNull(email))
                .phone(notNull(phone))
                .companyIdentifier(notNull(companyIdentifier))
                .build();
    }

    public static Company reconstituteFromDatabase(
            @Nullable final UUID id,
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone,
            @Nullable final CompanyIdentifier companyIdentifier
    ) {
        return builder()
                .id(notNull(id))
                .name(notNull(name))
                .email(notNull(email))
                .phone(notNull(phone))
                .companyIdentifier(notNull(companyIdentifier))
                .build();
    }

    @Override
    public Company changeCoreFields(
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phoneNumber
    ) {
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
    public Company changeName(@Nullable final ClientName name) {
        final ClientName newName = notNull(name);
        if (hasName(newName)) {
            return this;
        }
        return toBuilder().name(newName).build();
    }

    @Override
    public Company changeEmail(@Nullable final ClientEmail email) {
        final ClientEmail newEmail = notNull(email);
        if (hasEmail(newEmail)) {
            return this;
        }
        return toBuilder().email(newEmail).build();
    }

    @Override
    public Company changePhone(@Nullable ClientPhoneNumber phoneNumber) {
        final ClientPhoneNumber newPhone = notNull(phoneNumber);
        if (hasPhone(newPhone)) {
            return this;
        }
        return toBuilder().phone(newPhone).build();
    }

    private static CompanyBuilder builder() {
        return new CompanyBuilder();
    }

    private CompanyBuilder toBuilder() {
        return builder()
                .id(getId())
                .name(getName())
                .email(getEmail())
                .phone(getPhone())
                .companyIdentifier(getCompanyIdentifier());
    }

    @NoArgsConstructor
    @NullUnmarked
    private static final class CompanyBuilder {
        private UUID id;
        private ClientName name;
        private ClientEmail email;
        private ClientPhoneNumber phone;
        private CompanyIdentifier companyIdentifier;

        public CompanyBuilder id(final UUID id) {
            this.id = id;
            return this;
        }

        public CompanyBuilder name(final ClientName name) {
            this.name = name;
            return this;
        }

        public CompanyBuilder email(final ClientEmail email) {
            this.email = email;
            return this;
        }

        public CompanyBuilder phone(final ClientPhoneNumber phone) {
            this.phone = phone;
            return this;
        }

        public CompanyBuilder companyIdentifier(final CompanyIdentifier companyIdentifier) {
            this.companyIdentifier = companyIdentifier;
            return this;
        }

        public Company build() {
            return new Company(id, name, email, phone, companyIdentifier);
        }
    }
}

