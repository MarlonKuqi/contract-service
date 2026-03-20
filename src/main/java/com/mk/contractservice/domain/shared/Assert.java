package com.mk.contractservice.domain.shared;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Assert {

    public static <T> T notNull(final T something) {
        if (something == null) {
            throw new InvalidDomainObjectError("Null found for a non-null model attribute");
        }
        return something;
    }
}

