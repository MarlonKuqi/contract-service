package com.mk.contractservice.domain.shared.exception;

import lombok.Getter;

@Getter
public class VersionMismatchException extends RuntimeException {

    private final long expectedVersion;
    private final long currentVersion;

    public VersionMismatchException(final long expectedVersion, final long currentVersion) {
        super(String.format("Expected version %d but the current version is %d", expectedVersion, currentVersion));
        this.expectedVersion = expectedVersion;
        this.currentVersion = currentVersion;
    }
}
