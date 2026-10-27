package com.mk.contractservice.domain.client.exception;

import lombok.Getter;

@Getter
public class CompanyIdentifierAlreadyExistsException extends IllegalArgumentException {

    private final String companyIdentifier;

    public CompanyIdentifierAlreadyExistsException(final String message, final String companyIdentifier) {
        super(message);
        this.companyIdentifier = companyIdentifier;
    }

}

