package com.mk.contractservice.domain.contract.importjob.exception;

public class InvalidIdempotencyKeyException extends RuntimeException {

    public InvalidIdempotencyKeyException(final String message) {
        super(message);
    }
}
