package com.mk.contractservice.domain.contract.importjob;

import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

import static com.mk.contractservice.domain.shared.Assert.notNull;

public sealed interface ContractImportJobLine permits ContractImportJobLine.Imported, ContractImportJobLine.Rejected {

    UUID jobId();

    int lineNumber();

    @Nullable
    UUID clientId();

    static Imported imported(
            @Nullable final UUID jobId,
            final int lineNumber,
            @Nullable final UUID clientId,
            @Nullable final UUID contractId
    ) {
        return new Imported(notNull(jobId), lineNumber, notNull(clientId), notNull(contractId));
    }

    static Rejected rejected(
            @Nullable final UUID jobId,
            final int lineNumber,
            @Nullable final UUID clientId,
            @Nullable final ContractImportRejectionReason reason,
            @Nullable final String message
    ) {
        return new Rejected(notNull(jobId), lineNumber, clientId, notNull(reason), notNull(message));
    }

    record Imported(UUID jobId, int lineNumber, UUID clientId, UUID contractId) implements ContractImportJobLine {

        public Imported {
            notNull(jobId);
            notNull(clientId);
            notNull(contractId);
        }
    }

    record Rejected(
            UUID jobId,
            int lineNumber,
            @Nullable UUID clientId,
            ContractImportRejectionReason reason,
            String message
    ) implements ContractImportJobLine {

        public Rejected {
            notNull(jobId);
            notNull(reason);
            notNull(message);
        }
    }
}
