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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FailContractImportJob Handler")
class FailContractImportJobTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-03T09:00:00Z"), ZoneId.of("Europe/Zurich"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final FailContractImportJob.Command COMMAND =
            new FailContractImportJob.Command(JOB_ID, ContractImportFailureReason.TOO_MANY_ATTEMPTS);

    @Mock
    private ContractImportJobRepository jobRepository;
    @Captor
    private ArgumentCaptor<ContractImportJob> jobCaptor;

    private ContractImportJob jobWithStatus(final ContractImportJobStatus status) {
        return ContractImportJob.reconstituteFromDatabase(JOB_ID, "contracts.csv", status, 2, 1, 1, 0, null, NOW, null, null);
    }

    @Test
    @DisplayName("GIVEN a running job WHEN failing it THEN the failed job is saved with its reason")
    void shouldSaveFailedJob() {
        when(jobRepository.getById(JOB_ID)).thenReturn(jobWithStatus(ContractImportJobStatus.RUNNING));

        new FailContractImportJob.Handler(jobRepository, CLOCK).execute(COMMAND);

        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo(ContractImportJobStatus.FAILED);
        assertThat(jobCaptor.getValue().getFinishedAt()).isEqualTo(NOW);
        assertThat(jobCaptor.getValue().getFailureReason()).isEqualTo(ContractImportFailureReason.TOO_MANY_ATTEMPTS);
    }

    @Test
    @DisplayName("GIVEN a completed job WHEN failing it THEN nothing is saved")
    void shouldNotFailCompletedJob() {
        when(jobRepository.getById(JOB_ID)).thenReturn(jobWithStatus(ContractImportJobStatus.COMPLETED));
        final FailContractImportJob handler = new FailContractImportJob.Handler(jobRepository, CLOCK);

        assertThatThrownBy(() -> handler.execute(COMMAND)).isInstanceOf(IllegalStateException.class);

        verify(jobRepository, never()).save(any());
    }
}
