package com.mk.contractservice.domain.shared;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Assert {

    public static <T> T notNull(final T something) {
        return Optional.ofNullable(something)
                .orElseThrow(() -> new InvalidDomainObjectError("Null found for a non-null model attribute"));
    }
}

