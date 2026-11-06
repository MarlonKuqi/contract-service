package com.mk.contractservice.domain.contract.importjob.exception;

public class InvalidImportFileException extends RuntimeException {

    public InvalidImportFileException(final String message) {
        super(message);
    }
}
