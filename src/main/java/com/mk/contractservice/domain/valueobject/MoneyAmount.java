package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class MoneyAmount {

    @PositiveOrZero
    @Digits(integer = 12, fraction = 2)
    private BigDecimal value;

    private MoneyAmount(final BigDecimal v) {
        this.value = v;
    }

    public static MoneyAmount of(final BigDecimal v) {
        return new MoneyAmount(v);
    }

    @JsonValue
    public BigDecimal value() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o instanceof final MoneyAmount other) return Objects.equals(value, other.value);
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value != null ? value.toString() : "";
    }
}
