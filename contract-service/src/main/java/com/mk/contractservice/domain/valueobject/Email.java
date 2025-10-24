package com.mk.contractservice.domain.valueobject;

import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class Email {

    @jakarta.validation.constraints.Email(message = "Invalid email format")
    private String value;

    private Email(String v) {
        this.value = v.trim().toLowerCase();
    }

    public static Email of(String v) {
        return new Email(v);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o instanceof final Email e) return Objects.equals(value, e.value);
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
