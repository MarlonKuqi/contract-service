package com.mk.contractservice.domain.shared;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ValueObjectUtils {

    private ValueObjectUtils() {
        throw new UnsupportedOperationException("Utility class");
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

