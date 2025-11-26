package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.exception.ExpiredContractException;
import com.mk.contractservice.domain.exception.InvalidContractException;
import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;
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

    Client client;

    ContractPeriod period;

    ContractCost costAmount;

    @Builder(toBuilder = true)
    private Contract(
            @Nullable final UUID id,
            @Nullable final Client client,
            @Nullable final ContractPeriod period,
            @Nullable final ContractCost costAmount
    ) {
        if (client == null) {
            throw InvalidContractException.forNullClient();
        }
        if (period == null) {
            throw InvalidContractException.forNullPeriod();
        }
        if (costAmount == null) {
            throw InvalidContractException.forNullCostAmount();
        }
        this.id = id;
        this.client = client;
        this.period = period;
        this.costAmount = costAmount;
    }

    public static Contract of(final Client client, final ContractPeriod period, final ContractCost costAmount) {
        return builder()
                .client(client)
                .period(period)
                .costAmount(costAmount)
                .build();
    }

    public static Contract reconstitute(final UUID id, final Client client, final ContractPeriod period, final ContractCost costAmount) {
        return builder()
                .id(id)
                .client(client)
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

    @SuppressWarnings("ConstantConditions")
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

