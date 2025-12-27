package com.mk.contractservice.domain.contract.valueobject;

import com.mk.contractservice.domain.contract.exception.InvalidContractCostException;
import com.mk.contractservice.domain.shared.ValueObjectUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.function.Predicate;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContractCost {

    BigDecimal value;

    public static final Predicate<BigDecimal> IS_ZERO_OR_NEGATIVE =
            amount -> amount.compareTo(BigDecimal.ZERO) <= 0;

    public static final Predicate<BigDecimal> HAS_INVALID_SCALE = amount -> amount.scale() > 2;

    public static ContractCost reconstituteFromDatabase(final BigDecimal trustedValue) {
        return ValueObjectUtils.guardNotNull(trustedValue, ContractCost::new, ContractCost.class);
    }

    public static ContractCost of(@Nullable final BigDecimal rawValue) {
        return ValueObjectUtils.validateAndCreate(
                rawValue,
                ContractCost::normalize,
                ContractCost::validate,
                ContractCost::new
        );
    }

    private static BigDecimal normalize(@Nullable final BigDecimal rawValue) {
        if (rawValue == null) {
            throw new InvalidContractCostException("Contract cost amount must not be null");
        }
        return rawValue;
    }

    private static void validate(@Nullable final BigDecimal rawValue) {
        if (IS_ZERO_OR_NEGATIVE.test(rawValue)) {
            throw new InvalidContractCostException("Contract cost amount must be greater than zero: " + rawValue);
        }
        if (HAS_INVALID_SCALE.test(rawValue)) {
            throw new InvalidContractCostException("Contract cost amount must have at most 2 decimal places: " + rawValue);
        }
    }
}

