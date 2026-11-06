package com.mk.contractservice.domain.contract.importjob;

import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ContractImportJobLine")
class ContractImportJobLineTest {

    private static final UUID JOB_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID CONTRACT_ID = UUID.randomUUID();
    private static final String MESSAGE = "Contract cost amount must be greater than zero: -1";

    @Test
    @DisplayName("GIVEN a created contract WHEN recording its line THEN the line is imported with its contract")
    void shouldRecordImportedLine() {
        final ContractImportJobLine line = ContractImportJobLine.imported(JOB_ID, 2, CLIENT_ID, CONTRACT_ID);

        assertThat(line).isInstanceOfSatisfying(ContractImportJobLine.Imported.class, imported -> {
            assertThat(imported.jobId()).isEqualTo(JOB_ID);
            assertThat(imported.lineNumber()).isEqualTo(2);
            assertThat(imported.clientId()).isEqualTo(CLIENT_ID);
            assertThat(imported.contractId()).isEqualTo(CONTRACT_ID);
        });
    }

    @Test
    @DisplayName("GIVEN an imported line without client WHEN recording it THEN it is refused")
    void shouldRefuseImportedLineWithoutClient() {
        assertThatThrownBy(() -> ContractImportJobLine.imported(JOB_ID, 2, null, CONTRACT_ID))
                .isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName("GIVEN an imported line without contract WHEN recording it THEN it is refused")
    void shouldRefuseImportedLineWithoutContract() {
        assertThatThrownBy(() -> ContractImportJobLine.imported(JOB_ID, 2, CLIENT_ID, null))
                .isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName("GIVEN a rejected line WHEN recording it THEN the line keeps its reason and message")
    void shouldRecordRejectedLine() {
        final ContractImportJobLine line = ContractImportJobLine.rejected(
                JOB_ID, 3, CLIENT_ID, ContractImportRejectionReason.INVALID_COST, MESSAGE);

        assertThat(line).isInstanceOfSatisfying(ContractImportJobLine.Rejected.class, rejected -> {
            assertThat(rejected.lineNumber()).isEqualTo(3);
            assertThat(rejected.clientId()).isEqualTo(CLIENT_ID);
            assertThat(rejected.reason()).isEqualTo(ContractImportRejectionReason.INVALID_COST);
            assertThat(rejected.message()).isEqualTo(MESSAGE);
        });
    }

    @Test
    @DisplayName("GIVEN an unreadable client WHEN rejecting the line THEN it is recorded without client")
    void shouldRecordRejectedLineWithoutClient() {
        final ContractImportJobLine line = ContractImportJobLine.rejected(
                JOB_ID, 4, null, ContractImportRejectionReason.INVALID_LINE_FORMAT, MESSAGE);

        assertThat(line.clientId()).isNull();
    }

    @Test
    @DisplayName("GIVEN a rejected line without reason WHEN recording it THEN it is refused")
    void shouldRefuseRejectedLineWithoutReason() {
        assertThatThrownBy(() -> ContractImportJobLine.rejected(JOB_ID, 3, CLIENT_ID, null, MESSAGE))
                .isInstanceOf(InvalidDomainObjectError.class);
    }

    @Test
    @DisplayName("GIVEN a rejected line without message WHEN recording it THEN it is refused")
    void shouldRefuseRejectedLineWithoutMessage() {
        assertThatThrownBy(() -> ContractImportJobLine.rejected(JOB_ID, 3, CLIENT_ID, ContractImportRejectionReason.INVALID_COST, null))
                .isInstanceOf(InvalidDomainObjectError.class);
    }
}
