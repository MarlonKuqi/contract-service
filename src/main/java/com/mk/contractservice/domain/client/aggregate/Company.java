package com.mk.contractservice.domain.client.aggregate;

import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
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
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public final class Company extends Client {

    CompanyIdentifier companyIdentifier;

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
        return toBuilder()
                .name(notNull(name))
                .email(notNull(email))
                .phone(notNull(phoneNumber))
                .build();
    }

    @Override
    public Company changeName(@Nullable final ClientName name) {
        return toBuilder().name(notNull(name)).build();
    }

    @Override
    public Company changeEmail(@Nullable final ClientEmail email) {
        return toBuilder().email(notNull(email)).build();
    }

    @Override
    public Company changePhone(@Nullable ClientPhoneNumber phoneNumber) {
        return toBuilder().phone(notNull(phoneNumber)).build();
    }

    public static CompanyBuilder builder() {
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
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE)
    @NullUnmarked
    public static class CompanyBuilder {
        UUID id;
        ClientName name;
        ClientEmail email;
        ClientPhoneNumber phone;
        CompanyIdentifier companyIdentifier;

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

