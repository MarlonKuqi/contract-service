package com.mk.contractservice.domain.contract.importjob;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

import static com.mk.contractservice.domain.shared.Assert.notNull;

public sealed interface ContractImportTermination permits ContractImportTermination.Ended, ContractImportTermination.Failed {

    LocalDateTime finishedAt();

    static Ended ended(@Nullable final LocalDateTime now) {
        return new Ended(notNull(now));
    }

    static Failed failed(@Nullable final ContractImportFailureReason reason, @Nullable final LocalDateTime now) {
        return new Failed(notNull(now), notNull(reason));
    }

    static ContractImportTermination reconstituteFromDatabase(
            @Nullable final LocalDateTime finishedAt,
            @Nullable final ContractImportFailureReason failureReason
    ) {
        return failureReason == null ? ended(finishedAt) : failed(failureReason, finishedAt);
    }

    @Value
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Ended implements ContractImportTermination {

        LocalDateTime finishedAt;
    }

    @Value
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Failed implements ContractImportTermination {

        LocalDateTime finishedAt;

        ContractImportFailureReason reason;
    }
}
