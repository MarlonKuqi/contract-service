package com.mk.contractservice.domain.contract.importjob;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

import static com.mk.contractservice.domain.shared.Assert.notBlank;
import static com.mk.contractservice.domain.shared.Assert.notNull;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ContractImportCancellation {

    LocalDateTime cancelledAt;

    String cancelledBy;

    public static ContractImportCancellation of(
            @Nullable final LocalDateTime cancelledAt,
            @Nullable final String cancelledBy
    ) {
        return new ContractImportCancellation(notNull(cancelledAt), notBlank(cancelledBy));
    }

    public static ContractImportCancellation reconstituteFromDatabase(
            @Nullable final LocalDateTime cancelledAt,
            @Nullable final String cancelledBy
    ) {
        return new ContractImportCancellation(notNull(cancelledAt), notNull(cancelledBy));
    }
}
