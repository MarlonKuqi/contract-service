package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.exception.ExpiredContractException;
import com.mk.contractservice.domain.exception.InvalidContractException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class Contract {

    @Nullable
    UUID id;
    UUID clientId;
    ContractPeriod period;
    ContractCost costAmount;

    @Builder(toBuilder = true)
    private Contract(
            @Nullable final UUID id,
            @Nullable final UUID clientId,
            @Nullable final ContractPeriod period,
            @Nullable final ContractCost costAmount
    ) {
        if (clientId == null) {
            throw InvalidContractException.forNullClient();
        }
        if (period == null) {
            throw InvalidContractException.forNullPeriod();
        }
        if (costAmount == null) {
            throw InvalidContractException.forNullCostAmount();
        }
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
                .clientId(clientId)
                .period(period)
                .costAmount(costAmount)
                .build();
    }

    public static Contract reconstitute(
            @Nullable final UUID id,
            @Nullable final UUID clientId,
            @Nullable final ContractPeriod period,
            @Nullable final ContractCost costAmount
    ) {
        return builder()
                .id(id)
                .clientId(clientId)
                .period(period)
                .costAmount(costAmount)
                .build();
    }

    public boolean isActive() {
        return period.isActive();
    }

    public boolean isInactive() {
        return !isActive();
    }

    public Contract changeCost(final ContractCost newAmount) {
        if (newAmount == null) {
            throw InvalidContractException.forNullNewCostAmount();
        }
        if (isInactive()) {
            throw new ExpiredContractException(getId());
        }
        return toBuilder()
                .costAmount(newAmount)
                .build();
    }
}

