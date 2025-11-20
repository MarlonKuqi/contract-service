package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public final class Company extends Client {

    static String COMPANY_IDENTIFIER_NULL_MESSAGE = "Company identifier must not be null";

    CompanyIdentifier companyIdentifier;

    private Company(final UUID id, final ClientName name, final Email email, final PhoneNumber phone, final CompanyIdentifier companyIdentifier) {
        super(id, name, email, phone);
        this.companyIdentifier = companyIdentifier;
        checkInvariants();
    }

    @Override
    protected void checkInvariants() {
        super.checkInvariants();
        if (companyIdentifier == null) {
            throw new IllegalArgumentException(COMPANY_IDENTIFIER_NULL_MESSAGE);
        }
    }

    public static Company of(final ClientName name, final Email email, final PhoneNumber phone, final CompanyIdentifier companyIdentifier) {
        return builder()
                .name(name)
                .email(email)
                .phone(phone)
                .companyIdentifier(companyIdentifier)
                .build();
    }

    public static Company reconstitute(final UUID id, final ClientName name, final Email email, final PhoneNumber phone, final CompanyIdentifier companyIdentifier) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null when reconstituting a Company");
        }
        return builder()
                .id(id)
                .name(name)
                .email(email)
                .phone(phone)
                .companyIdentifier(companyIdentifier)
                .build();
    }

    public Company withCommonFields(final ClientName name, final Email email, final PhoneNumber phone) {
        return toBuilder()
                .name(name)
                .email(email)
                .phone(phone)
                .build();
    }

    @Override
    public Company updatePartial(final ClientName name, final Email email, final PhoneNumber phone) {
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
    public static class CompanyBuilder {
        UUID id;
        ClientName name;
        Email email;
        PhoneNumber phone;
        CompanyIdentifier companyIdentifier;

        public CompanyBuilder id(final UUID id) {
            this.id = id;
            return this;
        }

        public CompanyBuilder name(final ClientName name) {
            this.name = name;
            return this;
        }

        public CompanyBuilder email(final Email email) {
            this.email = email;
            return this;
        }

        public CompanyBuilder phone(final PhoneNumber phone) {
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

