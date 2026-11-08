package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportCancellation;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobFixtures;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobState;
import com.mk.contractservice.domain.contract.importjob.ContractImportProgress;
import com.mk.contractservice.domain.contract.importjob.ContractImportTermination;
import com.mk.contractservice.domain.contract.importjob.exception.ContractImportJobNotFoundException;
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
@DisplayName("CancelContractImportJob Handler")
class CancelContractImportJobTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-03T09:00:00Z"), ZoneId.of("Europe/Zurich"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final String COUNSELOR = "paul.counselor";

    @Mock
    private ContractImportJobRepository jobRepository;

    @Mock
    private ContractRepository contractRepository;

    @Captor
    private ArgumentCaptor<ContractImportJob> jobCaptor;

    private CancelContractImportJob.Handler handler() {
        return new CancelContractImportJob.Handler(jobRepository, contractRepository, CLOCK);
    }

    private static ContractImportJob completedJob() {
        return ContractImportJob.reconstituteFromDatabase(JOB_ID,
                ContractImportJobFixtures.submission(NOW.minusHours(1)),
                ContractImportJobState.Status.COMPLETED,
                ContractImportProgress.reconstituteFromDatabase(3, 3, 2, 1),
                null,
                ContractImportTermination.reconstituteFromDatabase(NOW.minusMinutes(30), null),
                null);
    }

    @Test
    @DisplayName("GIVEN a completed job WHEN cancelling THEN it is cancelled and its active contracts are closed now")
    void shouldCancelJobAndCloseItsContracts() {
        when(jobRepository.lockById(JOB_ID)).thenReturn(completedJob());
        when(jobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contractRepository.closeAllActiveImportedBy(JOB_ID, NOW)).thenReturn(2);

        final CancelContractImportJob.Result result = handler().execute(new CancelContractImportJob.Command(JOB_ID,
                COUNSELOR));

        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().status()).isEqualTo(ContractImportJobState.Status.CANCELLED);
        assertThat(jobCaptor.getValue().state().cancellation()).isEqualTo(ContractImportCancellation.of(NOW,
                COUNSELOR));
        assertThat(result.job()).isSameAs(jobCaptor.getValue());
        assertThat(result.closedContracts()).isEqualTo(2);
    }

    @Test
    @DisplayName("GIVEN an already cancelled job WHEN cancelling again THEN nothing is saved nor closed")
    void shouldIgnoreAlreadyCancelledJob() {
        final ContractImportJob cancelled = ContractImportJobFixtures.cancelledJob(JOB_ID);
        when(jobRepository.lockById(JOB_ID)).thenReturn(cancelled);

        final CancelContractImportJob.Result result = handler().execute(new CancelContractImportJob.Command(JOB_ID,
                "other.counselor"));

        assertThat(result.job()).isSameAs(cancelled);
        assertThat(result.closedContracts()).isZero();
        verify(jobRepository, never()).save(any());
        verify(contractRepository, never()).closeAllActiveImportedBy(any(), any());
    }

    @Test
    @DisplayName("GIVEN an unknown job WHEN cancelling THEN it is not found and nothing is closed")
    void shouldNotCloseContractsOfUnknownJob() {
        when(jobRepository.lockById(JOB_ID)).thenThrow(new ContractImportJobNotFoundException(JOB_ID));

        assertThatThrownBy(() -> handler().execute(new CancelContractImportJob.Command(JOB_ID, COUNSELOR)))
                .isInstanceOf(ContractImportJobNotFoundException.class);
        verify(contractRepository, never()).closeAllActiveImportedBy(any(), any());
    }
}
