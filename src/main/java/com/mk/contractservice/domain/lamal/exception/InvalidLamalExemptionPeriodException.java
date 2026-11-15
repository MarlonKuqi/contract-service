package com.mk.contractservice.domain.lamal.exception;

import com.mk.contractservice.domain.shared.exception.DomainValidationException;

public class InvalidLamalExemptionPeriodException extends DomainValidationException {

    private static final String CODE = "invalidLamalExemptionPeriod";

    public InvalidLamalExemptionPeriodException(final String message) {
        super(message, CODE);
    }
}
