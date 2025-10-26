package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object representing a contract cost amount.
 * Immutable and self-validating.
 *
 * <p>Use {@link #of(BigDecimal)} factory method to create instances.
 * Direct construction is prevented to ensure validation.</p>
 */
@Embeddable
public final class ContractCost {

    @Column(name = "cost_amount", nullable = false, precision = 12, scale = 2)
    private final BigDecimal value;

    /**
     * JPA no-args constructor.
     * Protected to prevent direct instantiation outside JPA context.
     */
    protected ContractCost() {
        this.value = null;
    }

    /**
     * Private constructor to force use of {@link #of(BigDecimal)} factory method.
     *
     * @param value the validated contract cost amount
     */
    private ContractCost(final BigDecimal value) {
        this.value = value;
    }

    /**
     * Factory method to create a validated ContractCost.
     *
     * <p>Performs validation:
     * <ul>
     *   <li>Null check</li>
     *   <li>Non-negative check (>= 0)</li>
     *   <li>Scale validation (max 2 decimal places)</li>
     * </ul>
     *
     * @param rawValue the contract cost amount
     * @return a validated ContractCost instance
     * @throws IllegalArgumentException if validation fails
     */
    public static ContractCost of(final BigDecimal rawValue) {
        if (rawValue == null) {
            throw new IllegalArgumentException("Contract cost amount must not be null");
        }

        if (rawValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Contract cost amount must not be negative: " + rawValue);
        }

        if (rawValue.scale() > 2) {
            throw new IllegalArgumentException("Contract cost amount must have at most 2 decimal places: " + rawValue);
        }

        return new ContractCost(rawValue);
    }

    @JsonValue
    public BigDecimal value() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof ContractCost other && Objects.equals(value, other.value));
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value != null ? value.toString() : "0.00";
    }
}

