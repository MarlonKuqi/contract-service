package com.mk.contractservice.web.exception;

public class InvalidPaginationException extends RuntimeException {

    public InvalidPaginationException(String message) {
        super(message);
    }
}
