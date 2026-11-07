package com.mk.contractservice.domain.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.exception.ContractImportJobCancelledException;
import com.mk.contractservice.domain.contract.importjob.exception.ContractImportJobTransitionNotAllowedException;
import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Getter
public final class ContractImportJob {

    private final @Nullable UUID id;
    private final ContractImportSubmission submission;
    private final ContractImportJobStatus status;
    private final ContractImportProgress progress;
    private final @Nullable Instant leaseUntil;
    private final @Nullable ContractImportTermination termination;
    private final @Nullable ContractImportCancellation cancellation;

    @Builder(toBuilder = true, access = AccessLevel.PRIVATE)
    private ContractImportJob(
            @Nullable final UUID id,
            @Nullable final ContractImportSubmission submission,
            @Nullable final ContractImportJobStatus status,
            @Nullable final ContractImportProgress progress,
            @Nullable final Instant leaseUntil,
            @Nullable final ContractImportTermination termination,
            @Nullable final ContractImportCancellation cancellation
    ) {
        this.id = id;
        this.submission = notNull(submission);
        this.status = notNull(status);
        this.progress = notNull(progress);
        this.leaseUntil = leaseUntil;
        this.termination = termination;
        this.cancellation = cancellation;
        requireConsistentState();
    }

    private void requireConsistentState() {
        final boolean consistent = switch (status) {
            case PENDING, RUNNING -> cancellation == null && termination == null;
            case COMPLETED -> cancellation == null && termination instanceof ContractImportTermination.Ended;
            case FAILED -> cancellation == null && termination instanceof ContractImportTermination.Failed;
            case CANCELLED -> cancellation != null && termination != null;
        };
        if (!consistent) {
            throw new InvalidDomainObjectError(
                    "Import job " + id + " cannot be " + status + " with termination " + termination + " and cancellation " + cancellation);
        }
    }

    public static ContractImportJob of(@Nullable final ContractImportSubmission submission, final int totalLines) {
        return builder()
                .submission(submission)
                .status(ContractImportJobStatus.PENDING)
                .progress(ContractImportProgress.notStarted(totalLines))
                .build();
    }

    public static ContractImportJob reconstituteFromDatabase(
            @Nullable final UUID id,
            @Nullable final ContractImportSubmission submission,
            @Nullable final ContractImportJobStatus status,
            @Nullable final ContractImportProgress progress,
            @Nullable final Instant leaseUntil,
            @Nullable final ContractImportTermination termination,
            @Nullable final ContractImportCancellation cancellation
    ) {
        return builder()
                .id(notNull(id))
                .submission(submission)
                .status(status)
                .progress(progress)
                .leaseUntil(leaseUntil)
                .termination(termination)
                .cancellation(cancellation)
                .build();
    }

    public boolean hasContent(final String sha256) {
        return submission.hasContent(sha256);
    }

    public ContractImportJob start(final Instant lease) {
        return switch (status) {
            case PENDING, RUNNING -> toBuilder()
                    .status(ContractImportJobStatus.RUNNING)
                    .leaseUntil(notNull(lease))
                    .build();
            case COMPLETED, FAILED, CANCELLED -> throw notAllowed("start");
        };
    }

    public ContractImportJob recordBatch(final int imported, final int rejected, final Instant renewedLease) {
        return switch (status) {
            case RUNNING -> toBuilder()
                    .progress(progress.advance(imported, rejected))
                    .leaseUntil(notNull(renewedLease))
                    .build();
            case CANCELLED -> throw new ContractImportJobCancelledException(id);
            case PENDING, COMPLETED, FAILED -> throw notAllowed("record a batch");
        };
    }

    public ContractImportJob finish(final LocalDateTime now) {
        return switch (status) {
            case RUNNING -> progress.isComplete()
                    ? complete(now)
                    : fail(ContractImportFailureReason.INCOMPLETE_PROCESSING, now);
            case CANCELLED -> this;
            case PENDING, COMPLETED, FAILED -> throw notAllowed("finish");
        };
    }

    private ContractImportJob complete(final LocalDateTime now) {
        return toBuilder()
                .status(ContractImportJobStatus.COMPLETED)
                .leaseUntil(null)
                .termination(ContractImportTermination.ended(now))
                .build();
    }

    public ContractImportJob release() {
        return switch (status) {
            case RUNNING -> toBuilder()
                    .status(ContractImportJobStatus.PENDING)
                    .leaseUntil(null)
                    .build();
            case CANCELLED -> this;
            case PENDING, COMPLETED, FAILED -> throw notAllowed("release");
        };
    }

    public ContractImportJob fail(final ContractImportFailureReason reason, final LocalDateTime now) {
        return switch (status) {
            case RUNNING -> toBuilder()
                    .status(ContractImportJobStatus.FAILED)
                    .leaseUntil(null)
                    .termination(ContractImportTermination.failed(reason, now))
                    .build();
            case CANCELLED -> this;
            case PENDING, COMPLETED, FAILED -> throw notAllowed("fail");
        };
    }

    public ContractImportJob cancel(final LocalDateTime now, final String counselor) {
        return switch (status) {
            case PENDING, RUNNING, COMPLETED, FAILED -> toBuilder()
                    .status(ContractImportJobStatus.CANCELLED)
                    .leaseUntil(null)
                    .termination(termination == null ? ContractImportTermination.ended(now) : termination)
                    .cancellation(ContractImportCancellation.of(now, counselor))
                    .build();
            case CANCELLED -> this;
        };
    }

    private ContractImportJobTransitionNotAllowedException notAllowed(final String transition) {
        return new ContractImportJobTransitionNotAllowedException(id, transition, status);
    }
}
