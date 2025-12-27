package com.mk.contractservice.domain.shared;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public abstract class Entity {

    protected static <T> T guardNotNull(
            @Nullable final T fieldValue,
            final Supplier<? extends RuntimeException> exceptionSupplier
    ) {
        if (fieldValue == null) {
            throw exceptionSupplier.get();
        }
        return fieldValue;
    }

    protected static <T> T guardNotNull(
            @Nullable final T fieldValue,
            final String fieldName,
            final Class<?> entityClass
    ) {
        if (fieldValue == null) {
            throw new IllegalStateException(
                    "Database integrity violation: " + entityClass.getSimpleName() + "." +
                            fieldName + " is null. " +
                            "This indicates a bug in database mapping, constraints, or migration scripts."
            );
        }
        return fieldValue;
    }
}

