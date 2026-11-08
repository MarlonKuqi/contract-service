package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.contract.exception.InvalidContractPeriodException;
import com.mk.contractservice.domain.shared.ValueObjectUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.function.Predicate;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ContractPeriod {

    LocalDateTime startDate;

    @Nullable
    LocalDateTime endDate;

    private static final int MIN_YEAR = 1;
    private static final int MAX_YEAR = 9999;


    private static final Predicate<LocalDateTime> IS_YEAR_OUT_OF_RANGE = date -> date.getYear() < MIN_YEAR || date
            .getYear() > MAX_YEAR;

    public boolean isEffectiveAt(final LocalDateTime referenceDate) {
        return endDate == null || referenceDate.isBefore(endDate);
    }

    public static ContractPeriod of(
            @Nullable final LocalDateTime startDate,
            @Nullable final LocalDateTime endDate,
            final LocalDateTime now
    ) {
        notNull(now);
        return ValueObjectUtils
                .<ContractPeriod, @Nullable LocalDateTime, @Nullable LocalDateTime, LocalDateTime>validateAndCreate(
                        startDate,
                        endDate,
                        start -> start == null ? now : start,
                        ContractPeriod::validate,
                        ContractPeriod::new
                );
    }

    public static ContractPeriod reconstituteFromDatabase(final LocalDateTime startDate,
            @Nullable final LocalDateTime endDate) {
        return new ContractPeriod(notNull(startDate), endDate);
    }

    private static void validate(final LocalDateTime normalizedStart, @Nullable final LocalDateTime endDate) {
        if (IS_YEAR_OUT_OF_RANGE.test(normalizedStart) || (endDate != null && IS_YEAR_OUT_OF_RANGE.test(endDate))) {
            throw new InvalidContractPeriodException(
                    "Contract dates must be between years " + MIN_YEAR + " and " + MAX_YEAR + ". " + "Start: " + normalizedStart + ", End: " + endDate
            );
        }
        if (endDate != null && !endDate.isAfter(normalizedStart)) {
            throw new InvalidContractPeriodException(
                    "Contract end date must be after start date. " + "Start: " + normalizedStart + ", End: " + endDate
            );
        }
    }
}
