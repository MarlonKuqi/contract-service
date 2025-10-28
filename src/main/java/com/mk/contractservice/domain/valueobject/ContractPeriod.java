package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Embeddable
public final class ContractPeriod {

    @Column(name = "start_date", nullable = false)
    private final OffsetDateTime startDate;

    @Column(name = "end_date")
    private final OffsetDateTime endDate;

    protected ContractPeriod() {
        this.startDate = null;
        this.endDate = null;
    }

    private ContractPeriod(final OffsetDateTime startDate, final OffsetDateTime endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static ContractPeriod of(final OffsetDateTime startDate, final OffsetDateTime endDate) {
        final OffsetDateTime normalizedStart = (startDate != null) ? startDate : OffsetDateTime.now();
        validate(normalizedStart, endDate);
        return new ContractPeriod(normalizedStart, endDate);
    }

    private static void validate(final OffsetDateTime startDate, final OffsetDateTime endDate) {
        if (endDate != null && !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException(
                    "Contract end date must be after start date. " +
                    "Start: " + startDate + ", End: " + endDate
            );
        }
    }

    public boolean isActiveAt(final OffsetDateTime referenceDate) {
        return endDate == null || referenceDate.isBefore(endDate);
    }

    public boolean isActive() {
        return isActiveAt(OffsetDateTime.now());
    }

    @JsonProperty("startDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    public OffsetDateTime startDate() {
        return startDate;
    }

    @JsonProperty("endDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    public OffsetDateTime endDate() {
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

