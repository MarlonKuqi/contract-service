package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.contract.exception.ExpiredContractException;
import com.mk.contractservice.domain.contract.exception.InvalidContractPeriodException;
import com.mk.contractservice.domain.shared.ContractHolder;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Getter
@EqualsAndHashCode
public class Contract {

    private final @Nullable UUID id;
    private final @Nullable UUID clientId;
    private final ContractCoverage coverage;
    private final ContractPeriod period;
    private final ContractCost costAmount;
    private final LocalDateTime lastModified;

    @Builder(toBuilder = true, access = AccessLevel.PRIVATE)
    private Contract(
            @Nullable final UUID id,
            @Nullable final UUID clientId,
            @Nullable final ContractCoverage coverage,
            @Nullable final ContractPeriod period,
            @Nullable final ContractCost costAmount,
            @Nullable final LocalDateTime lastModified
    ) {
        this.id = id;
        this.clientId = notNull(clientId);
        this.coverage = notNull(coverage);
        this.period = notNull(period);
        this.costAmount = notNull(costAmount);
        this.lastModified = notNull(lastModified);
    }

    public static Contract of(
            @Nullable final UUID clientId,
            @Nullable final ContractHolder contractHolder,
            @Nullable final ContractCoverage coverage,
            @Nullable final ContractPeriod period,
            @Nullable final ContractCost costAmount,
            @Nullable final LocalDateTime now
    ) {
        final ContractCoverage contractCoverage = notNull(coverage);
        final ContractPeriod contractPeriod = notNull(period);
        contractCoverage.ensureAllowedFor(notNull(contractHolder), contractPeriod);
        return builder()
                .clientId(notNull(clientId))
                .coverage(contractCoverage)
                .period(contractPeriod)
                .costAmount(notNull(costAmount))
                .lastModified(notNull(now))
                .build();
    }

    public static Contract reconstituteFromDatabase(
            @Nullable final UUID id,
            @Nullable final UUID clientId,
            @Nullable final ContractCoverage coverage,
            @Nullable final ContractPeriod period,
            @Nullable final ContractCost costAmount,
            @Nullable final LocalDateTime lastModified
    ) {
        return builder()
                .id(notNull(id))
                .clientId(notNull(clientId))
                .coverage(notNull(coverage))
                .period(notNull(period))
                .costAmount(notNull(costAmount))
                .lastModified(notNull(lastModified))
                .build();
    }

    public ContractType type() {
        return coverage.type();
    }

    public Optional<LamalFranchise> franchise() {
        return switch (coverage) {
            case ContractCoverage.Lamal lamal -> Optional.of(lamal.franchise());
            case ContractCoverage.Laa _,ContractCoverage.Lpp _ -> Optional.empty();
        };
    }

    public boolean isActiveAt(final LocalDateTime referenceDate) {
        return period().isEffectiveAt(notNull(referenceDate));
    }

    public boolean isInactiveAt(final LocalDateTime referenceDate) {
        return !isActiveAt(referenceDate);
    }

    public Contract changeCost(@Nullable final ContractCost newCost, final LocalDateTime now) {
        final ContractCost cost = notNull(newCost);
        assertModifiableAt(now);
        if (costAmount.isSameAmountAs(cost)) {
            return this;
        }
        return toBuilder()
                .costAmount(cost)
                .lastModified(now)
                .build();
    }

    public Contract changePeriod(
            @Nullable final ContractPeriod newPeriod,
            @Nullable final ContractHolder contractHolder,
            final LocalDateTime now
    ) {
        final ContractPeriod changedPeriod = notNull(newPeriod);
        assertModifiableAt(now);
        if (period.equals(changedPeriod)) {
            return this;
        }
        if (changedPeriod.endsBefore(now)) {
            throw new InvalidContractPeriodException(
                    "Contract end date cannot be in the past. End: " + changedPeriod.endDate() + ", now: " + now
            );
        }
        coverage.ensureAllowedFor(notNull(contractHolder), changedPeriod);
        return toBuilder()
                .period(changedPeriod)
                .lastModified(now)
                .build();
    }

    private void assertModifiableAt(final LocalDateTime now) {
        if (isInactiveAt(now)) {
            throw new ExpiredContractException(id());
        }
    }
}
