package com.mk.contractservice.features.shared;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PageBounds {

    public static void requireValidPage(final int page) {
        if (page < 0) {
            throw new InvalidPageRequestException("Page number must not be less than zero, but was: " + page);
        }
    }

    public static void requireValidSize(final int size, final int maxSize) {
        if (size < 1) {
            throw new InvalidPageRequestException("Page size must not be less than one, but was: " + size);
        }
        if (size > maxSize) {
            throw new InvalidPageRequestException("Page size must not exceed " + maxSize + ", but was: " + size);
        }
    }
}
