package com.mk.contractservice.domain.exception;

import lombok.Getter;

@Getter
public class DomainValidationException extends RuntimeException {

    private final String code;

    public DomainValidationException(String message) {
        this(message, null, null);
    }

    public DomainValidationException(String message, String code) {
        this(message, code, null);
    }

    public DomainValidationException(String message, String code, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

}

