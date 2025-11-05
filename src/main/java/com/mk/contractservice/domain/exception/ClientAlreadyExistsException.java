package com.mk.contractservice.domain.exception;

import org.springframework.http.HttpStatus;

public class ClientAlreadyExistsException extends RuntimeException {

    private final String businessKey;
    private final HttpStatus status = HttpStatus.CONFLICT;

    public ClientAlreadyExistsException(String message) {
        super(message);
        this.businessKey = null;
    }

    public ClientAlreadyExistsException(String message, String businessKey) {
        super(message);
        this.businessKey = businessKey;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public HttpStatus getStatus() {
        return status;
    }
}