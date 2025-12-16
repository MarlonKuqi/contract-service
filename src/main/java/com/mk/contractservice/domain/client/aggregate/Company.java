package com.mk.contractservice.domain.client.aggregate;

import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Getter
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public final class Company extends Client {

    public static final String COMPANY_IDENTIFIER_NULL_MESSAGE = "Company identifier must not be null";

    CompanyIdentifier companyIdentifier;

    private Company(
            @Nullable final UUID id,
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone,
            @Nullable final CompanyIdentifier companyIdentifier
    ) {
        super(id, name, email, phone);

        if (companyIdentifier == null) {
            throw new IllegalArgumentException(COMPANY_IDENTIFIER_NULL_MESSAGE);
        }

        this.companyIdentifier = companyIdentifier;
    }

    public static Company of(
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone,
            @Nullable final CompanyIdentifier companyIdentifier
    ) {
        return builder()
                .name(name)
                .email(email)
                .phone(phone)
                .companyIdentifier(companyIdentifier)
                .build();
    }

    public static Company reconstitute(
            @Nullable final UUID id,
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone,
            @Nullable final CompanyIdentifier companyIdentifier
    ) {
        return builder()
                .id(id)
                .name(name)
                .email(email)
                .phone(phone)
                .companyIdentifier(companyIdentifier)
                .build();
    }

    public Company withCommonFields(final ClientName name, final ClientEmail email, final ClientPhoneNumber phone) {
        return toBuilder()
                .name(name)
                .email(email)
                .phone(phone)
                .build();
    }

    @Override
    public Company updatePartial(
            @Nullable final ClientName name,
            @Nullable final ClientEmail email,
            @Nullable final ClientPhoneNumber phone
    ) {
        return toBuilder()
                .name(name != null ? name : this.getName())
                .email(email != null ? email : this.getEmail())
                .phone(phone != null ? phone : this.getPhone())
                .build();
    }

    public static CompanyBuilder builder() {
        return new CompanyBuilder();
    }

    public CompanyBuilder toBuilder() {
        return builder()
                .id(this.getId())
                .name(this.getName())
                .email(this.getEmail())
                .phone(this.getPhone())
                .companyIdentifier(this.companyIdentifier);
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

