package com.mk.contractservice.infrastructure.persistence.contract.importjob;

import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "contract_import_job_line", schema = "contracts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ContractImportJobLineJpaEntity {

    @Embeddable
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class LineId implements Serializable {

        @Column(name = "job_id", nullable = false, updatable = false)
        private UUID jobId;

        @Column(name = "line_number", nullable = false, updatable = false)
        private int lineNumber;
    }

    @EmbeddedId
    private LineId id;

    @Nullable
    @Column(name = "client_id", updatable = false)
    private UUID clientId;

    @Nullable
    @Column(name = "contract_id", updatable = false)
    private UUID contractId;

    @Nullable
    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason", updatable = false)
    private ContractImportRejectionReason rejectionReason;

    @Nullable
    @Column(name = "rejection_message", updatable = false)
    private String rejectionMessage;
}
