package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import lombok.Getter;

import java.util.UUID;

@Getter
public final class Company extends Client {

    private static final String COMPANY_IDENTIFIER_NULL_MESSAGE = "Company identifier must not be null";

    private final CompanyIdentifier companyIdentifier;

    private Company(final UUID id, final ClientName name, final Email email, final PhoneNumber phone, final CompanyIdentifier companyIdentifier) {
        super(id, name, email, phone);
        if (companyIdentifier == null) {
            throw new IllegalArgumentException(COMPANY_IDENTIFIER_NULL_MESSAGE);
        }
        this.companyIdentifier = companyIdentifier;
    }


    public static CompanyBuilder builder() {
        return new CompanyBuilder();
    }

    public static class CompanyBuilder {
        private UUID id;
        private ClientName name;
        private Email email;
        private PhoneNumber phone;
        private CompanyIdentifier companyIdentifier;

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
