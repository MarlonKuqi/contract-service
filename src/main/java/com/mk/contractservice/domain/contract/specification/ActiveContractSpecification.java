package com.mk.contractservice.domain.contract.specification;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ActiveContractSpecification {

    public static final String SQL_WHERE_ACTIVE_NOW =
            "(c.endDate IS NULL OR c.endDate > CURRENT_TIMESTAMP)";

    public static final String SQL_WHERE_ACTIVE_AT_DATE =
            "(c.endDate IS NULL OR c.endDate > :referenceDate)";

    public static boolean isSatisfiedBy(LocalDateTime endDate, LocalDateTime referenceDate) {
        return endDate == null || referenceDate.isBefore(endDate);
    }

    public static boolean isSatisfiedBy(LocalDateTime endDate) {
        return isSatisfiedBy(endDate, LocalDateTime.now());
    }

    @Deprecated(since = "2.0.0", forRemoval = true)
    public static boolean isActive(LocalDateTime endDate, LocalDateTime referenceDate) {
        return isSatisfiedBy(endDate, referenceDate);
    }

    @Deprecated(since = "2.0.0", forRemoval = true)
    public static boolean isActive(LocalDateTime endDate) {
        return isSatisfiedBy(endDate);
    }
}



