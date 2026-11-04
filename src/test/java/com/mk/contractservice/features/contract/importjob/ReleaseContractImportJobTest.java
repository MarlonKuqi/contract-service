package com.mk.contractservice.features.contract.importjob;

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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(jobRepository.getById(JOB_ID)).thenReturn(ContractImportJob.reconstituteFromDatabase(
                JOB_ID, "contracts.csv", ContractImportJobStatus.RUNNING, 2, 1, 1, 0,
                Instant.parse("2026-11-03T09:05:00Z"), LocalDateTime.of(2026, 11, 3, 10, 0), null, null));

        new ReleaseContractImportJob.Handler(jobRepository).execute(new ReleaseContractImportJob.Command(JOB_ID));

        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo(ContractImportJobStatus.PENDING);
        assertThat(jobCaptor.getValue().getLeaseUntil()).isNull();
    }
}
