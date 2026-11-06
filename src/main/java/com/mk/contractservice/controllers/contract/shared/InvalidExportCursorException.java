package com.mk.contractservice.controllers.contract.shared;

public class InvalidExportCursorException extends RuntimeException {

    public InvalidExportCursorException() {
        super("The export cursor is invalid. Use the nextCursor value returned by the previous page, unchanged.");
    }
}
