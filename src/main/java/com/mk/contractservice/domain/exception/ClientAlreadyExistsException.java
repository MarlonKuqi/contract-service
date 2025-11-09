package com.mk.contractservice.domain.exception;

public class ClientAlreadyExistsException extends RuntimeException {

    private final String businessKey;

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
}