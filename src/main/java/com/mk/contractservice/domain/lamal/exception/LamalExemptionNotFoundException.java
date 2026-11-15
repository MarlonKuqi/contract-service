package com.mk.contractservice.domain.lamal.exception;

import java.util.Objects;
import java.util.UUID;

public class LamalExemptionNotFoundException extends RuntimeException {

    public LamalExemptionNotFoundException(final UUID clientId) {
        super("No LAMal exemption recorded for client " + clientId);
    }

    @Override
    public String getMessage() {
        return Objects.requireNonNull(super.getMessage());
    }
}
