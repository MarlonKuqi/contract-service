package com.mk.contractservice.infrastructure.persistence.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportFailureReason;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contract_import_job", schema = "contracts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContractImportJobJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "file_name", nullable = false, updatable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContractImportJobState.Status status;

    @Column(name = "total_lines", nullable = false, updatable = false)
    private int totalLines;

    @Column(name = "processed_lines", nullable = false)
    private int processedLines;

    @Column(name = "imported_count", nullable = false)
    private int importedCount;

    @Column(name = "rejected_count", nullable = false)
    private int rejectedCount;

    @Nullable
    @Column(name = "lease_until")
    private Instant leaseUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Nullable
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Nullable
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason")
    private ContractImportFailureReason failureReason;

    @Nullable
    @Column(name = "submitted_by", updatable = false)
    private String submittedBy;

    @Nullable
    @Column(name = "idempotency_key", updatable = false)
    private String idempotencyKey;

    @Column(name = "content_sha256", nullable = false, updatable = false, length = 64)
    private String contentSha256;

    @Nullable
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Nullable
    @Column(name = "cancelled_by")
    private String cancelledBy;

    public ContractImportJobJpaEntity(
            final String fileName,
            final int totalLines,
            final LocalDateTime createdAt,
            @Nullable final String submittedBy,
            @Nullable final String idempotencyKey,
            final String contentSha256
    ) {
        this.fileName = fileName;
        this.totalLines = totalLines;
        this.createdAt = createdAt;
        this.submittedBy = submittedBy;
        this.idempotencyKey = idempotencyKey;
        this.contentSha256 = contentSha256;
    }
}
