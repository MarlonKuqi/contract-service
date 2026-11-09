package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFixtures;
import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobFixtures;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobLine;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.features.shared.ContractImportCsv.RawLine;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessContractImportBatch Handler")
class ProcessContractImportBatchTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Zurich");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 3, 10, 0);
    private static final Clock CLOCK = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_CLIENT_ID = UUID.randomUUID();

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ContractImportJobRepository jobRepository;
    @Captor
    private ArgumentCaptor<List<ContractImportJobLine>> linesCaptor;
    @Captor
    private ArgumentCaptor<ContractImportJob> jobCaptor;

    private ProcessContractImportBatch handler;

    @BeforeEach
    void setUp() {
        handler = new ProcessContractImportBatch.Handler(clientRepository, contractRepository, jobRepository, CLOCK);
    }

    private ContractImportJob runningJob(final int totalLines) {
        return ContractImportJobFixtures.runningJob(JOB_ID, totalLines, 0, 0, CLOCK.instant().plusSeconds(60), NOW);
    }

    private Contract persisted(final UUID id) {
        return ContractFixtures.existingContract(id, CLIENT_ID, NOW, null, new BigDecimal("100.00"), NOW);
    }

    private static Tuple outcomeOf(final ContractImportJobLine line) {
        return switch (line) {
            case ContractImportJobLine.Imported(_, int lineNumber, UUID clientId, UUID contractId) -> tuple(
                    lineNumber,
                    clientId,
                    contractId);
            case ContractImportJobLine.Rejected(_, int lineNumber, UUID clientId, ContractImportRejectionReason reason, _) ->
                tuple(lineNumber, clientId, reason);
        };
    }

    @Test
    @DisplayName("GIVEN mixed lines WHEN processing THEN valid lines create contracts and the others are rejected")
    void shouldImportValidLinesAndRejectOthers() {
        final UUID contractId = UUID.randomUUID();
        when(jobRepository.getById(JOB_ID)).thenReturn(runningJob(5));
        when(clientRepository.lockExistingIds(Set.of(CLIENT_ID, UNKNOWN_CLIENT_ID))).thenReturn(Set.of(CLIENT_ID));
        when(contractRepository.saveAll(anyList())).thenReturn(List.of(persisted(contractId)));

        handler.execute(new ProcessContractImportBatch.Command(JOB_ID,
                List.of(
                        new RawLine(2, CLIENT_ID + ";;;100.00"),
                        new RawLine(3, CLIENT_ID + ";;;-1"),
                        new RawLine(4, UNKNOWN_CLIENT_ID + ";;;100.00"),
                        new RawLine(5, "garbage"),
                        new RawLine(6, CLIENT_ID + ";2026-01-01T00:00:00;2025-01-01T00:00:00;100.00")
                )));

        verify(jobRepository).saveLines(linesCaptor.capture());
        assertThat(linesCaptor.getValue())
                .extracting(ProcessContractImportBatchTest::outcomeOf)
                .containsExactlyInAnyOrder(
                        tuple(2, CLIENT_ID, contractId),
                        tuple(3, CLIENT_ID, ContractImportRejectionReason.INVALID_COST),
                        tuple(4, UNKNOWN_CLIENT_ID, ContractImportRejectionReason.CLIENT_NOT_FOUND),
                        tuple(5, null, ContractImportRejectionReason.INVALID_LINE_FORMAT),
                        tuple(6, CLIENT_ID, ContractImportRejectionReason.INVALID_PERIOD)
                );
    }

    @Test
    @DisplayName(
        "GIVEN several valid lines around a rejected one WHEN processing THEN each imported line keeps its own contract")
    void shouldLinkEachImportedLineToItsContract() {
        final UUID firstContractId = UUID.randomUUID();
        final UUID secondContractId = UUID.randomUUID();
        when(jobRepository.getById(JOB_ID)).thenReturn(runningJob(3));
        when(clientRepository.lockExistingIds(Set.of(CLIENT_ID))).thenReturn(Set.of(CLIENT_ID));
        when(contractRepository.saveAll(anyList())).thenReturn(List.of(persisted(firstContractId),
                persisted(secondContractId)));

        handler.execute(new ProcessContractImportBatch.Command(JOB_ID,
                List.of(
                        new RawLine(2, CLIENT_ID + ";;;100.00"),
                        new RawLine(3, CLIENT_ID + ";;;-1"),
                        new RawLine(4, CLIENT_ID + ";;;200.00")
                )));

        verify(jobRepository).saveLines(linesCaptor.capture());
        assertThat(linesCaptor.getValue())
                .filteredOn(ContractImportJobLine.Imported.class::isInstance)
                .extracting(ProcessContractImportBatchTest::outcomeOf)
                .containsExactlyInAnyOrder(
                        tuple(2, CLIENT_ID, firstContractId),
                        tuple(4, CLIENT_ID, secondContractId)
                );
    }

    @Test
    @DisplayName("GIVEN a batch WHEN processing THEN the job records its counters and renews its lease")
    void shouldRecordProgress() {
        when(jobRepository.getById(JOB_ID)).thenReturn(runningJob(5));
        when(clientRepository.lockExistingIds(any())).thenReturn(Set.of(CLIENT_ID));
        when(contractRepository.saveAll(anyList())).thenReturn(List.of(persisted(UUID.randomUUID())));

        handler.execute(new ProcessContractImportBatch.Command(JOB_ID,
                List.of(
                        new RawLine(2, CLIENT_ID + ";;;100.00"),
                        new RawLine(3, CLIENT_ID + ";;;-1")
                )));

        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().progress().processedLines()).isEqualTo(2);
        assertThat(jobCaptor.getValue().progress().importedCount()).isEqualTo(1);
        assertThat(jobCaptor.getValue().progress().rejectedCount()).isEqualTo(1);
        assertThat(jobCaptor.getValue().state().leaseUntil()).isEqualTo(CLOCK.instant()
                .plus(ClaimContractImportJob.LEASE));
    }
}
