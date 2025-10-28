package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "company")
@DiscriminatorValue("COMPANY")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class Company extends Client {

    private static final String COMPANY_IDENTIFIER_NULL_MESSAGE = "Company identifier must not be null";

    @Embedded
    @NotNull(message = COMPANY_IDENTIFIER_NULL_MESSAGE)
    @Valid
    private final CompanyIdentifier companyIdentifier;

    public Company(final ClientName name, final Email email, final PhoneNumber phone, final CompanyIdentifier companyIdentifier) {
        super(name, email, phone);
        if (companyIdentifier == null) {
            throw new IllegalArgumentException(COMPANY_IDENTIFIER_NULL_MESSAGE);
        }
        this.companyIdentifier = companyIdentifier;
    }
}

