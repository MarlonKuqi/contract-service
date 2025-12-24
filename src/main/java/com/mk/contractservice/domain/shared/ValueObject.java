package com.mk.contractservice.domain.shared;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ValueObject {

    private ValueObject() {
        throw new UnsupportedOperationException("Utility class");
    }


    public static <T, V> T guardNotNull(
            final V trustedValue,
            final Function<V, T> factory,
            final Class<?> valueObjectClass
    ) {
        if (trustedValue == null) {
            throw new IllegalStateException(
                    "Database integrity violation: " + valueObjectClass.getSimpleName() + " value is null. " +
                            "This indicates a bug in database mapping, constraints, or migration scripts."
            );
        }
        return factory.apply(trustedValue);
    }

    public static <T, V1, V2> T guardNotNull(
            final V1 trustedValue1,
            final V2 optionalValue1,
            final BiFunction<V1, V2, T> factory,
            final Class<?> valueObjectClass
    ) {
        if (trustedValue1 == null) {
            throw new IllegalStateException(
                    "Database integrity violation: " + valueObjectClass.getSimpleName() + " first value is null. " +
                            "This indicates a bug in database mapping, constraints, or migration scripts."
            );
        }
        return factory.apply(trustedValue1, optionalValue1);
    }

    public static <T, V, N> T validateAndCreate(
            final V rawValue,
            final Function<V, N> normalizer,
            final Consumer<N> validator,
            final Function<N, T> factory
    ) {
        final N normalized = normalizer.apply(rawValue);
        validator.accept(normalized);
        return factory.apply(normalized);
    }


    public static <T, V1, V2, N> T validateAndCreate(
            final V1 rawValue,
            final V2 optionalValue,
            final Function<V1, N> normalizer,
            final BiConsumer<N, V2> validator,
            final BiFunction<N, V2, T> factory
    ) {
        final N normalized = normalizer.apply(rawValue);
        validator.accept(normalized, optionalValue);
        return factory.apply(normalized, optionalValue);
    }
}

