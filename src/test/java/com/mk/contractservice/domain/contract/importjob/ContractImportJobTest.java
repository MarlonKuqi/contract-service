package com.mk.contractservice.domain.contract.importjob;

import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ContractImportJob")
class ContractImportJobTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 3, 10, 0);
    private static final Instant LEASE = Instant.parse("2026-11-03T09:05:00Z");
    private static final ContractImportFailureReason REASON = ContractImportFailureReason.UNEXPECTED_ERROR;

    private ContractImportJob pendingJob(final int totalLines) {
        return ContractImportJob.of("contracts.csv", totalLines, NOW);
    }

    private ContractImportJob completedJob() {
        return pendingJob(1).start(LEASE).recordBatch(1, 0, LEASE).finish(NOW);
    }

    @Test
    @DisplayName("GIVEN valid data WHEN of THEN job is pending with empty counters")
    void shouldCreatePendingJob() {
        final ContractImportJob job = pendingJob(3);

        assertThat(job.getId()).isNull();
        assertThat(job.getStatus()).isEqualTo(ContractImportJobStatus.PENDING);
        assertThat(job.getFileName()).isEqualTo("contracts.csv");
        assertThat(job.getTotalLines()).isEqualTo(3);
        assertThat(job.getProcessedLines()).isZero();
        assertThat(job.getImportedCount()).isZero();
        assertThat(job.getRejectedCount()).isZero();
        assertThat(job.getCreatedAt()).isEqualTo(NOW);
        assertThat(job.getFinishedAt()).isNull();
        assertThat(job.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("GIVEN a null field WHEN of THEN it is rejected")
    void shouldRejectNullFields() {
        assertThatThrownBy(() -> ContractImportJob.of(null, 3, NOW)).isInstanceOf(InvalidDomainObjectError.class);
        assertThatThrownBy(() -> ContractImportJob.of("contracts.csv", 3, null)).isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName("GIVEN database values WHEN reconstituteFromDatabase THEN job keeps them")
    void shouldReconstitute() {
        final UUID id = UUID.randomUUID();

        final ContractImportJob job = ContractImportJob.reconstituteFromDatabase(
                id, "contracts.csv", ContractImportJobStatus.FAILED, 3, 1, 1, 0, LEASE, NOW, NOW, REASON);

        assertThat(job.getId()).isEqualTo(id);
        assertThat(job.getStatus()).isEqualTo(ContractImportJobStatus.FAILED);
        assertThat(job.getProcessedLines()).isEqualTo(1);
        assertThat(job.getLeaseUntil()).isEqualTo(LEASE);
        assertThat(job.getFailureReason()).isEqualTo(REASON);
    }

    @Test
    @DisplayName("GIVEN a null id WHEN reconstituteFromDatabase THEN it is rejected")
    void shouldRejectNullIdOnReconstitution() {
        assertThatThrownBy(() -> ContractImportJob.reconstituteFromDatabase(
                null, "contracts.csv", ContractImportJobStatus.RUNNING, 3, 1, 1, 0, LEASE, NOW, null, null))
                .isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName("GIVEN a pending job WHEN start THEN it runs under a lease")
    void shouldStart() {
        final ContractImportJob job = pendingJob(3).start(LEASE);

        assertThat(job.getStatus()).isEqualTo(ContractImportJobStatus.RUNNING);
        assertThat(job.getLeaseUntil()).isEqualTo(LEASE);
    }

    @Test
    @DisplayName("GIVEN a running job WHEN a batch is recorded THEN counters grow and the lease is renewed")
    void shouldRecordBatch() {
        final Instant renewedLease = LEASE.plusSeconds(60);

        final ContractImportJob job = pendingJob(3).start(LEASE).recordBatch(1, 1, renewedLease);

        assertThat(job.getStatus()).isEqualTo(ContractImportJobStatus.RUNNING);
        assertThat(job.getProcessedLines()).isEqualTo(2);
        assertThat(job.getImportedCount()).isEqualTo(1);
        assertThat(job.getRejectedCount()).isEqualTo(1);
        assertThat(job.getLeaseUntil()).isEqualTo(renewedLease);
        assertThat(job.getFinishedAt()).isNull();
    }

    @Test
    @DisplayName("GIVEN the last batch WHEN it is recorded THEN the job keeps running until the end of the execution")
    void shouldKeepRunningAfterLastBatch() {
        final ContractImportJob job = pendingJob(2).start(LEASE).recordBatch(1, 1, LEASE);

        assertThat(job.getStatus()).isEqualTo(ContractImportJobStatus.RUNNING);
        assertThat(job.getFinishedAt()).isNull();
    }

    @Test
    @DisplayName("GIVEN an empty batch WHEN it is recorded THEN only the lease changes")
    void shouldRecordEmptyBatch() {
        final Instant renewedLease = LEASE.plusSeconds(60);

        final ContractImportJob job = pendingJob(2).start(LEASE).recordBatch(0, 0, renewedLease);

        assertThat(job.getProcessedLines()).isZero();
        assertThat(job.getLeaseUntil()).isEqualTo(renewedLease);
    }

    @Test
    @DisplayName("GIVEN a batch beyond the total WHEN it is recorded THEN it is rejected")
    void shouldRejectBatchBeyondTotal() {
        final ContractImportJob job = pendingJob(1).start(LEASE);

        assertThatThrownBy(() -> job.recordBatch(2, 0, LEASE)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("GIVEN every line processed WHEN finish THEN the job completes and releases its lease")
    void shouldCompleteOnFinish() {
        final ContractImportJob job = pendingJob(2).start(LEASE).recordBatch(1, 1, LEASE).finish(NOW);

        assertThat(job.getStatus()).isEqualTo(ContractImportJobStatus.COMPLETED);
        assertThat(job.getFinishedAt()).isEqualTo(NOW);
        assertThat(job.getLeaseUntil()).isNull();
        assertThat(job.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("GIVEN missing lines WHEN finish THEN the job fails as incomplete")
    void shouldFailOnFinishWithMissingLines() {
        final ContractImportJob job = pendingJob(2).start(LEASE).recordBatch(1, 0, LEASE).finish(NOW);

        assertThat(job.getStatus()).isEqualTo(ContractImportJobStatus.FAILED);
        assertThat(job.getFailureReason()).isEqualTo(ContractImportFailureReason.INCOMPLETE_PROCESSING);
        assertThat(job.getFinishedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("GIVEN a running job WHEN release THEN it waits again, keeps its counters and has no lease")
    void shouldRelease() {
        final ContractImportJob job = pendingJob(3).start(LEASE).recordBatch(1, 0, LEASE).release();

        assertThat(job.getStatus()).isEqualTo(ContractImportJobStatus.PENDING);
        assertThat(job.getProcessedLines()).isEqualTo(1);
        assertThat(job.getLeaseUntil()).isNull();
        assertThat(job.getFinishedAt()).isNull();
    }

    @Test
    @DisplayName("GIVEN a running job WHEN fail THEN it stops, keeps its counters, gives its reason and releases its lease")
    void shouldFail() {
        final ContractImportJob job = pendingJob(3).start(LEASE).recordBatch(1, 0, LEASE).fail(REASON, NOW);

        assertThat(job.getStatus()).isEqualTo(ContractImportJobStatus.FAILED);
        assertThat(job.getImportedCount()).isEqualTo(1);
        assertThat(job.getFinishedAt()).isEqualTo(NOW);
        assertThat(job.getLeaseUntil()).isNull();
        assertThat(job.getFailureReason()).isEqualTo(REASON);
    }

    @Test
    @DisplayName("GIVEN a job that is not running WHEN a batch is recorded THEN it is rejected")
    void shouldRejectBatchWhenNotRunning() {
        final ContractImportJob pending = pendingJob(3);
        final ContractImportJob failed = pendingJob(3).start(LEASE).fail(REASON, NOW);
        final ContractImportJob completed = completedJob();

        assertThatThrownBy(() -> pending.recordBatch(1, 0, LEASE)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> failed.recordBatch(1, 0, LEASE)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> completed.recordBatch(1, 0, LEASE)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("GIVEN a job that is not running WHEN fail, finish or release THEN it is rejected")
    void shouldRejectEndOfRunWhenNotRunning() {
        final ContractImportJob pending = pendingJob(3);
        final ContractImportJob failed = pendingJob(3).start(LEASE).fail(REASON, NOW);
        final ContractImportJob completed = completedJob();

        for (final ContractImportJob job : new ContractImportJob[]{pending, failed, completed}) {
            assertThatThrownBy(() -> job.fail(REASON, NOW)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> job.finish(NOW)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(job::release).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    @DisplayName("GIVEN a running job WHEN fail without a reason THEN it is rejected")
    void shouldRejectFailWithoutReason() {
        final ContractImportJob job = pendingJob(3).start(LEASE);

        assertThatThrownBy(() -> job.fail(null, NOW)).isInstanceOf(InvalidDomainObjectError.class);
    }
}
