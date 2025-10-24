package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class PhoneNumber {

    @Pattern(regexp = "\\+?[0-9 .()/-]{7,20}")
    private String value;

    private PhoneNumber(final String v) {
        this.value = v.trim();
    }

    public static PhoneNumber of(final String v) {
        return new PhoneNumber(v);
    }

    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o instanceof final PhoneNumber other) return Objects.equals(value, other.value);
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

