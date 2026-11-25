package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.contract.exception.InvalidContractFranchiseException;
import com.mk.contractservice.domain.shared.ValueObjectUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LamalFranchise {

    static final SortedSet<Integer> CHILD_AMOUNTS = sorted(0, 100, 200, 300, 400, 500, 600);

    static final SortedSet<Integer> ADULT_AMOUNTS = sorted(300, 500, 1000, 1500, 2000, 2500);

    public static final int ADULT_FROM_YEAR_OF_AGE = 19;

    Integer value;

    public static LamalFranchise of(@Nullable final Integer rawValue) {
        return ValueObjectUtils.validateAndCreate(
                rawValue,
                LamalFranchise::normalize,
                LamalFranchise::validate,
                LamalFranchise::new
        );
    }

    public static LamalFranchise reconstituteFromDatabase(final Integer trustedValue) {
        return new LamalFranchise(notNull(trustedValue));
    }

    public void ensureSuits(final LocalDate insuredBirthDate, final ContractPeriod period) {
        final LocalDateTime endOfChildhood = LocalDate.of(insuredBirthDate.getYear() + ADULT_FROM_YEAR_OF_AGE, 1, 1)
                .atStartOfDay();
        if (!period.startDate().isBefore(endOfChildhood)) {
            ensureAllowed(ADULT_AMOUNTS, "an adult");
            return;
        }
        ensureAllowed(CHILD_AMOUNTS, "a child");
        final LocalDateTime endDate = period.endDate();
        if (endDate == null || endDate.isAfter(endOfChildhood)) {
            throw new InvalidContractFranchiseException(
                    "A LAMAL contract taken out for a child must end at the latest on " + endOfChildhood
                            .toLocalDate() + ", January 1st of the year the insured person turns " + ADULT_FROM_YEAR_OF_AGE + ". End: " + endDate
            );
        }
    }

    private void ensureAllowed(final SortedSet<Integer> allowedAmounts, final String insured) {
        if (!allowedAmounts.contains(value)) {
            throw new InvalidContractFranchiseException(
                    "A LAMAL franchise of " + value + " CHF is not allowed for " + insured + ". Allowed: " + allowedAmounts
            );
        }
    }

    private static Integer normalize(@Nullable final Integer rawValue) {
        if (rawValue == null) {
            throw new InvalidContractFranchiseException("A LAMAL contract requires a franchise");
        }
        return rawValue;
    }

    private static void validate(final Integer normalized) {
        if (!CHILD_AMOUNTS.contains(normalized) && !ADULT_AMOUNTS.contains(normalized)) {
            throw new InvalidContractFranchiseException(
                    "A LAMAL franchise must be one of " + CHILD_AMOUNTS + " CHF for a child or " + ADULT_AMOUNTS + " CHF for an adult: " + normalized
            );
        }
    }

    private static SortedSet<Integer> sorted(final Integer... amounts) {
        return Collections.unmodifiableSortedSet(new TreeSet<>(List.of(amounts)));
    }
}
