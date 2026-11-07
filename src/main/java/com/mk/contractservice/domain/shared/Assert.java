package com.mk.contractservice.domain.shared;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Assert {

    public static <T> T notNull(@Nullable final T something) {
        if (something == null) {
            throw new InvalidDomainObjectError("Null found for a non-null model attribute");
        }
        return something;
    }
}

