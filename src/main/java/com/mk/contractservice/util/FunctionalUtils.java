package com.mk.contractservice.util;

import java.util.function.Consumer;


public final class FunctionalUtils {

    private FunctionalUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }


    public static <T> boolean applyIfPresent(final T value, final Consumer<T> action) {
        if (value != null) {
            action.accept(value);
            return true;
        }
        return false;
    }
}
