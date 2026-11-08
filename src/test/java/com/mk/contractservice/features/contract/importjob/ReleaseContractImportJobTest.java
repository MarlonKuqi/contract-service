package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobFixtures;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReleaseContractImportJob Handler")
class ReleaseContractImportJobTest {

    private static final UUID JOB_ID = UUID.randomUUID();

    @Mock
    private ContractImportJobRepository jobRepository;
    @Captor
    private ArgumentCaptor<ContractImportJob> jobCaptor;

    @Test
    @DisplayName("GIVEN a running job WHEN releasing it THEN it is saved as pending without a lease")
    void shouldReleaseJob() {
        when(jobRepository.getById(JOB_ID)).thenReturn(ContractImportJobFixtures.runningJob(
                JOB_ID,
                2,
                1,
                0,
                Instant.parse("2026-11-03T09:05:00Z"),
                LocalDateTime.of(2026, 11, 3, 10, 0)));

        new ReleaseContractImportJob.Handler(jobRepository).execute(new ReleaseContractImportJob.Command(JOB_ID));

        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().status()).isEqualTo(ContractImportJobState.Status.PENDING);
        assertThat(jobCaptor.getValue().state().leaseUntil()).isNull();
    }

    @Test
    @DisplayName("GIVEN a job cancelled while it ran WHEN releasing it THEN it stays cancelled and nothing is saved")
    void shouldKeepCancelledJob() {
        when(jobRepository.getById(JOB_ID)).thenReturn(ContractImportJobFixtures.cancelledJob(JOB_ID));

        new ReleaseContractImportJob.Handler(jobRepository).execute(new ReleaseContractImportJob.Command(JOB_ID));

        verify(jobRepository, never()).save(any());
    }
}
