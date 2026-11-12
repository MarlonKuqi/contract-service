package com.mk.contractservice.domain.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJobState.Cancelled;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobState.Completed;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobState.Failed;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobState.Pending;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobState.Running;
import com.mk.contractservice.domain.contract.importjob.exception.ContractImportJobCancelledException;
import com.mk.contractservice.domain.contract.importjob.exception.ContractImportJobTransitionNotAllowedException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@Getter
@EqualsAndHashCode
public final class ContractImportJob {

    private final @Nullable UUID id;
    private final ContractImportSubmission submission;
    private final ContractImportJobState state;
    private final ContractImportProgress progress;

    @Builder(toBuilder = true, access = AccessLevel.PRIVATE)
    private ContractImportJob(
            @Nullable final UUID id,
            @Nullable final ContractImportSubmission submission,
            @Nullable final ContractImportJobState state,
            @Nullable final ContractImportProgress progress
    ) {
        this.id = id;
        this.submission = notNull(submission);
        this.state = notNull(state);
        this.progress = notNull(progress);
    }

    public static ContractImportJob of(@Nullable final ContractImportSubmission submission, final int totalLines) {
        return builder()
                .submission(submission)
                .state(ContractImportJobState.pending())
                .progress(ContractImportProgress.notStarted(totalLines))
                .build();
    }

    public static ContractImportJob reconstituteFromDatabase(
            @Nullable final UUID id,
            @Nullable final ContractImportSubmission submission,
            final ContractImportJobState.@Nullable Status status,
            @Nullable final ContractImportProgress progress,
            @Nullable final Instant leaseUntil,
            @Nullable final ContractImportTermination termination,
            @Nullable final ContractImportCancellation cancellation
    ) {
        return builder()
                .id(notNull(id))
                .submission(submission)
                .state(ContractImportJobState.reconstituteFromDatabase(status, leaseUntil, termination, cancellation))
                .progress(progress)
                .build();
    }

    public ContractImportJobState.Status status() {
        return state.status();
    }

    public boolean hasContent(final String sha256) {
        return submission.hasContent(sha256);
    }

    public ContractImportJob start(final Instant lease) {
        return switch (state) {
            case Pending _,Running _ -> withState(ContractImportJobState.running(lease));
            case Completed _,Failed _,Cancelled _ -> throw notAllowed("start");
        };
    }

    public ContractImportJob recordBatch(final int imported, final int rejected, final Instant renewedLease) {
        return switch (state) {
            case Running _ -> toBuilder()
                    .progress(progress.advance(imported, rejected))
                    .state(ContractImportJobState.running(renewedLease))
                    .build();
            case Cancelled _ -> throw new ContractImportJobCancelledException(id);
            case Pending _,Completed _,Failed _ -> throw notAllowed("record a batch");
        };
    }

    public ContractImportJob finish(final LocalDateTime now) {
        return switch (state) {
            case Running _ -> progress.isComplete() ? withState(ContractImportJobState.completed(
                    ContractImportTermination.ended(now))) : fail(ContractImportFailureReason.INCOMPLETE_PROCESSING,
                            now);
            case Cancelled _ -> this;
            case Pending _,Completed _,Failed _ -> throw notAllowed("finish");
        };
    }

    public ContractImportJob release() {
        return switch (state) {
            case Running _ -> withState(ContractImportJobState.pending());
            case Cancelled _ -> this;
            case Pending _,Completed _,Failed _ -> throw notAllowed("release");
        };
    }

    public ContractImportJob fail(final ContractImportFailureReason reason, final LocalDateTime now) {
        return switch (state) {
            case Running _ -> withState(ContractImportJobState.failed(ContractImportTermination.failed(reason, now)));
            case Cancelled _ -> this;
            case Pending _,Completed _,Failed _ -> throw notAllowed("fail");
        };
    }

    public ContractImportJob cancel(final LocalDateTime now, final String counselor) {
        return switch (state) {
            case Pending _,Running _ ->
                withState(ContractImportJobState.cancelled(ContractImportCancellation.of(now, counselor),
                        ContractImportTermination.ended(now)));
            case Completed(ContractImportTermination.Ended ended) ->
                withState(ContractImportJobState.cancelled(ContractImportCancellation.of(now, counselor), ended));
            case Failed(ContractImportTermination.Failed failed) ->
                withState(ContractImportJobState.cancelled(ContractImportCancellation.of(now, counselor), failed));
            case Cancelled _ -> this;
        };
    }

    private ContractImportJob withState(final ContractImportJobState newState) {
        return toBuilder().state(newState).build();
    }

    private ContractImportJobTransitionNotAllowedException notAllowed(final String transition) {
        return new ContractImportJobTransitionNotAllowedException(id, transition, status());
    }
}
