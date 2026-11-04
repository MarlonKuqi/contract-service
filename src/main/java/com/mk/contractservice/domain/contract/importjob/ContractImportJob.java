package com.mk.contractservice.domain.contract.importjob;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Getter
public class ContractImportJob {

    private final @Nullable UUID id;
    private final String fileName;
    private final ContractImportJobStatus status;
    private final int totalLines;
    private final int processedLines;
    private final int importedCount;
    private final int rejectedCount;
    private final @Nullable Instant leaseUntil;
    private final LocalDateTime createdAt;
    private final @Nullable LocalDateTime finishedAt;
    private final @Nullable ContractImportFailureReason failureReason;

    @Builder(toBuilder = true, access = AccessLevel.PRIVATE)
    private ContractImportJob(
            @Nullable final UUID id,
            @Nullable final String fileName,
            @Nullable final ContractImportJobStatus status,
            final int totalLines,
            final int processedLines,
            final int importedCount,
            final int rejectedCount,
            @Nullable final Instant leaseUntil,
            @Nullable final LocalDateTime createdAt,
            @Nullable final LocalDateTime finishedAt,
            @Nullable final ContractImportFailureReason failureReason
    ) {
        this.id = id;
        this.fileName = notNull(fileName);
        this.status = notNull(status);
        this.totalLines = totalLines;
        this.processedLines = processedLines;
        this.importedCount = importedCount;
        this.rejectedCount = rejectedCount;
        this.leaseUntil = leaseUntil;
        this.createdAt = notNull(createdAt);
        this.finishedAt = finishedAt;
        this.failureReason = failureReason;
    }

    public static ContractImportJob of(
            @Nullable final String fileName,
            final int totalLines,
            @Nullable final LocalDateTime now
    ) {
        return builder()
                .fileName(fileName)
                .status(ContractImportJobStatus.PENDING)
                .totalLines(totalLines)
                .createdAt(now)
                .build();
    }

    public static ContractImportJob reconstituteFromDatabase(
            @Nullable final UUID id,
            @Nullable final String fileName,
            @Nullable final ContractImportJobStatus status,
            final int totalLines,
            final int processedLines,
            final int importedCount,
            final int rejectedCount,
            @Nullable final Instant leaseUntil,
            @Nullable final LocalDateTime createdAt,
            @Nullable final LocalDateTime finishedAt,
            @Nullable final ContractImportFailureReason failureReason
    ) {
        return builder()
                .id(notNull(id))
                .fileName(fileName)
                .status(status)
                .totalLines(totalLines)
                .processedLines(processedLines)
                .importedCount(importedCount)
                .rejectedCount(rejectedCount)
                .leaseUntil(leaseUntil)
                .createdAt(createdAt)
                .finishedAt(finishedAt)
                .failureReason(failureReason)
                .build();
    }

    public ContractImportJob start(final Instant lease) {
        return toBuilder()
                .status(ContractImportJobStatus.RUNNING)
                .leaseUntil(notNull(lease))
                .build();
    }

    public ContractImportJob recordBatch(final int imported, final int rejected, final Instant renewedLease) {
        assertRunning();
        final int processed = processedLines + imported + rejected;
        if (processed > totalLines) {
            throw new IllegalStateException("Job " + id + " processed " + processed + " lines out of " + totalLines);
        }
        return toBuilder()
                .processedLines(processed)
                .importedCount(importedCount + imported)
                .rejectedCount(rejectedCount + rejected)
                .leaseUntil(notNull(renewedLease))
                .build();
    }

    public ContractImportJob finish(final LocalDateTime now) {
        assertRunning();
        if (isIncompleteProcessing()) {
            return fail(ContractImportFailureReason.INCOMPLETE_PROCESSING, now);
        }
        return toBuilder()
                .status(ContractImportJobStatus.COMPLETED)
                .leaseUntil(null)
                .finishedAt(notNull(now))
                .build();
    }

    private boolean isIncompleteProcessing() {
        return processedLines != totalLines;
    }

    public ContractImportJob release() {
        assertRunning();
        return toBuilder()
                .status(ContractImportJobStatus.PENDING)
                .leaseUntil(null)
                .build();
    }

    public ContractImportJob fail(final ContractImportFailureReason reason, final LocalDateTime now) {
        assertRunning();
        return toBuilder()
                .status(ContractImportJobStatus.FAILED)
                .leaseUntil(null)
                .finishedAt(notNull(now))
                .failureReason(notNull(reason))
                .build();
    }

    private void assertRunning() {
        if (status.isNotRunning()) {
            throw new IllegalStateException("Job " + id + " is not running: " + status);
        }
    }
}
