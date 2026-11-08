package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportFailureReason;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobFixtures;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobState;
import com.mk.contractservice.domain.contract.importjob.ContractImportTermination;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FinishContractImportJob Handler")
class FinishContractImportJobTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-03T09:00:00Z"), ZoneId.of("Europe/Zurich"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final UUID JOB_ID = UUID.randomUUID();

    @Mock
    private ContractImportJobRepository jobRepository;
    @Captor
    private ArgumentCaptor<ContractImportJob> jobCaptor;

    private ContractImportJob runningJob(final int processedLines) {
        return ContractImportJobFixtures.runningJob(JOB_ID, 2, processedLines, 0, CLOCK.instant(), NOW);
    }

    @Test
    @DisplayName("GIVEN every line processed WHEN finishing THEN the completed job is saved")
    void shouldCompleteJob() {
        when(jobRepository.getById(JOB_ID)).thenReturn(runningJob(2));

        new FinishContractImportJob.Handler(jobRepository, CLOCK).execute(new FinishContractImportJob.Command(JOB_ID));

        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().status()).isEqualTo(ContractImportJobState.Status.COMPLETED);
        assertThat(jobCaptor.getValue().state().termination()).isEqualTo(ContractImportTermination.ended(NOW));
    }

    @Test
    @DisplayName("GIVEN missing lines WHEN finishing THEN the job is saved as failed")
    void shouldFailIncompleteJob() {
        when(jobRepository.getById(JOB_ID)).thenReturn(runningJob(1));

        new FinishContractImportJob.Handler(jobRepository, CLOCK).execute(new FinishContractImportJob.Command(JOB_ID));

        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().status()).isEqualTo(ContractImportJobState.Status.FAILED);
        assertThat(jobCaptor.getValue().state().termination()).isEqualTo(ContractImportTermination.failed(
                ContractImportFailureReason.INCOMPLETE_PROCESSING,
                NOW));
    }

    @Test
    @DisplayName("GIVEN a job cancelled while it ran WHEN finishing it THEN it stays cancelled and nothing is saved")
    void shouldKeepCancelledJob() {
        when(jobRepository.getById(JOB_ID)).thenReturn(ContractImportJobFixtures.cancelledJob(JOB_ID));

        new FinishContractImportJob.Handler(jobRepository, CLOCK).execute(new FinishContractImportJob.Command(JOB_ID));

        verify(jobRepository, never()).save(any());
    }
}
