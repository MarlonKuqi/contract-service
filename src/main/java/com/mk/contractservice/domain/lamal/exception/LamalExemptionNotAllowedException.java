package com.mk.contractservice.domain.lamal.exception;

import com.mk.contractservice.domain.shared.exception.DomainValidationException;

public class LamalExemptionNotAllowedException extends DomainValidationException {

    private static final String CODE = "lamalExemptionNotAllowed";

    public LamalExemptionNotAllowedException() {
        super("Only a person living in a bordering country can be exempted from LAMal", CODE);
    }
}
