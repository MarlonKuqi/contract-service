package com.mk.contractservice.features.shared;

import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import com.mk.contractservice.features.shared.ContractImportLineEvaluator.Accepted;
import com.mk.contractservice.features.shared.ContractImportLineEvaluator.Outcome;
import com.mk.contractservice.features.shared.ContractImportLineEvaluator.Rejected;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContractImportLineEvaluator")
class ContractImportLineEvaluatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 6, 10, 0);
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final Set<UUID> EXISTING_CLIENTS = Set.of(CLIENT_ID);
    private static final BigDecimal VALID_COST = new BigDecimal("100.00");
    private static final BigDecimal NEGATIVE_COST = new BigDecimal("-1.00");

    private static Outcome evaluate(final UUID clientId,
            final LocalDateTime startDate,
            final LocalDateTime endDate,
            final BigDecimal cost) {
        return ContractImportLineEvaluator.evaluate(clientId, startDate, endDate, cost, EXISTING_CLIENTS, NOW);
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
}
