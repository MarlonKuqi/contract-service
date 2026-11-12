package com.mk.contractservice.features.shared;

public class InvalidPageRequestException extends RuntimeException {

    public InvalidPageRequestException(final String message) {
        super(message);
    }
}
