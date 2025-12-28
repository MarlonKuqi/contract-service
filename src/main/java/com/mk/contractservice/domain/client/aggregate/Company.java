package com.mk.contractservice.domain.client.aggregate;

import com.mk.contractservice.domain.client.exception.InvalidClientException;
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
        this.companyIdentifier = companyIdentifier;
    }

    private static CompanyIdentifier guardCompanyFields(@Nullable final CompanyIdentifier companyIdentifier) {
        return guardNotNull(companyIdentifier, InvalidClientException::forNullCompanyIdentifier);
    }

    public static Company of(
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone,
            @Nullable final CompanyIdentifier companyIdentifier
    ) {
        return builder()
                .name(guardName(name))
                .email(guardEmail(email))
                .phone(guardPhone(phone))
                .companyIdentifier(guardCompanyFields(companyIdentifier))
                .build();
    }

    public static Company reconstituteFromDatabase(
            @Nullable final UUID id,
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone,
            @Nullable final CompanyIdentifier companyIdentifier
    ) {
        final Class<Company> currentClass = Company.class;
        return builder()
                .id(guardNotNull(id, "id", currentClass))
                .name(guardNotNull(name, "name", currentClass))
                .email(guardNotNull(email, "email", currentClass))
                .phone(guardNotNull(phone, "phone", currentClass))
                .companyIdentifier(guardNotNull(companyIdentifier, "companyIdentifier", currentClass))
                .build();
    }

    @Override
    public Company withCommonFields(final ClientName name, final ClientEmail email, final ClientPhoneNumber phone) {
        return toBuilder()
                .name(guardName(name))
                .email(guardEmail(email))
                .phone(guardPhone(phone))
                .build();
    }

    @Override
    public Company withName(final ClientName name) {
        return toBuilder().name(guardName(name)).build();
    }

    @Override
    public Company withEmail(final ClientEmail email) {
        return toBuilder().email(guardEmail(email)).build();
    }

    @Override
    public Company withPhone(final ClientPhoneNumber phone) {
        return toBuilder().phone(guardPhone(phone)).build();
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
    @FieldDefaults(level = lombok.AccessLevel.PRIVATE)
    @NullUnmarked
    private static class CompanyBuilder {
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

