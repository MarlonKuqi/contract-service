package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.contract.exception.InvalidContractPeriodException;
import com.mk.contractservice.domain.shared.ValueObjectUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.function.BiPredicate;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ContractPeriod {

    LocalDateTime startDate;

    @Nullable
    LocalDateTime endDate;

    private static final BiPredicate<LocalDateTime, @Nullable LocalDateTime> START_IS_AFTER_END =
            (start, end) -> end != null && !end.isAfter(start);

    public boolean isEffectiveAt(final LocalDateTime referenceDate) {
        return endDate == null || referenceDate.isBefore(endDate);
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
        return new ContractPeriod(notNull(startDate), endDate);
    }

    private static LocalDateTime normalize(final LocalDateTime startDate) {
        return startDate == null ? LocalDateTime.now() : startDate;
    }

    private static void validate(final LocalDateTime normalizedStart, @Nullable final LocalDateTime endDate) {
        if (START_IS_AFTER_END.test(normalizedStart, endDate)) {
            throw new InvalidContractPeriodException(
                    "Contract end date must be after start date. " +
                            "Start: " + normalizedStart + ", End: " + endDate
            );
        }
    }
}
