package com.mk.contractservice.domain.contract.importjob.exception;

import org.jspecify.annotations.Nullable;

public class IdempotencyKeyReusedException extends RuntimeException {

    public IdempotencyKeyReusedException(@Nullable final String idempotencyKey) {
        super("The idempotency key '" + idempotencyKey + "' was already used for another file");
    }
}
