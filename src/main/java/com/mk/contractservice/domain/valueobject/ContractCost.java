package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mk.contractservice.domain.exception.InvalidContractCostException;

import java.math.BigDecimal;
import java.util.Objects;

public final class ContractCost {

    private final BigDecimal value;


    private ContractCost(final BigDecimal value) {
        this.value = value;
    }

    public static ContractCost of(final BigDecimal rawValue) {
        validate(rawValue);
        return new ContractCost(rawValue);
    }

    private static void validate(final BigDecimal rawValue) {
        if (rawValue == null) {
            throw new InvalidContractCostException("Contract cost amount must not be null");
        }

        if (rawValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidContractCostException("Contract cost amount must be greater than zero: " + rawValue);
        }

        if (rawValue.scale() > 2) {
            throw new InvalidContractCostException("Contract cost amount must have at most 2 decimal places: " + rawValue);
        }
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

