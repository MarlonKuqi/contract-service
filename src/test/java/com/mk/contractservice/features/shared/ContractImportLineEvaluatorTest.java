package com.mk.contractservice.features.shared;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientFixtures;
import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
        return ContractImportLineEvaluator.evaluate(clientId, type, startDate, endDate, cost, EXISTING_CLIENTS, NOW);
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
