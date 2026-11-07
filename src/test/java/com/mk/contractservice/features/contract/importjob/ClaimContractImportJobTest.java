package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobFixtures;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClaimContractImportJob Handler")
class ClaimContractImportJobTest {

    private static final Instant NOW = Instant.parse("2026-11-03T09:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Europe/Zurich"));

    @Mock
    private ContractImportJobRepository jobRepository;

    @Test
    @DisplayName("GIVEN a job to run WHEN claiming THEN it runs under a new lease")
    void shouldClaimUnderLease() {
        when(jobRepository.findNextToRun(NOW)).thenReturn(Optional.of(ContractImportJobFixtures.pendingJob(1, LocalDateTime.now(CLOCK))));
        when(jobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final Optional<ContractImportJob> claimed = new ClaimContractImportJob.Handler(jobRepository, CLOCK).execute();

        assertThat(claimed).isPresent();
        assertThat(claimed.get().status()).isEqualTo(ContractImportJobState.Status.RUNNING);
        assertThat(claimed.get().state().leaseUntil()).isEqualTo(NOW.plus(ClaimContractImportJob.LEASE));
    }

    @Test
    @DisplayName("GIVEN the hour repeated at the end of summer time WHEN claiming THEN the lease still lasts its real duration")
    void shouldKeepLeaseDurationAcrossClockChange() {
        final Instant duringRepeatedHour = Instant.parse("2026-10-25T00:58:00Z");
        final Clock clock = Clock.fixed(duringRepeatedHour, ZoneId.of("Europe/Zurich"));
        when(jobRepository.findNextToRun(duringRepeatedHour)).thenReturn(Optional.of(ContractImportJobFixtures.pendingJob(1, LocalDateTime.now(clock))));
        when(jobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final Optional<ContractImportJob> claimed = new ClaimContractImportJob.Handler(jobRepository, clock).execute();

        assertThat(claimed.orElseThrow().state().leaseUntil()).isEqualTo(Instant.parse("2026-10-25T01:03:00Z"));
    }

    @Test
    @DisplayName("GIVEN no job to run WHEN claiming THEN nothing is saved")
    void shouldClaimNothing() {
        when(jobRepository.findNextToRun(NOW)).thenReturn(Optional.empty());

        assertThat(new ClaimContractImportJob.Handler(jobRepository, CLOCK).execute()).isEmpty();

        verify(jobRepository, never()).save(any());
    }
}
