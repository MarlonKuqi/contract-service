package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.valueobject.MoneyAmount;
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

    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;

    @Column(name = "end_date")
    private OffsetDateTime endDate;

    @Embedded
    private MoneyAmount costAmount;

    @Column(name = "last_modified", nullable = false)
    private OffsetDateTime lastModified;


    public Contract(final Client client, final OffsetDateTime startDate, final OffsetDateTime endDate, final MoneyAmount costAmount) {
        if (client == null) {
            throw new IllegalArgumentException("Client cannot be null for a contract");
        }
        this.client = client;
        this.startDate = startDate;
        this.endDate = endDate;
        this.costAmount = costAmount;
        this.lastModified = OffsetDateTime.now();
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.lastModified = OffsetDateTime.now();
    }

    public void changeCost(MoneyAmount newAmount) {
        this.costAmount = newAmount;
        touch();
    }

    public void closeNow() {
        this.endDate = OffsetDateTime.now();
        touch();
    }
}
