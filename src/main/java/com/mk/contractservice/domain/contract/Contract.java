package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.exception.InvalidContractException;
import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contract")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Embedded
    @NotNull(message = "Contract period must not be null")
    @Valid
    private ContractPeriod period;

    @Embedded
    @NotNull(message = "Cost amount must not be null")
    @Valid
    private ContractCost costAmount;

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;

    public Contract(final Client client, final ContractPeriod period, final ContractCost costAmount) {
        if (client == null) {
            throw InvalidContractException.forNullClient();
        }
        if (period == null) {
            throw InvalidContractException.forNullPeriod();
        }
        if (costAmount == null) {
            throw InvalidContractException.forNullCostAmount();
        }
        this.client = client;
        this.period = period;
        this.costAmount = costAmount;
        this.lastModified = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.lastModified = LocalDateTime.now();
    }

    public void changeCost(final ContractCost newAmount) {
        if (newAmount == null) {
            throw InvalidContractException.forNullNewCostAmount();
        }
        this.costAmount = newAmount;
        touch();
    }
}

