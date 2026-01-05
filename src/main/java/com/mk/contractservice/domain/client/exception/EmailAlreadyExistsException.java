package com.mk.contractservice.domain.client.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    private final String email;

    public EmailAlreadyExistsException(final String message, final String email) {
        super(message);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}

