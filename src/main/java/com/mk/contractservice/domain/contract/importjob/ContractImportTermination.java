package com.mk.contractservice.domain.contract.importjob;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ContractImportTermination {

    LocalDateTime finishedAt;

    @Nullable
    ContractImportFailureReason failureReason;

    public static ContractImportTermination ended(@Nullable final LocalDateTime now) {
        return new ContractImportTermination(notNull(now), null);
    }

    public static ContractImportTermination failed(
            @Nullable final ContractImportFailureReason reason,
            @Nullable final LocalDateTime now
    ) {
        return new ContractImportTermination(notNull(now), notNull(reason));
    }

    public static ContractImportTermination reconstituteFromDatabase(
            @Nullable final LocalDateTime finishedAt,
            @Nullable final ContractImportFailureReason failureReason
    ) {
        return new ContractImportTermination(notNull(finishedAt), failureReason);
    }
}
