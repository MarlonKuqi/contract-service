package com.mk.contractservice.domain.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.exception.ContractImportJobCancelledException;
import com.mk.contractservice.domain.contract.importjob.exception.ContractImportJobTransitionNotAllowedException;
import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.mk.contractservice.domain.contract.importjob.ContractImportJobFixtures.CANCELLER;
import static com.mk.contractservice.domain.contract.importjob.ContractImportJobFixtures.CONTENT_SHA256;
import static com.mk.contractservice.domain.contract.importjob.ContractImportJobFixtures.FILE_NAME;
import static com.mk.contractservice.domain.contract.importjob.ContractImportJobFixtures.SUBMITTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ContractImportJob")
class ContractImportJobTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 3, 10, 0);
    private static final Instant LEASE = Instant.parse("2026-11-03T09:05:00Z");
    private static final ContractImportFailureReason REASON = ContractImportFailureReason.UNEXPECTED_ERROR;
    private static final LocalDateTime LATER = NOW.plusHours(1);
    private static final ContractImportProgress PROGRESS = ContractImportProgress.reconstituteFromDatabase(3, 1, 1, 0);

    private ContractImportJob pendingJob(final int totalLines) {
        return ContractImportJobFixtures.pendingJob(totalLines, NOW);
    }

    private ContractImportJob completedJob() {
        return pendingJob(1).start(LEASE).recordBatch(1, 0, LEASE).finish(NOW);
    }

    private static LocalDateTime finishedAtOf(final ContractImportJob job) {
        final ContractImportTermination termination = job.state().termination();
        return termination == null ? null : termination.finishedAt();
    }

    private static @Nullable ContractImportFailureReason failureReasonOf(final ContractImportJob job) {
        return switch (job.state().termination()) {
            case ContractImportTermination.Failed(_, ContractImportFailureReason reason) -> reason;
            case ContractImportTermination.Ended _ -> null;
            case null -> null;
        };
    }

    private static ContractImportJob reconstitute(
            final UUID id,
            final ContractImportSubmission submission,
            final ContractImportJobState.Status status,
            final ContractImportProgress progress
    ) {
        return ContractImportJob.reconstituteFromDatabase(id, submission, status, progress, LEASE, null, null);
    }

    @Test
    @DisplayName("GIVEN valid data WHEN of THEN job is pending with empty counters")
    void shouldCreatePendingJob() {
        final ContractImportJob job = pendingJob(3);

        assertThat(job.id()).isNull();
        assertThat(job.status()).isEqualTo(ContractImportJobState.Status.PENDING);
        assertThat(job.submission().fileName()).isEqualTo(FILE_NAME);
        assertThat(job.submission().submittedAt()).isEqualTo(NOW);
        assertThat(job.submission().submittedBy()).isEqualTo(SUBMITTER);
        assertThat(job.submission().idempotencyKey()).isNull();
        assertThat(job.submission().contentSha256()).isEqualTo(CONTENT_SHA256);
        assertThat(job.progress().totalLines()).isEqualTo(3);
        assertThat(job.progress().processedLines()).isZero();
        assertThat(job.progress().importedCount()).isZero();
        assertThat(job.progress().rejectedCount()).isZero();
        assertThat(job.state().termination()).isNull();
        assertThat(job.state().cancellation()).isNull();
    }

    @Test
    @DisplayName("GIVEN an idempotency key WHEN of THEN the job keeps it")
    void shouldKeepIdempotencyKey() {
        final ContractImportJob job = ContractImportJob.of(
                ContractImportSubmission.of(FILE_NAME, SUBMITTER, IdempotencyKey.of("key-1"), CONTENT_SHA256, NOW),
                3);

        assertThat(job.submission().idempotencyKey()).isEqualTo(IdempotencyKey.of("key-1"));
    }

    @Test
    @DisplayName("GIVEN a job WHEN comparing its content THEN only the same fingerprint matches")
    void shouldRecognizeItsContent() {
        final ContractImportJob job = pendingJob(3);

        assertThat(job.hasContent(CONTENT_SHA256)).isTrue();
        assertThat(job.hasContent("b".repeat(64))).isFalse();
    }

    @Test
    @DisplayName("GIVEN a null field WHEN of THEN it is rejected")
    void shouldRejectNullFields() {
        assertThatThrownBy(() -> ContractImportSubmission.of(null, SUBMITTER, null, CONTENT_SHA256, NOW)).isInstanceOf(
                InvalidDomainObjectError.class);
        assertThatThrownBy(() -> ContractImportSubmission.of(FILE_NAME, SUBMITTER, null, CONTENT_SHA256, null))
                .isInstanceOf(InvalidDomainObjectError.class);
        assertThatThrownBy(() -> ContractImportSubmission.of(FILE_NAME, null, null, CONTENT_SHA256, NOW)).isInstanceOf(
                InvalidDomainObjectError.class);
        assertThatThrownBy(() -> ContractImportSubmission.of(FILE_NAME, SUBMITTER, null, null, NOW)).isInstanceOf(
                InvalidDomainObjectError.class);
        assertThatThrownBy(() -> ContractImportJob.of(null, 3)).isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName("GIVEN a blank file name or submitter WHEN of THEN it is rejected")
    void shouldRejectBlankFields() {
        assertThatThrownBy(() -> ContractImportSubmission.of(" ", SUBMITTER, null, CONTENT_SHA256, NOW)).isInstanceOf(
                InvalidDomainObjectError.class);
        assertThatThrownBy(() -> ContractImportSubmission.of(FILE_NAME, " ", null, CONTENT_SHA256, NOW)).isInstanceOf(
                InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName("GIVEN a file without any line WHEN of THEN it is rejected")
    void shouldRejectJobWithoutLines() {
        final ContractImportSubmission submission = ContractImportJobFixtures.submission(NOW);

        assertThatThrownBy(() -> ContractImportJob.of(submission, 0)).isInstanceOf(InvalidDomainObjectError.class);
        assertThatThrownBy(() -> ContractImportJob.of(submission, -1)).isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName("GIVEN database values WHEN reconstituteFromDatabase THEN job keeps them")
    void shouldReconstitute() {
        final UUID id = UUID.randomUUID();
        final ContractImportSubmission submission = ContractImportSubmission.reconstituteFromDatabase(FILE_NAME,
                SUBMITTER,
                IdempotencyKey.reconstituteFromDatabase("key-1"),
                CONTENT_SHA256,
                NOW);

        final ContractImportJob job = ContractImportJob.reconstituteFromDatabase(
                id,
                submission,
                ContractImportJobState.Status.CANCELLED,
                PROGRESS,
                null,
                ContractImportTermination.reconstituteFromDatabase(NOW, REASON),
                ContractImportCancellation.reconstituteFromDatabase(LATER, CANCELLER));

        assertThat(job.id()).isEqualTo(id);
        assertThat(job.status()).isEqualTo(ContractImportJobState.Status.CANCELLED);
        assertThat(job.progress().processedLines()).isEqualTo(1);
        assertThat(finishedAtOf(job)).isEqualTo(NOW);
        assertThat(failureReasonOf(job)).isEqualTo(REASON);
        assertThat(job.submission().submittedBy()).isEqualTo(SUBMITTER);
        assertThat(job.submission().idempotencyKey()).isEqualTo(IdempotencyKey.of("key-1"));
        assertThat(job.submission().contentSha256()).isEqualTo(CONTENT_SHA256);
        assertThat(job.state().cancellation()).isEqualTo(ContractImportCancellation.of(LATER, CANCELLER));
    }

    private static ContractImportJob reconstituteWith(
            final ContractImportJobState.Status status,
            final ContractImportTermination termination,
            final ContractImportCancellation cancellation
    ) {
        return ContractImportJob.reconstituteFromDatabase(UUID.randomUUID(),
                ContractImportJobFixtures.submission(NOW),
                status,
                PROGRESS,
                status == ContractImportJobState.Status.RUNNING ? LEASE : null,
                termination,
                cancellation);
    }

    private static ContractImportTermination consistentTerminationOf(final ContractImportJobState.Status status) {
        return switch (status) {
            case PENDING, RUNNING -> null;
            case COMPLETED, CANCELLED -> ContractImportTermination.ended(NOW);
            case FAILED -> ContractImportTermination.failed(REASON, NOW);
        };
    }

    @Test
    @DisplayName(
        "GIVEN a failed status with a termination without failure WHEN reconstituteFromDatabase THEN it is rejected")
    void shouldRejectFailedStatusWithEndedTermination() {
        final ContractImportTermination ended = ContractImportTermination.ended(NOW);

        assertThatThrownBy(() -> reconstituteWith(ContractImportJobState.Status.FAILED, ended, null))
                .isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName(
        "GIVEN a failed status with a failed termination WHEN reconstituteFromDatabase THEN the job keeps its failure")
    void shouldReconstituteFailedJob() {
        final ContractImportTermination failed = ContractImportTermination.failed(REASON, NOW);

        final ContractImportJob job = reconstituteWith(ContractImportJobState.Status.FAILED, failed, null);

        assertThat(job.status()).isEqualTo(ContractImportJobState.Status.FAILED);
        assertThat(job.state().termination()).isEqualTo(failed);
    }

    @Test
    @DisplayName("GIVEN a completed status with a failed termination WHEN reconstituteFromDatabase THEN it is rejected")
    void shouldRejectCompletedStatusWithFailedTermination() {
        final ContractImportTermination failed = ContractImportTermination.failed(REASON, NOW);

        assertThatThrownBy(() -> reconstituteWith(ContractImportJobState.Status.COMPLETED, failed, null))
                .isInstanceOf(InvalidDomainObjectError.class);
    }

    @ParameterizedTest
    @EnumSource(value = ContractImportJobState.Status.class, names = {"COMPLETED", "FAILED"})
    @DisplayName("GIVEN a finished status without termination WHEN reconstituteFromDatabase THEN it is rejected")
    void shouldRejectFinishedStatusWithoutTermination(final ContractImportJobState.Status status) {
        assertThatThrownBy(() -> reconstituteWith(status, null, null))
                .isInstanceOf(InvalidDomainObjectError.class);
    }

    @ParameterizedTest
    @EnumSource(value = ContractImportJobState.Status.class, names = {"PENDING", "RUNNING"})
    @DisplayName("GIVEN an unfinished status with a termination WHEN reconstituteFromDatabase THEN it is rejected")
    void shouldRejectUnfinishedStatusWithTermination(final ContractImportJobState.Status status) {
        final ContractImportTermination ended = ContractImportTermination.ended(NOW);

        assertThatThrownBy(() -> reconstituteWith(status, ended, null))
                .isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName("GIVEN a cancelled status without cancellation WHEN reconstituteFromDatabase THEN it is rejected")
    void shouldRejectCancelledStatusWithoutCancellation() {
        final ContractImportTermination ended = ContractImportTermination.ended(NOW);

        assertThatThrownBy(() -> reconstituteWith(ContractImportJobState.Status.CANCELLED, ended, null))
                .isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName("GIVEN a cancelled status without termination WHEN reconstituteFromDatabase THEN it is rejected")
    void shouldRejectCancelledStatusWithoutTermination() {
        final ContractImportCancellation cancellation = ContractImportCancellation.of(LATER, CANCELLER);

        assertThatThrownBy(() -> reconstituteWith(ContractImportJobState.Status.CANCELLED, null, cancellation))
                .isInstanceOf(InvalidDomainObjectError.class);
    }

    @ParameterizedTest
    @EnumSource(value = ContractImportJobState.Status.class, names = "CANCELLED", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName(
        "GIVEN a status other than cancelled with a cancellation WHEN reconstituteFromDatabase THEN it is rejected")
    void shouldRejectCancellationOfJobNotCancelled(final ContractImportJobState.Status status) {
        final ContractImportTermination termination = consistentTerminationOf(status);
        final ContractImportCancellation cancellation = ContractImportCancellation.of(LATER, CANCELLER);

        assertThatThrownBy(() -> reconstituteWith(status, termination, cancellation))
                .isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName("GIVEN a running status without lease WHEN reconstituteFromDatabase THEN it is rejected")
    void shouldRejectRunningStatusWithoutLease() {
        assertThatThrownBy(() -> ContractImportJob.reconstituteFromDatabase(UUID.randomUUID(),
                ContractImportJobFixtures.submission(NOW),
                ContractImportJobState.Status.RUNNING,
                PROGRESS,
                null,
                null,
                null))
                .isInstanceOf(InvalidDomainObjectError.class);
    }

    @ParameterizedTest
    @EnumSource(value = ContractImportJobState.Status.class, names = "RUNNING", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("GIVEN a status other than running with a lease WHEN reconstituteFromDatabase THEN it is rejected")
    void shouldRejectLeaseOfJobNotRunning(final ContractImportJobState.Status status) {
        final ContractImportTermination termination = consistentTerminationOf(status);
        final ContractImportCancellation cancellation = status == ContractImportJobState.Status.CANCELLED ? ContractImportCancellation
                .of(LATER, CANCELLER) : null;

        assertThatThrownBy(() -> ContractImportJob.reconstituteFromDatabase(UUID.randomUUID(),
                ContractImportJobFixtures.submission(NOW),
                status,
                PROGRESS,
                LEASE,
                termination,
                cancellation))
                .isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName("GIVEN a job cancelled after its failure WHEN reconstituteFromDatabase THEN it keeps its failure")
    void shouldReconstituteJobCancelledAfterFailure() {
        final ContractImportJob job = reconstituteWith(ContractImportJobState.Status.CANCELLED,
                ContractImportTermination.failed(REASON, NOW),
                ContractImportCancellation.of(LATER, CANCELLER));

        assertThat(failureReasonOf(job)).isEqualTo(REASON);
    }

    @Test
    @DisplayName("GIVEN a legacy job without submitter WHEN reconstituteFromDatabase THEN it is accepted")
    void shouldReconstituteLegacyJobWithoutSubmitter() {
        final ContractImportSubmission submission = ContractImportSubmission.reconstituteFromDatabase(FILE_NAME,
                null,
                null,
                CONTENT_SHA256,
                NOW);

        final ContractImportJob job = reconstitute(UUID.randomUUID(),
                submission,
                ContractImportJobState.Status.RUNNING,
                PROGRESS);

        assertThat(job.submission().submittedBy()).isNull();
    }

    @Test
    @DisplayName("GIVEN a null mandatory field WHEN reconstituteFromDatabase THEN it is rejected")
    void shouldRejectNullFieldsOnReconstitution() {
        final ContractImportSubmission submission = ContractImportJobFixtures.submission(NOW);
        final UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> reconstitute(null, submission, ContractImportJobState.Status.RUNNING, PROGRESS))
                .isInstanceOf(InvalidDomainObjectError.class);
        assertThatThrownBy(() -> reconstitute(id, null, ContractImportJobState.Status.RUNNING, PROGRESS)).isInstanceOf(
                InvalidDomainObjectError.class);
        assertThatThrownBy(() -> reconstitute(id, submission, null, PROGRESS)).isInstanceOf(
                InvalidDomainObjectError.class);
        assertThatThrownBy(() -> reconstitute(id, submission, ContractImportJobState.Status.RUNNING, null))
                .isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName(
        "GIVEN a submission without file name, fingerprint or date WHEN reconstituteFromDatabase THEN it is rejected")
    void shouldRejectIncompleteSubmissionOnReconstitution() {
        assertThatThrownBy(() -> ContractImportSubmission.reconstituteFromDatabase(null,
                SUBMITTER,
                null,
                CONTENT_SHA256,
                NOW)).isInstanceOf(InvalidDomainObjectError.class);
        assertThatThrownBy(() -> ContractImportSubmission.reconstituteFromDatabase(FILE_NAME,
                SUBMITTER,
                null,
                null,
                NOW)).isInstanceOf(InvalidDomainObjectError.class);
        assertThatThrownBy(() -> ContractImportSubmission.reconstituteFromDatabase(FILE_NAME,
                SUBMITTER,
                null,
                CONTENT_SHA256,
                null)).isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName(
        "GIVEN a termination or cancellation without mandatory field WHEN reconstituteFromDatabase THEN it is rejected")
    void shouldRejectIncompleteTerminationAndCancellationOnReconstitution() {
        assertThatThrownBy(() -> ContractImportTermination.reconstituteFromDatabase(null, REASON)).isInstanceOf(
                InvalidDomainObjectError.class);
        assertThatThrownBy(() -> ContractImportCancellation.reconstituteFromDatabase(null, CANCELLER)).isInstanceOf(
                InvalidDomainObjectError.class);
        assertThatThrownBy(() -> ContractImportCancellation.reconstituteFromDatabase(LATER, null)).isInstanceOf(
                InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName(
        "GIVEN a termination without failure reason WHEN reconstituteFromDatabase THEN the job ended without failure")
    void shouldReconstituteEndedTermination() {
        assertThat(ContractImportTermination.reconstituteFromDatabase(NOW, null)).isEqualTo(ContractImportTermination
                .ended(NOW));
    }

    @Test
    @DisplayName(
        "GIVEN a termination with a failure reason WHEN reconstituteFromDatabase THEN the job failed for this reason")
    void shouldReconstituteFailedTermination() {
        assertThat(ContractImportTermination.reconstituteFromDatabase(NOW, REASON)).isEqualTo(ContractImportTermination
                .failed(REASON, NOW));
    }

    @Test
    @DisplayName("GIVEN a pending job WHEN start THEN it runs under a lease")
    void shouldStart() {
        final ContractImportJob job = pendingJob(3).start(LEASE);

        assertThat(job.status()).isEqualTo(ContractImportJobState.Status.RUNNING);
        assertThat(job.state().leaseUntil()).isEqualTo(LEASE);
    }

    @Test
    @DisplayName(
        "GIVEN a running job whose lease expired WHEN start THEN another execution takes it over with a new lease")
    void shouldRestartRunningJob() {
        final Instant newLease = LEASE.plusSeconds(600);

        final ContractImportJob job = pendingJob(3).start(LEASE).recordBatch(1, 0, LEASE).start(newLease);

        assertThat(job.status()).isEqualTo(ContractImportJobState.Status.RUNNING);
        assertThat(job.state().leaseUntil()).isEqualTo(newLease);
        assertThat(job.progress().processedLines()).isEqualTo(1);
    }

    @Test
    @DisplayName("GIVEN a finished job WHEN start THEN it is rejected")
    void shouldRejectStartOfFinishedJob() {
        final ContractImportJob completed = completedJob();
        final ContractImportJob failed = pendingJob(3).start(LEASE).fail(REASON, NOW);
        final ContractImportJob cancelled = pendingJob(3).cancel(LATER, CANCELLER);

        for (final ContractImportJob job : new ContractImportJob[]{completed, failed, cancelled}) {
            assertThatThrownBy(() -> job.start(LEASE)).isInstanceOf(
                    ContractImportJobTransitionNotAllowedException.class);
        }
    }

    @Test
    @DisplayName("GIVEN a negative count WHEN a batch is recorded THEN it is rejected")
    void shouldRejectNegativeBatch() {
        final ContractImportJob job = pendingJob(3).start(LEASE).recordBatch(1, 1, LEASE);

        assertThatThrownBy(() -> job.recordBatch(-1, 0, LEASE)).isInstanceOf(InvalidDomainObjectError.class);
        assertThatThrownBy(() -> job.recordBatch(0, -1, LEASE)).isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName("GIVEN a running job WHEN a batch is recorded THEN counters grow and the lease is renewed")
    void shouldRecordBatch() {
        final Instant renewedLease = LEASE.plusSeconds(60);

        final ContractImportJob job = pendingJob(3).start(LEASE).recordBatch(1, 1, renewedLease);

        assertThat(job.status()).isEqualTo(ContractImportJobState.Status.RUNNING);
        assertThat(job.progress().processedLines()).isEqualTo(2);
        assertThat(job.progress().importedCount()).isEqualTo(1);
        assertThat(job.progress().rejectedCount()).isEqualTo(1);
        assertThat(job.state().leaseUntil()).isEqualTo(renewedLease);
        assertThat(job.state().termination()).isNull();
    }

    @Test
    @DisplayName("GIVEN the last batch WHEN it is recorded THEN the job keeps running until the end of the execution")
    void shouldKeepRunningAfterLastBatch() {
        final ContractImportJob job = pendingJob(2).start(LEASE).recordBatch(1, 1, LEASE);

        assertThat(job.status()).isEqualTo(ContractImportJobState.Status.RUNNING);
        assertThat(job.state().termination()).isNull();
    }

    @Test
    @DisplayName("GIVEN an empty batch WHEN it is recorded THEN only the lease changes")
    void shouldRecordEmptyBatch() {
        final Instant renewedLease = LEASE.plusSeconds(60);

        final ContractImportJob job = pendingJob(2).start(LEASE).recordBatch(0, 0, renewedLease);

        assertThat(job.progress().processedLines()).isZero();
        assertThat(job.state().leaseUntil()).isEqualTo(renewedLease);
    }

    @Test
    @DisplayName("GIVEN a batch beyond the total WHEN it is recorded THEN it is rejected")
    void shouldRejectBatchBeyondTotal() {
        final ContractImportJob job = pendingJob(1).start(LEASE);

        assertThatThrownBy(() -> job.recordBatch(2, 0, LEASE)).isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName("GIVEN every line processed WHEN finish THEN the job completes and releases its lease")
    void shouldCompleteOnFinish() {
        final ContractImportJob job = pendingJob(2).start(LEASE).recordBatch(1, 1, LEASE).finish(NOW);

        assertThat(job.status()).isEqualTo(ContractImportJobState.Status.COMPLETED);
        assertThat(finishedAtOf(job)).isEqualTo(NOW);
        assertThat(job.state().leaseUntil()).isNull();
        assertThat(failureReasonOf(job)).isNull();
    }

    @Test
    @DisplayName("GIVEN missing lines WHEN finish THEN the job fails as incomplete")
    void shouldFailOnFinishWithMissingLines() {
        final ContractImportJob job = pendingJob(2).start(LEASE).recordBatch(1, 0, LEASE).finish(NOW);

        assertThat(job.status()).isEqualTo(ContractImportJobState.Status.FAILED);
        assertThat(failureReasonOf(job)).isEqualTo(ContractImportFailureReason.INCOMPLETE_PROCESSING);
        assertThat(finishedAtOf(job)).isEqualTo(NOW);
    }

    @Test
    @DisplayName("GIVEN a running job WHEN release THEN it waits again, keeps its counters and has no lease")
    void shouldRelease() {
        final ContractImportJob job = pendingJob(3).start(LEASE).recordBatch(1, 0, LEASE).release();

        assertThat(job.status()).isEqualTo(ContractImportJobState.Status.PENDING);
        assertThat(job.progress().processedLines()).isEqualTo(1);
        assertThat(job.state().leaseUntil()).isNull();
        assertThat(job.state().termination()).isNull();
    }

    @Test
    @DisplayName(
        "GIVEN a running job WHEN fail THEN it stops, keeps its counters, gives its reason and releases its lease")
    void shouldFail() {
        final ContractImportJob job = pendingJob(3).start(LEASE).recordBatch(1, 0, LEASE).fail(REASON, NOW);

        assertThat(job.status()).isEqualTo(ContractImportJobState.Status.FAILED);
        assertThat(job.progress().importedCount()).isEqualTo(1);
        assertThat(finishedAtOf(job)).isEqualTo(NOW);
        assertThat(job.state().leaseUntil()).isNull();
        assertThat(failureReasonOf(job)).isEqualTo(REASON);
    }

    @Test
    @DisplayName("GIVEN a job that is not running WHEN a batch is recorded THEN it is rejected")
    void shouldRejectBatchWhenNotRunning() {
        final ContractImportJob pending = pendingJob(3);
        final ContractImportJob failed = pendingJob(3).start(LEASE).fail(REASON, NOW);
        final ContractImportJob completed = completedJob();

        assertThatThrownBy(() -> pending.recordBatch(1, 0, LEASE)).isInstanceOf(
                ContractImportJobTransitionNotAllowedException.class);
        assertThatThrownBy(() -> failed.recordBatch(1, 0, LEASE)).isInstanceOf(
                ContractImportJobTransitionNotAllowedException.class);
        assertThatThrownBy(() -> completed.recordBatch(1, 0, LEASE)).isInstanceOf(
                ContractImportJobTransitionNotAllowedException.class);
    }

    @Test
    @DisplayName("GIVEN a job that is not running WHEN fail, finish or release THEN it is rejected")
    void shouldRejectEndOfRunWhenNotRunning() {
        final ContractImportJob pending = pendingJob(3);
        final ContractImportJob failed = pendingJob(3).start(LEASE).fail(REASON, NOW);
        final ContractImportJob completed = completedJob();

        for (final ContractImportJob job : new ContractImportJob[]{pending, failed, completed}) {
            assertThatThrownBy(() -> job.fail(REASON, NOW)).isInstanceOf(
                    ContractImportJobTransitionNotAllowedException.class);
            assertThatThrownBy(() -> job.finish(NOW)).isInstanceOf(
                    ContractImportJobTransitionNotAllowedException.class);
            assertThatThrownBy(job::release).isInstanceOf(ContractImportJobTransitionNotAllowedException.class);
        }
    }

    @Test
    @DisplayName("GIVEN a running job WHEN fail without a reason THEN it is rejected")
    void shouldRejectFailWithoutReason() {
        final ContractImportJob job = pendingJob(3).start(LEASE);

        assertThatThrownBy(() -> job.fail(null, NOW)).isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName("GIVEN a pending job WHEN cancel THEN it is cancelled by the counselor and ends now")
    void shouldCancelPendingJob() {
        final ContractImportJob job = pendingJob(3).cancel(LATER, CANCELLER);

        assertThat(job.status()).isEqualTo(ContractImportJobState.Status.CANCELLED);
        assertThat(job.state().cancellation()).isEqualTo(ContractImportCancellation.of(LATER, CANCELLER));
        assertThat(finishedAtOf(job)).isEqualTo(LATER);
        assertThat(job.status()).isEqualTo(ContractImportJobState.Status.CANCELLED);
    }

    @Test
    @DisplayName("GIVEN a running job WHEN cancel THEN it keeps its counters and releases its lease")
    void shouldCancelRunningJob() {
        final ContractImportJob job = pendingJob(3).start(LEASE).recordBatch(1, 1, LEASE).cancel(LATER, CANCELLER);

        assertThat(job.status()).isEqualTo(ContractImportJobState.Status.CANCELLED);
        assertThat(job.progress().importedCount()).isEqualTo(1);
        assertThat(job.progress().rejectedCount()).isEqualTo(1);
        assertThat(job.state().leaseUntil()).isNull();
    }

    @Test
    @DisplayName("GIVEN a finished job WHEN cancel THEN it keeps its end date and its failure reason")
    void shouldCancelFinishedJob() {
        final ContractImportJob completed = completedJob().cancel(LATER, CANCELLER);
        final ContractImportJob failed = pendingJob(3).start(LEASE).fail(REASON, NOW).cancel(LATER, CANCELLER);

        assertThat(completed.status()).isEqualTo(ContractImportJobState.Status.CANCELLED);
        assertThat(finishedAtOf(completed)).isEqualTo(NOW);
        assertThat(failed.status()).isEqualTo(ContractImportJobState.Status.CANCELLED);
        assertThat(finishedAtOf(failed)).isEqualTo(NOW);
        assertThat(failureReasonOf(failed)).isEqualTo(REASON);
    }

    @Test
    @DisplayName("GIVEN a cancelled job WHEN cancel again THEN nothing changes")
    void shouldIgnoreSecondCancellation() {
        final ContractImportJob cancelled = pendingJob(3).cancel(LATER, CANCELLER);

        assertThat(cancelled.cancel(LATER.plusHours(1), SUBMITTER)).isSameAs(cancelled);
    }

    @Test
    @DisplayName("GIVEN a cancellation without date or counselor WHEN cancel THEN it is rejected")
    void shouldRejectIncompleteCancellation() {
        final ContractImportJob job = pendingJob(3);

        assertThatThrownBy(() -> job.cancel(null, CANCELLER)).isInstanceOf(InvalidDomainObjectError.class);
        assertThatThrownBy(() -> job.cancel(LATER, null)).isInstanceOf(InvalidDomainObjectError.class);
        assertThatThrownBy(() -> job.cancel(LATER, " ")).isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName(
        "GIVEN a job cancelled while it runs WHEN its batch is recorded THEN the batch is refused as cancelled")
    void shouldRefuseBatchOfCancelledJob() {
        final ContractImportJob job = pendingJob(3).start(LEASE).cancel(LATER, CANCELLER);

        assertThatThrownBy(() -> job.recordBatch(1, 0, LEASE)).isInstanceOf(ContractImportJobCancelledException.class);
    }

    @Test
    @DisplayName("GIVEN a cancelled job WHEN its execution ends THEN finish, release and fail leave it cancelled")
    void shouldKeepCancelledJobWhenExecutionEnds() {
        final ContractImportJob job = pendingJob(3).start(LEASE).cancel(LATER, CANCELLER);

        assertThat(job.finish(NOW)).isSameAs(job);
        assertThat(job.release()).isSameAs(job);
        assertThat(job.fail(REASON, NOW)).isSameAs(job);
    }

    @Test
    @DisplayName("GIVEN two jobs reconstituted from the same data WHEN comparing them THEN they are equal")
    void shouldCompareJobsByValue() {
        final UUID id = UUID.randomUUID();
        final ContractImportSubmission submission = ContractImportJobFixtures.submission(NOW);

        final ContractImportJob job = reconstitute(id, submission, ContractImportJobState.Status.RUNNING, PROGRESS);
        final ContractImportJob same = reconstitute(id, submission, ContractImportJobState.Status.RUNNING, PROGRESS);

        assertThat(same).isEqualTo(job).hasSameHashCodeAs(job);
        assertThat(job.release()).isNotEqualTo(job);
    }
}
