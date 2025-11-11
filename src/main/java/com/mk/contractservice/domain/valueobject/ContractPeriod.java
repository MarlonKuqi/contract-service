package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mk.contractservice.domain.exception.InvalidContractPeriodException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.BiPredicate;

public final class ContractPeriod {

    public static final BiPredicate<LocalDateTime, LocalDateTime> END_IS_AFTER_START =
        (start, end) -> end == null || (start != null && end.isAfter(start));

    private final LocalDateTime startDate;

    private final LocalDateTime endDate;


    private ContractPeriod(final LocalDateTime startDate, final LocalDateTime endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static ContractPeriod of(final LocalDateTime startDate, final LocalDateTime endDate) {
        final LocalDateTime normalizedStart = (startDate != null) ? startDate : LocalDateTime.now();
        validate(normalizedStart, endDate);
        return new ContractPeriod(normalizedStart, endDate);
    }

    private static void validate(final LocalDateTime startDate, final LocalDateTime endDate) {
        if (!END_IS_AFTER_START.test(startDate, endDate)) {
            throw new InvalidContractPeriodException(
                    "Contract end date must be after start date. " +
                            "Start: " + startDate + ", End: " + endDate
            );
        }
    }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return endDate == null || now.isBefore(endDate);
    }

    @JsonProperty("startDate")
    public LocalDateTime startDate() {
        return startDate;
    }

    @JsonProperty("endDate")
    public LocalDateTime endDate() {
        return endDate;
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof ContractPeriod other && Objects.equals(startDate, other.startDate)
                && Objects.equals(endDate, other.endDate));
    }

    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate);
    }

    @Override
    public String toString() {
        return "ContractPeriod{startDate=" + startDate + ", endDate=" + endDate + '}';
    }
}

