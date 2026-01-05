package com.mk.contractservice.infrastructure.persistence.contract.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contract")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContractJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Version
    @Column(name = "version", nullable = false)
    Long version = 0L;

    @Nullable
    @Column(name = "client_id")
    UUID clientId;

    @Column(name = "start_date", nullable = false)
    LocalDateTime startDate;

    @Nullable
    @Column(name = "end_date")
    LocalDateTime endDate;

    @Column(name = "cost_amount", nullable = false, precision = 19, scale = 2)
    BigDecimal costAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_modified", nullable = false)
    LocalDateTime lastModified;

    public ContractJpaEntity(
            @Nullable final UUID clientId,
            final LocalDateTime startDate,
            @Nullable final LocalDateTime endDate,
            final BigDecimal costAmount
    ) {
        this.clientId = clientId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.costAmount = costAmount;
    }
}

