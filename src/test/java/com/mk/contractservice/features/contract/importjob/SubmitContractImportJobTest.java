package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.exception.InvalidImportFileException;
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

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmitContractImportJob Handler")
class SubmitContractImportJobTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-03T09:00:00Z"), ZoneId.of("Europe/Zurich"));
    private static final String HEADER = "clientId;startDate;endDate;costAmount\n";

    @Mock
    private ContractImportJobRepository jobRepository;
    @Captor
    private ArgumentCaptor<ContractImportJob> jobCaptor;

    private static byte[] fileOf(final int lines) {
        return (HEADER + "line\n".repeat(lines)).getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("GIVEN a valid file WHEN submitting THEN a pending job is stored with the file")
    void shouldStorePendingJob() {
        final byte[] content = fileOf(2);
        when(jobRepository.insert(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        new SubmitContractImportJob.Handler(jobRepository, CLOCK)
                .execute(new SubmitContractImportJob.Command("contracts.csv", content));

        verify(jobRepository).insert(jobCaptor.capture(), same(content));
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo(ContractImportJobStatus.PENDING);
        assertThat(jobCaptor.getValue().getTotalLines()).isEqualTo(2);
        assertThat(jobCaptor.getValue().getCreatedAt()).isEqualTo(LocalDateTime.now(CLOCK));
    }

    @Test
    @DisplayName("GIVEN exactly the maximum of lines WHEN building Command THEN it is accepted")
    void shouldAcceptMaximumOfLines() {
        assertThatCode(() -> new SubmitContractImportJob.Command("contracts.csv", fileOf(SubmitContractImportJob.MAX_LINES)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GIVEN too many lines WHEN building Command THEN it is rejected")
    void shouldRejectTooManyLines() {
        final byte[] content = fileOf(SubmitContractImportJob.MAX_LINES + 1);

        assertThatThrownBy(() -> new SubmitContractImportJob.Command("contracts.csv", content))
                .isInstanceOf(InvalidImportFileException.class)
                .hasMessageContaining(String.valueOf(SubmitContractImportJob.MAX_LINES));
    }

    @Test
    @DisplayName("GIVEN a file name too long WHEN building Command THEN it is rejected")
    void shouldRejectFileNameTooLong() {
        final String fileName = "a".repeat(SubmitContractImportJob.MAX_FILE_NAME_LENGTH + 1);

        assertThatThrownBy(() -> new SubmitContractImportJob.Command(fileName, fileOf(1)))
                .isInstanceOf(InvalidImportFileException.class);
    }
}
