package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.contract.exception.ExpiredContractException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Getter
@EqualsAndHashCode
public class Contract {

    private final @Nullable UUID id;
    private final @Nullable UUID clientId;
    private final ContractPeriod period;
    private final ContractCost costAmount;
    private final LocalDateTime lastModified;

    @Builder(toBuilder = true, access = AccessLevel.PRIVATE)
    private Contract(
            @Nullable final UUID id,
            @Nullable final UUID clientId,
            @Nullable final ContractPeriod period,
            @Nullable final ContractCost costAmount,
            @Nullable final LocalDateTime lastModified
    ) {
        this.id = id;
        this.clientId = notNull(clientId);
        this.period = notNull(period);
        this.costAmount = notNull(costAmount);
        this.lastModified = notNull(lastModified);
    }

    public static Contract of(
            @Nullable final UUID clientId,
            @Nullable final ContractPeriod period,
            @Nullable final ContractCost costAmount,
            @Nullable final LocalDateTime now
    ) {
        return builder()
                .clientId(notNull(clientId))
                .period(notNull(period))
                .costAmount(notNull(costAmount))
                .lastModified(notNull(now))
                .build();
    }

    public static Contract reconstituteFromDatabase(
            @Nullable final UUID id,
            @Nullable final UUID clientId,
            @Nullable final ContractPeriod period,
            @Nullable final ContractCost costAmount,
            @Nullable final LocalDateTime lastModified
    ) {
        return builder()
                .id(notNull(id))
                .clientId(notNull(clientId))
                .period(notNull(period))
                .costAmount(notNull(costAmount))
                .lastModified(notNull(lastModified))
                .build();
    }

    public boolean isActiveAt(final LocalDateTime referenceDate) {
        return getPeriod().isEffectiveAt(notNull(referenceDate));
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

    private void assertModifiableAt(final LocalDateTime now) {
        if (isInactiveAt(now)) {
            throw new ExpiredContractException(getId());
        }
    }
}
