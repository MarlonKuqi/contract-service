package com.mk.contractservice.controllers.shared;

public class PreconditionRequiredException extends RuntimeException {

    public PreconditionRequiredException(final String message) {
        super(message);
    }
}
