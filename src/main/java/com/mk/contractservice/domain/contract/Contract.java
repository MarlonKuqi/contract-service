package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.exception.InvalidContractException;
import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Contract {

    private UUID id;

    private final Client client;

    private final ContractPeriod period;

    private ContractCost costAmount;

    private LocalDateTime lastModified;

    private Contract(final UUID id, final Client client, final ContractPeriod period, final ContractCost costAmount) {
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
        this.lastModified = LocalDateTime.now();
    }


    public static ContractBuilder builder() {
        return new ContractBuilder();
    }

    private void touch() {
        this.lastModified = LocalDateTime.now();
    }

    public boolean isActive() {
        return period.isActive();
    }

    public void changeCost(final ContractCost newAmount) {
        if (newAmount == null) {
            throw InvalidContractException.forNullNewCostAmount();
        }
        this.costAmount = newAmount;
        touch();
    }

    public static class ContractBuilder {
        private UUID id;
        private Client client;
        private ContractPeriod period;
        private ContractCost costAmount;

        public ContractBuilder id(final UUID id) {
            this.id = id;
            return this;
        }

        public ContractBuilder client(final Client client) {
            this.client = client;
            return this;
        }

        public ContractBuilder period(final ContractPeriod period) {
            this.period = period;
            return this;
        }

        public ContractBuilder costAmount(final ContractCost costAmount) {
            this.costAmount = costAmount;
            return this;
        }

        public Contract build() {
            return new Contract(id, client, period, costAmount);
        }
    }
}

