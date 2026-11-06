package com.mk.contractservice.domain.contract.importjob;

import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContractImportJobLine {

    private final UUID jobId;
    private final int lineNumber;
    private final @Nullable UUID clientId;
    private final @Nullable UUID contractId;
    private final @Nullable ContractImportRejectionReason rejectionReason;
    private final @Nullable String rejectionMessage;

    public static ContractImportJobLine imported(
            @Nullable final UUID jobId,
            final int lineNumber,
            @Nullable final UUID clientId,
            @Nullable final UUID contractId
    ) {
        return new ContractImportJobLine(notNull(jobId), lineNumber, notNull(clientId), notNull(contractId), null, null);
    }

    public static ContractImportJobLine rejected(
            @Nullable final UUID jobId,
            final int lineNumber,
            @Nullable final UUID clientId,
            @Nullable final ContractImportRejectionReason reason,
            @Nullable final String message
    ) {
        return new ContractImportJobLine(notNull(jobId), lineNumber, clientId, null, notNull(reason), notNull(message));
    }
}
