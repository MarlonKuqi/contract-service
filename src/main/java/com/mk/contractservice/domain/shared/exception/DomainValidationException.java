package com.mk.contractservice.domain.shared.exception;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Getter
public class DomainValidationException extends RuntimeException {

    private final @Nullable String code;

    public DomainValidationException(final String message) {
        this(message, null, null);
    }

    public DomainValidationException(final String message, @Nullable final String code) {
        this(message, code, null);
    }

    public DomainValidationException(final String message, @Nullable final String code, @Nullable final Throwable cause) {
        super(Objects.requireNonNull(message), cause);
        this.code = code;
    }

    @Override
    public String getMessage() {
        return Objects.requireNonNull(super.getMessage());
    }
}
