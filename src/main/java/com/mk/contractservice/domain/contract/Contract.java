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

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Contract {

    final UUID id;

    final Client client;

    final ContractPeriod period;

    ContractCost costAmount;

    LocalDateTime lastModified;

    @Builder(toBuilder = true)
    private Contract(final UUID id, final Client client, final ContractPeriod period, final ContractCost costAmount, final LocalDateTime lastModified) {
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
        this.lastModified = lastModified != null ? lastModified : LocalDateTime.now();
    }


    public static Contract of(final Client client, final ContractPeriod period, final ContractCost costAmount) {
        return builder()
                .client(client)
                .period(period)
                .costAmount(costAmount)
                .build();
    }

    public static Contract reconstitute(final UUID id, final Client client, final ContractPeriod period, final ContractCost costAmount, final LocalDateTime lastModified) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null when reconstituting a Contract");
        }
        return builder()
                .id(id)
                .client(client)
                .period(period)
                .costAmount(costAmount)
                .lastModified(lastModified)
                .build();
    }


    private void updateLastModified() {
        this.lastModified = LocalDateTime.now();
    }

    public boolean isActive() {
        return period.isActive();
    }

    public boolean isInactive() {
        return !isActive();
    }

    public void changeCost(final ContractCost newAmount) {
        if (newAmount == null) {
            throw InvalidContractException.forNullNewCostAmount();
        }
        if (isInactive()) {
            throw new ExpiredContractException(getId());
        }
        this.costAmount = newAmount;
        updateLastModified();
    }
}

