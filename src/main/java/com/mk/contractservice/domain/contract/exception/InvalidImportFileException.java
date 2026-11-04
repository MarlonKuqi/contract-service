package com.mk.contractservice.domain.contract.exception;

public class InvalidImportFileException extends RuntimeException {

    public InvalidImportFileException(final String message) {
        super(message);
    }
}
