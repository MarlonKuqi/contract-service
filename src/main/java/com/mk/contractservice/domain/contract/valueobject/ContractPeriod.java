package com.mk.contractservice.domain.contract.valueobject;

import com.mk.contractservice.domain.contract.exception.InvalidContractPeriodException;
import com.mk.contractservice.domain.shared.ValueObjectUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.function.BiPredicate;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContractPeriod {

    LocalDateTime startDate;

    @Nullable
    LocalDateTime endDate;

    public static final BiPredicate<@Nullable LocalDateTime, @Nullable LocalDateTime> END_IS_AFTER_START =
            (start, end) -> end == null || (start != null && end.isAfter(start));

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return endDate == null || now.isBefore(endDate);
    }

    public static ContractPeriod of(@Nullable final LocalDateTime startDate, @Nullable final LocalDateTime endDate) {
        return ValueObjectUtils.validateAndCreate(
                startDate,
                endDate,
                ContractPeriod::normalize,
                ContractPeriod::validate,
                ContractPeriod::new
        );
    }

    public static ContractPeriod reconstituteFromDatabase(final LocalDateTime startDate, @Nullable final LocalDateTime endDate) {
        return ValueObjectUtils.guardNotNull(startDate, endDate, ContractPeriod::new, ContractPeriod.class);
    }

    private static LocalDateTime normalize(final LocalDateTime startDate) {
        return startDate == null ? LocalDateTime.now() : startDate;
    }

    private static void validate(final LocalDateTime normalizedStart, @Nullable final LocalDateTime endDate) {
        if (!END_IS_AFTER_START.test(normalizedStart, endDate)) {
            throw new InvalidContractPeriodException(
                    "Contract end date must be after start date. " +
                            "Start: " + normalizedStart + ", End: " + endDate
            );
        }
    }
}

