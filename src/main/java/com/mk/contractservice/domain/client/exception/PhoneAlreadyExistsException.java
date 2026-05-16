package com.mk.contractservice.domain.client.exception;

public class PhoneAlreadyExistsException extends RuntimeException {

    private final String phoneNumber;

    public PhoneAlreadyExistsException(final String message, final String phoneNumber) {
        super(message);
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}

