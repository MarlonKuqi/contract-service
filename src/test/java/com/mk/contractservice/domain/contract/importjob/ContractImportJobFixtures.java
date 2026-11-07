package com.mk.contractservice.domain.contract.importjob;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public final class ContractImportJobFixtures {

    public static final String FILE_NAME = "contracts.csv";
    public static final String SUBMITTER = "marie.counselor";
    public static final String CANCELLER = "paul.counselor";
    public static final String CONTENT_SHA256 = "a".repeat(64);

    private static final LocalDateTime CANCELLED_JOB_CREATED_AT = LocalDateTime.of(2026, 11, 3, 10, 0);
    private static final LocalDateTime CANCELLED_JOB_CANCELLED_AT = LocalDateTime.of(2026, 11, 3, 10, 5);

    private ContractImportJobFixtures() {
    }

    public static ContractImportSubmission submission(final LocalDateTime submittedAt) {
        return ContractImportSubmission.of(FILE_NAME, SUBMITTER, null, CONTENT_SHA256, submittedAt);
    }

    public static ContractImportJob pendingJob(final int totalLines, final LocalDateTime submittedAt) {
        return ContractImportJob.of(submission(submittedAt), totalLines);
    }

    public static ContractImportJob runningJob(
            final UUID id,
            final int totalLines,
            final int importedCount,
            final int rejectedCount,
            final Instant leaseUntil,
            final LocalDateTime submittedAt
    ) {
        return ContractImportJob.reconstituteFromDatabase(
                id,
                submission(submittedAt),
                ContractImportJobState.Status.RUNNING,
                ContractImportProgress.reconstituteFromDatabase(totalLines, importedCount + rejectedCount, importedCount, rejectedCount),
                leaseUntil,
                null,
                null
        );
    }

    public static ContractImportJob cancelledJob(final UUID id) {
        return ContractImportJob.reconstituteFromDatabase(
                id,
                submission(CANCELLED_JOB_CREATED_AT),
                ContractImportJobState.Status.CANCELLED,
                ContractImportProgress.reconstituteFromDatabase(2, 1, 1, 0),
                null,
                ContractImportTermination.reconstituteFromDatabase(CANCELLED_JOB_CANCELLED_AT, null),
                ContractImportCancellation.reconstituteFromDatabase(CANCELLED_JOB_CANCELLED_AT, CANCELLER)
        );
    }
}
