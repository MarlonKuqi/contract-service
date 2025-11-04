package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mk.contractservice.domain.exception.InvalidContractPeriodException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;
import java.util.Objects;

@Embeddable
public final class ContractPeriod {

    @Column(name = "start_date", nullable = false)
    private final LocalDateTime startDate;

    @Column(name = "end_date")
    private final LocalDateTime endDate;

    protected ContractPeriod() {
        this.startDate = null;
        this.endDate = null;
    }

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
        if (endDate != null && !endDate.isAfter(startDate)) {
            throw new InvalidContractPeriodException(
                    "Contract end date must be after start date. " +
                            "Start: " + startDate + ", End: " + endDate
            );
        }
    }

    public boolean isActiveAt(final LocalDateTime referenceDate) {
        return endDate == null || referenceDate.isBefore(endDate);
    }

    public boolean isActive() {
        return isActiveAt(LocalDateTime.now());
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

