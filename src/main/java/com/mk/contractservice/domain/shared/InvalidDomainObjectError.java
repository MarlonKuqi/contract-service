package com.mk.contractservice.domain.shared;

public class InvalidDomainObjectError extends Error {

    public InvalidDomainObjectError(final String message) {
        super(message);
    }
}

