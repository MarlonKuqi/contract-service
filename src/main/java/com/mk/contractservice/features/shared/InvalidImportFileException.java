package com.mk.contractservice.features.shared;

public class InvalidImportFileException extends RuntimeException {

    public InvalidImportFileException(final String message) {
        super(message);
    }
}
