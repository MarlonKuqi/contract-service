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

    @Builder(toBuilder = true, access = AccessLevel.PUBLIC)
    private Contract(
            @Nullable final UUID id,
            @Nullable final UUID clientId,
            @Nullable final ContractPeriod period,
            @Nullable final ContractCost costAmount
    ) {
        this.id = id;
        this.clientId = clientId;
        this.period = period;
        this.costAmount = costAmount;
    }

    public static Contract of(
            @Nullable final UUID clientId,
            @Nullable final ContractPeriod period,
            @Nullable final ContractCost costAmount
    ) {
        return builder()
                .clientId(notNull(clientId))
                .period(notNull(period))
                .costAmount(notNull(costAmount))
                .build();
    }

    public static Contract reconstituteFromDatabase(
            @Nullable final UUID id,
            @Nullable final UUID clientId,
            @Nullable final ContractPeriod period,
            @Nullable final ContractCost costAmount
    ) {
        return builder()
                .id(notNull(id))
                .clientId(clientId)
                .period(notNull(period))
                .costAmount(notNull(costAmount))
                .build();
    }

    public boolean isActiveAt(final LocalDateTime referenceDate) {
        return getPeriod().isEffectiveAt(notNull(referenceDate));
    }

    public boolean isInactiveAt(final LocalDateTime referenceDate) {
        return !isActiveAt(referenceDate);
    }

    public Contract changeCost(@Nullable final ContractCost newCost, final LocalDateTime now) {
        assertModifiableAt(now);
        return toBuilder()
                .costAmount(notNull(newCost))
                .build();
    }

    private void assertModifiableAt(final LocalDateTime now) {
        if (isInactiveAt(now)) {
            throw new ExpiredContractException(getId());
        }
    }
}
