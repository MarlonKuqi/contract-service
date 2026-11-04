package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportFailureReason;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobStatus;
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
        return ContractImportJob.reconstituteFromDatabase(JOB_ID, "contracts.csv", ContractImportJobStatus.RUNNING,
                2, processedLines, processedLines, 0, CLOCK.instant(), NOW, null, null);
    }

    @Test
    @DisplayName("GIVEN every line processed WHEN finishing THEN the completed job is saved")
    void shouldCompleteJob() {
        when(jobRepository.getById(JOB_ID)).thenReturn(runningJob(2));

        new FinishContractImportJob.Handler(jobRepository, CLOCK).execute(new FinishContractImportJob.Command(JOB_ID));

        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo(ContractImportJobStatus.COMPLETED);
        assertThat(jobCaptor.getValue().getFinishedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("GIVEN missing lines WHEN finishing THEN the job is saved as failed")
    void shouldFailIncompleteJob() {
        when(jobRepository.getById(JOB_ID)).thenReturn(runningJob(1));

        new FinishContractImportJob.Handler(jobRepository, CLOCK).execute(new FinishContractImportJob.Command(JOB_ID));

        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo(ContractImportJobStatus.FAILED);
        assertThat(jobCaptor.getValue().getFailureReason()).isEqualTo(ContractImportFailureReason.INCOMPLETE_PROCESSING);
    }
}
