package com.mk.contractservice.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Value Object representing a contract period with start and end dates.
 * Immutable and self-validating.
 *
 * <p>Business rules:
 * <ul>
 *   <li>startDate defaults to now if not provided</li>
 *   <li>endDate can be null (active contract)</li>
 *   <li>If endDate is provided, it must be after startDate</li>
 * </ul>
 *
 * <p>Use {@link #of(OffsetDateTime, OffsetDateTime)} factory method to create instances.
 */
@Embeddable
public final class ContractPeriod {

    @Column(name = "start_date", nullable = false)
    private final OffsetDateTime startDate;

    @Column(name = "end_date")
    private final OffsetDateTime endDate;

    /**
     * JPA no-args constructor.
     * Protected to prevent direct instantiation outside JPA context.
     */
    protected ContractPeriod() {
        this.startDate = null;
        this.endDate = null;
    }

    /**
     * Private constructor to force use of {@link #of(OffsetDateTime, OffsetDateTime)} factory method.
     *
     * @param startDate the validated start date
     * @param endDate the validated end date (can be null)
     */
    private ContractPeriod(final OffsetDateTime startDate, final OffsetDateTime endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Factory method to create a validated ContractPeriod.
     *
     * <p>Business rules:
     * <ul>
     *   <li>If startDate is null, defaults to now</li>
     *   <li>endDate can be null (active contract)</li>
     *   <li>If endDate is provided, it must be after startDate</li>
     * </ul>
     *
     * @param startDate the start date (null defaults to now)
     * @param endDate the end date (null means active contract)
     * @return a validated ContractPeriod instance
     * @throws IllegalArgumentException if endDate is before or equal to startDate
     */
    public static ContractPeriod of(final OffsetDateTime startDate, final OffsetDateTime endDate) {
        // Business rule: startDate defaults to now if not provided
        final OffsetDateTime normalizedStart = (startDate != null) ? startDate : OffsetDateTime.now();

        // Business rule: if endDate is provided, it must be after startDate
        validateEndDateAfterStart(normalizedStart, endDate);

        return new ContractPeriod(normalizedStart, endDate);
    }

    /**
     * Validates that endDate is after startDate (business rule).
     *
     * @param startDate the start date (must not be null)
     * @param endDate the end date (can be null)
     * @throws IllegalArgumentException if endDate is before or equal to startDate
     */
    private static void validateEndDateAfterStart(final OffsetDateTime startDate, final OffsetDateTime endDate) {
        if (endDate != null && !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException(
                    "Contract end date must be after start date. " +
                    "Start: " + startDate + ", End: " + endDate
            );
        }
    }


    /**
     * Checks if the contract is active at a given reference date.
     * Business rule from specs: "active contracts (current date < end date)"
     *
     * @param referenceDate the reference date to check against
     * @return true if the contract is active at the reference date
     */
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

