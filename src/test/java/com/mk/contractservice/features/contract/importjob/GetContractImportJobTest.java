package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.exception.ContractImportJobNotFoundException;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetContractImportJob Handler")
class GetContractImportJobTest {

    private static final UUID JOB_ID = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 3, 10, 0);

    @Mock
    private ContractImportJobRepository jobRepository;

    private GetContractImportJob handler;

    @BeforeEach
    void setUp() {
        handler = new GetContractImportJob.Handler(jobRepository);
    }

    @Test
    @DisplayName("GIVEN an existing job WHEN getting it THEN it is returned")
    void shouldReturnJob() {
        final ContractImportJob job = ContractImportJob.of("contracts.csv", 1, NOW);
        when(jobRepository.getById(JOB_ID)).thenReturn(job);

        assertThat(handler.execute(new GetContractImportJob.Query(JOB_ID))).isSameAs(job);
    }

    @Test
    @DisplayName("GIVEN an unknown job WHEN getting it THEN it is not found")
    void shouldPropagateNotFound() {
        when(jobRepository.getById(JOB_ID)).thenThrow(new ContractImportJobNotFoundException(JOB_ID));
        final GetContractImportJob.Query query = new GetContractImportJob.Query(JOB_ID);

        assertThatThrownBy(() -> handler.execute(query)).isInstanceOf(ContractImportJobNotFoundException.class);
    }

    @Test
    @DisplayName("GIVEN a null job id WHEN building Query THEN it is rejected")
    void shouldRejectNullJobId() {
        assertThatThrownBy(() -> new GetContractImportJob.Query(null)).isInstanceOf(NullPointerException.class);
    }
}
