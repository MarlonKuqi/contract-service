package com.mk.contractservice.features.shared;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientFixtures;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFixtures;
import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import com.mk.contractservice.domain.contract.ContractOverlapPolicy;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.features.shared.ContractImportLineEvaluator.Accepted;
import com.mk.contractservice.features.shared.ContractImportLineEvaluator.Outcome;
import com.mk.contractservice.features.shared.ContractImportLineEvaluator.Rejected;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ContractImportLineEvaluator")
class ContractImportLineEvaluatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 6, 10, 0);
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final Map<UUID, Client> EXISTING_CLIENTS = ContractImportLineEvaluator.indexById(List.of(
            ClientFixtures.existingPerson(CLIENT_ID),
            ClientFixtures.existingCompany(COMPANY_ID)));
    private static final BigDecimal VALID_COST = new BigDecimal("100.00");
    private static final BigDecimal NEGATIVE_COST = new BigDecimal("-1.00");

    private static ContractOverlapPolicy.Batch overlapBatchOf(final Contract... existingContracts) {
        final ContractRepository contractRepository = mock(ContractRepository.class);
        when(contractRepository.lockAndFindAllByClientIdsAndTypes(anySet(), anySet())).thenReturn(List.of(
                existingContracts));
        return new ContractOverlapPolicy(contractRepository).startBatch(Set.of(CLIENT_ID, COMPANY_ID), NOW);
    }

    private static Outcome evaluate(final UUID clientId,
            final LocalDateTime startDate,
            final LocalDateTime endDate,
            final BigDecimal cost) {
        return evaluate(clientId, ContractType.LAMAL, startDate, endDate, cost);
    }

    private static Outcome evaluate(final UUID clientId,
            final ContractType type,
            final LocalDateTime startDate,
            final LocalDateTime endDate,
            final BigDecimal cost) {
        return evaluate(clientId, type, startDate, endDate, cost, overlapBatchOf());
    }

    private static Outcome evaluate(final UUID clientId,
            final ContractType type,
            final LocalDateTime startDate,
            final LocalDateTime endDate,
            final BigDecimal cost,
            final ContractOverlapPolicy.Batch overlapBatch) {
        return ContractImportLineEvaluator.evaluate(clientId,
                type,
                startDate,
                endDate,
                cost,
                EXISTING_CLIENTS,
                overlapBatch,
                NOW);
    }

    @Test
    @DisplayName("GIVEN a LAMal of the client covering the period WHEN evaluating a LAMal THEN the line is rejected for the overlap")
    void shouldRejectLamalOverlappingExistingOne() {
        final ContractOverlapPolicy.Batch overlapBatch = overlapBatchOf(ContractFixtures.existingContract(
                UUID.randomUUID(),
                CLIENT_ID,
                NOW.minusYears(1),
                null,
                VALID_COST,
                NOW));

        final Outcome outcome = evaluate(CLIENT_ID, ContractType.LAMAL, null, null, VALID_COST, overlapBatch);

        assertThat(outcome).isInstanceOfSatisfying(Rejected.class, rejected -> {
            assertThat(rejected.reason()).isEqualTo(ContractImportRejectionReason.OVERLAPPING_CONTRACT);
            assertThat(rejected.message()).contains(CLIENT_ID.toString());
        });
    }

    @Test
    @DisplayName("GIVEN an accepted LAMal line WHEN evaluating an overlapping LAMal line in the same import THEN the second one is rejected")
    void shouldRejectLineOverlappingAPreviouslyAcceptedOne() {
        final ContractOverlapPolicy.Batch overlapBatch = overlapBatchOf();

        final Outcome first = evaluate(CLIENT_ID, ContractType.LAMAL, null, null, VALID_COST, overlapBatch);
        final Outcome second = evaluate(CLIENT_ID,
                ContractType.LAMAL,
                NOW.plusYears(1),
                null,
                VALID_COST,
                overlapBatch);

        assertThat(first).isInstanceOf(Accepted.class);
        assertThat(second).isInstanceOfSatisfying(Rejected.class,
                rejected -> assertThat(rejected.reason()).isEqualTo(
                        ContractImportRejectionReason.OVERLAPPING_CONTRACT));
    }

    @Test
    @DisplayName("GIVEN a rejected line WHEN evaluating a line overlapping only the rejected one THEN it is accepted")
    void shouldNotCoverWithRejectedLine() {
        final ContractOverlapPolicy.Batch overlapBatch = overlapBatchOf();

        final Outcome rejected = evaluate(CLIENT_ID, ContractType.LAMAL, null, null, NEGATIVE_COST, overlapBatch);
        final Outcome accepted = evaluate(CLIENT_ID, ContractType.LAMAL, null, null, VALID_COST, overlapBatch);

        assertThat(rejected).isInstanceOf(Rejected.class);
        assertThat(accepted).isInstanceOf(Accepted.class);
    }

    @Test
    @DisplayName("GIVEN a valid line for an existing client WHEN evaluating THEN its contract starts now by default")
    void shouldAcceptValidLine() {
        final Outcome outcome = evaluate(CLIENT_ID, null, null, VALID_COST);

        assertThat(outcome).isInstanceOfSatisfying(Accepted.class, accepted -> {
            assertThat(accepted.contract().clientId()).isEqualTo(CLIENT_ID);
            assertThat(accepted.contract().period().startDate()).isEqualTo(NOW);
            assertThat(accepted.contract().costAmount().value()).isEqualByComparingTo(VALID_COST);
        });
    }

    @Test
    @DisplayName("GIVEN an unknown client and an invalid cost WHEN evaluating THEN only the unknown client is reported")
    void shouldReportUnknownClientFirst() {
        final UUID unknownClientId = UUID.randomUUID();

        final Outcome outcome = evaluate(unknownClientId, null, null, NEGATIVE_COST);

        assertThat(outcome).isInstanceOfSatisfying(Rejected.class, rejected -> {
            assertThat(rejected.reason()).isEqualTo(ContractImportRejectionReason.CLIENT_NOT_FOUND);
            assertThat(rejected.message()).contains(unknownClientId.toString());
        });
    }

    @Test
    @DisplayName("GIVEN an invalid cost WHEN evaluating THEN the line is rejected for its cost")
    void shouldRejectInvalidCost() {
        final Outcome outcome = evaluate(CLIENT_ID, null, null, NEGATIVE_COST);

        assertThat(outcome).isInstanceOfSatisfying(Rejected.class,
                rejected -> assertThat(rejected.reason()).isEqualTo(ContractImportRejectionReason.INVALID_COST));
    }

    @Test
    @DisplayName("GIVEN an invalid period and an invalid cost WHEN evaluating THEN only the period is reported")
    void shouldReportPeriodBeforeCost() {
        final Outcome outcome = evaluate(CLIENT_ID, NOW, NOW.minusDays(1), NEGATIVE_COST);

        assertThat(outcome).isInstanceOfSatisfying(Rejected.class,
                rejected -> assertThat(rejected.reason()).isEqualTo(ContractImportRejectionReason.INVALID_PERIOD));
    }

    @Test
    @DisplayName("GIVEN a LAMal line for a company WHEN evaluating THEN the line is rejected for its type")
    void shouldRejectTypeNotAllowedForClient() {
        final Outcome outcome = evaluate(COMPANY_ID, ContractType.LAMAL, null, null, VALID_COST);

        assertThat(outcome).isInstanceOfSatisfying(Rejected.class, rejected -> {
            assertThat(rejected.reason()).isEqualTo(ContractImportRejectionReason.TYPE_NOT_ALLOWED);
            assertThat(rejected.message()).contains("LAMAL").contains("company");
        });
    }

    @Test
    @DisplayName("GIVEN a type not allowed and an invalid cost WHEN evaluating THEN only the cost is reported")
    void shouldReportCostBeforeType() {
        final Outcome outcome = evaluate(COMPANY_ID, ContractType.LAMAL, null, null, NEGATIVE_COST);

        assertThat(outcome).isInstanceOfSatisfying(Rejected.class,
                rejected -> assertThat(rejected.reason()).isEqualTo(ContractImportRejectionReason.INVALID_COST));
    }
}
