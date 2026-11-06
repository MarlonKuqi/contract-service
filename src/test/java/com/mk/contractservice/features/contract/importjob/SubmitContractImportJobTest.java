package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobStatus;
import com.mk.contractservice.domain.contract.importjob.ContractImportProgress;
import com.mk.contractservice.domain.contract.importjob.ContractImportSubmission;
import com.mk.contractservice.domain.contract.importjob.ContractImportTermination;
import com.mk.contractservice.domain.contract.importjob.IdempotencyKey;
import com.mk.contractservice.domain.contract.importjob.exception.IdempotencyKeyReusedException;
import com.mk.contractservice.domain.contract.importjob.exception.InvalidIdempotencyKeyException;
import com.mk.contractservice.domain.contract.importjob.exception.InvalidImportFileException;
import com.mk.contractservice.features.shared.ContractImportCsv;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmitContractImportJob Handler")
class SubmitContractImportJobTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-03T09:00:00Z"), ZoneId.of("Europe/Zurich"));
    private static final String HEADER = "clientId;startDate;endDate;costAmount\n";
    private static final String SUBMITTER = "marie.counselor";
    private static final String KEY = "import-2026-11-03";

    @Mock
    private ContractImportJobRepository jobRepository;

    @Captor
    private ArgumentCaptor<ContractImportJob> jobCaptor;

    private static byte[] fileOf(final int lines) {
        return (HEADER + "line\n".repeat(lines)).getBytes(StandardCharsets.UTF_8);
    }

    private SubmitContractImportJob.Handler handler() {
        return new SubmitContractImportJob.Handler(jobRepository, CLOCK);
    }

    private static ContractImportJob storedJob(final byte[] content) {
        return ContractImportJob.reconstituteFromDatabase(UUID.randomUUID(),
                ContractImportSubmission.reconstituteFromDatabase("contracts.csv", SUBMITTER, IdempotencyKey.reconstituteFromDatabase(KEY), ContractImportCsv.fingerprintOf(content), LocalDateTime.now(CLOCK)),
                ContractImportJobStatus.COMPLETED, ContractImportProgress.reconstituteFromDatabase(2, 2, 2, 0), null,
                ContractImportTermination.reconstituteFromDatabase(LocalDateTime.now(CLOCK), null), null);
    }

    @Test
    @DisplayName(
            "GIVEN a valid file WHEN submitting THEN a pending job is stored with the file, its author and its fingerprint")
    void shouldStorePendingJob() {
        final byte[] content = fileOf(2);
        when(jobRepository.insert(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        final ContractImportJob result = handler().execute(new SubmitContractImportJob.Command("contracts.csv", content, SUBMITTER, null));

        verify(jobRepository).insert(jobCaptor.capture(), same(content));
        final ContractImportJob job = jobCaptor.getValue();
        assertThat(result).isSameAs(job);
        assertThat(job.status()).isEqualTo(ContractImportJobStatus.PENDING);
        ;
        assertThat(job.progress().totalLines()).isEqualTo(2);
        assertThat(job.submission().submittedAt()).isEqualTo(LocalDateTime.now(CLOCK));
        assertThat(job.submission().submittedBy()).isEqualTo(SUBMITTER);
        assertThat(job.submission().idempotencyKey()).isNull();
        assertThat(job.submission().contentSha256()).isEqualTo(ContractImportCsv.fingerprintOf(content));
    }

    @Test
    @DisplayName("GIVEN a new idempotency key WHEN submitting THEN the job is stored with its key")
    void shouldStoreNewKey() {
        when(jobRepository.findByIdempotencyKey(SUBMITTER, IdempotencyKey.of(KEY))).thenReturn(Optional.empty());
        when(jobRepository.insert(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler().execute(new SubmitContractImportJob.Command("contracts.csv", fileOf(2), SUBMITTER, KEY));

        verify(jobRepository).insert(jobCaptor.capture(), any());
        assertThat(jobCaptor.getValue().submission().idempotencyKey()).isEqualTo(IdempotencyKey.of(KEY));
    }

    @Test
    @DisplayName(
            "GIVEN the same file submitted again with the same key WHEN submitting THEN the first job is returned and nothing is stored")
    void shouldReturnExistingJobForSameKeyAndFile() {
        final byte[] content = fileOf(2);
        final ContractImportJob existing = storedJob(content);
        when(jobRepository.findByIdempotencyKey(SUBMITTER, IdempotencyKey.of(KEY))).thenReturn(Optional.of(existing));

        final ContractImportJob result = handler().execute(new SubmitContractImportJob.Command("contracts.csv", content, SUBMITTER, KEY));

        assertThat(result).isSameAs(existing);
        verify(jobRepository, never()).insert(any(), any());
    }

    @Test
    @DisplayName("GIVEN another file with an already used key WHEN submitting THEN it is refused and nothing is stored")
    void shouldRefuseKeyReusedForAnotherFile() {
        when(jobRepository.findByIdempotencyKey(SUBMITTER, IdempotencyKey.of(KEY))).thenReturn(Optional.of(storedJob(fileOf(2))));
        final SubmitContractImportJob.Command command = new SubmitContractImportJob.Command("contracts.csv", fileOf(3), SUBMITTER, KEY);

        assertThatThrownBy(() -> handler().execute(command)).isInstanceOf(IdempotencyKeyReusedException.class);
        verify(jobRepository, never()).insert(any(), any());
    }

    @Test
    @DisplayName("GIVEN a blank or too long key WHEN submitting THEN it is refused and nothing is stored")
    void shouldRefuseInvalidKey() {
        final byte[] content = fileOf(1);
        final String tooLong = "k".repeat(IdempotencyKey.MAX_LENGTH + 1);
        final SubmitContractImportJob.Command blankKey = new SubmitContractImportJob.Command("contracts.csv", content, SUBMITTER, " ");
        final SubmitContractImportJob.Command tooLongKey = new SubmitContractImportJob.Command("contracts.csv", content, SUBMITTER, tooLong);

        assertThatThrownBy(() -> handler().execute(blankKey)).isInstanceOf(InvalidIdempotencyKeyException.class);
        assertThatThrownBy(() -> handler().execute(tooLongKey)).isInstanceOf(InvalidIdempotencyKeyException.class);
        verify(jobRepository, never()).insert(any(), any());
    }

    @Test
    @DisplayName("GIVEN a key of the maximum length WHEN submitting THEN the job is stored with its key")
    void shouldAcceptKeyOfMaximumLength() {
        final String longestKey = "k".repeat(IdempotencyKey.MAX_LENGTH);
        when(jobRepository.findByIdempotencyKey(SUBMITTER, IdempotencyKey.of(longestKey))).thenReturn(Optional.empty());
        when(jobRepository.insert(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        handler().execute(new SubmitContractImportJob.Command("contracts.csv", fileOf(1), SUBMITTER, longestKey));

        verify(jobRepository).insert(jobCaptor.capture(), any());
        assertThat(jobCaptor.getValue().submission().idempotencyKey()).isEqualTo(IdempotencyKey.of(longestKey));
    }

    @Test
    @DisplayName("GIVEN no content or no author WHEN building Command THEN it is rejected")
    void shouldRequireContentAndAuthor() {
        final byte[] content = fileOf(1);

        assertThatThrownBy(() -> new SubmitContractImportJob.Command("contracts.csv", null, SUBMITTER, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SubmitContractImportJob.Command("contracts.csv", content, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("GIVEN an invalid file WHEN submitting THEN it is refused and nothing is stored")
    void shouldRefuseInvalidFile() {
        final SubmitContractImportJob.Command command = new SubmitContractImportJob.Command(
                "contracts.csv", "wrong;header\nline\n".getBytes(StandardCharsets.UTF_8), SUBMITTER, null);

        assertThatThrownBy(() -> handler().execute(command)).isInstanceOf(InvalidImportFileException.class);
        verify(jobRepository, never()).insert(any(), any());
    }
}
