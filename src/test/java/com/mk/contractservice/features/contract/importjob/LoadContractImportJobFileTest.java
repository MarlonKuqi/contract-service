package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoadContractImportJobFile Handler")
class LoadContractImportJobFileTest {

    private static final UUID JOB_ID = UUID.randomUUID();

    @Mock
    private ContractImportJobRepository jobRepository;

    @Test
    @DisplayName("GIVEN a job with a stored file WHEN loading THEN the content of the file is returned")
    void shouldReturnStoredFile() {
        final byte[] content = "clientId;type;franchise;startDate;endDate;costAmount\n".getBytes(
                StandardCharsets.UTF_8);
        when(jobRepository.loadFile(JOB_ID)).thenReturn(content);

        final byte[] loaded = new LoadContractImportJobFile.Handler(jobRepository)
                .execute(new LoadContractImportJobFile.Query(JOB_ID));

        assertThat(loaded).isSameAs(content);
    }

    @Test
    @DisplayName("GIVEN a job without stored file WHEN loading THEN the error of the port is propagated")
    void shouldPropagateMissingFile() {
        when(jobRepository.loadFile(JOB_ID)).thenThrow(new IllegalStateException("No file stored"));
        final LoadContractImportJobFile handler = new LoadContractImportJobFile.Handler(jobRepository);
        final LoadContractImportJobFile.Query query = new LoadContractImportJobFile.Query(JOB_ID);

        assertThatThrownBy(() -> handler.execute(query)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("GIVEN a null job id WHEN building Query THEN it is rejected")
    void shouldRejectNullJobId() {
        assertThatThrownBy(() -> new LoadContractImportJobFile.Query(null)).isInstanceOf(NullPointerException.class);
    }
}
