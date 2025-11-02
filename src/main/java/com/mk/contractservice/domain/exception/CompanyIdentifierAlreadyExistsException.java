package com.mk.contractservice.domain.exception;

public class CompanyIdentifierAlreadyExistsException extends IllegalArgumentException {

    private final String companyIdentifier;

    public CompanyIdentifierAlreadyExistsException(final String message, final String companyIdentifier) {
        super(message);
        this.companyIdentifier = companyIdentifier;
    }

    public String getCompanyIdentifier() {
        return companyIdentifier;
    }
}

