package com.mk.contractservice.domain.contract.importjob;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ContractImportRejection {

    int lineNumber;

    @Nullable
    UUID clientId;

    ContractImportRejectionReason reason;

    String message;

    public static ContractImportRejection reconstituteFromDatabase(
            final int lineNumber,
            @Nullable final UUID clientId,
            @Nullable final ContractImportRejectionReason reason,
            @Nullable final String message
    ) {
        return new ContractImportRejection(lineNumber, clientId, notNull(reason), notNull(message));
    }
}
