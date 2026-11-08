package com.mk.contractservice.domain.contract.importjob;

import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

import static com.mk.contractservice.domain.shared.Assert.notNull;

public sealed interface ContractImportTermination permits ContractImportTermination.Ended,
        ContractImportTermination.Failed {

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

    record Ended(LocalDateTime finishedAt) implements ContractImportTermination {

        public Ended {
            notNull(finishedAt);
        }
    }

    record Failed(LocalDateTime finishedAt, ContractImportFailureReason reason) implements ContractImportTermination {

        public Failed {
            notNull(finishedAt);
            notNull(reason);
        }
    }
}
