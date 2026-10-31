package com.mk.contractservice.controllers.shared;

public class UnmatchableIfMatchException extends RuntimeException {

    public UnmatchableIfMatchException(final String message) {
        super(message);
    }
}
