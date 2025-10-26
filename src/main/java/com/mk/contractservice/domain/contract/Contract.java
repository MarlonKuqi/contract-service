package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.client.Client;
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
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "contract")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder(toBuilder = false)
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
    private OffsetDateTime lastModified;

    /**
     * Creates a new contract.
     *
     * @param client the client (must not be null)
     * @param period the contract period with validated start and end dates
     * @param costAmount the cost amount
     * @throws IllegalArgumentException if client, period, or costAmount is null
     */
    public Contract(final Client client, final ContractPeriod period, final ContractCost costAmount) {
        if (client == null) {
            throw new IllegalArgumentException("Client cannot be null for a contract");
        }
        if (period == null) {
            throw new IllegalArgumentException("Contract period cannot be null");
        }
        if (costAmount == null) {
            throw new IllegalArgumentException("Cost amount cannot be null");
        }
        this.client = client;
        this.period = period;
        this.costAmount = costAmount;
        this.lastModified = OffsetDateTime.now();
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.lastModified = OffsetDateTime.now();
    }

    /**
     * Changes the cost amount of the contract.
     * Automatically updates lastModified timestamp.
     *
     * @param newAmount the new cost amount (must not be null)
     * @throws IllegalArgumentException if newAmount is null
     */
    public void changeCost(final ContractCost newAmount) {
        if (newAmount == null) {
            throw new IllegalArgumentException("New cost amount cannot be null");
        }
        this.costAmount = newAmount;
        touch();
    }

    /**
     * Closes the contract by setting the end date to now.
     * Creates a new ContractPeriod with the current start date and end date = now.
     */
    public void closeNow() {
        this.period = ContractPeriod.of(this.period.startDate(), OffsetDateTime.now());
        touch();
    }
}
