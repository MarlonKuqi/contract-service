package com.mk.contractservice.infrastructure.persistence.contract;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contract", schema = "contracts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContractJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Nullable
    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Nullable
    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "cost_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal costAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;

    public ContractJpaEntity(
            @Nullable final UUID clientId,
            final LocalDateTime startDate,
            @Nullable final LocalDateTime endDate,
            final BigDecimal costAmount,
            final LocalDateTime lastModified
    ) {
        this.clientId = clientId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.costAmount = costAmount;
        this.lastModified = lastModified;
    }
}
