package com.mk.contractservice.application.feature.shared.exception;

public class InvalidPaginationException extends RuntimeException {

    public InvalidPaginationException(String message) {
        super(message);
    }
}

