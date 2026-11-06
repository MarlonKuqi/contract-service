package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.exception.InvalidImportFileException;
import com.mk.contractservice.features.shared.ContractImportCsv;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ContractImportFile")
class ContractImportFileTest {

    private static final String HEADER = "clientId;startDate;endDate;costAmount\n";

    private static byte[] fileOf(final int lines) {
        return (HEADER + "line\n".repeat(lines)).getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("GIVEN a valid file WHEN of THEN it keeps its name and content, counts its lines and gives its fingerprint")
    void shouldDescribeValidFile() {
        final byte[] content = fileOf(2);

        final ContractImportFile file = ContractImportFile.of("contracts.csv", content);

        assertThat(file.getName()).isEqualTo("contracts.csv");
        assertThat(file.getContent()).isSameAs(content);
        assertThat(file.getTotalLines()).isEqualTo(2);
        assertThat(file.fingerprint()).isEqualTo(ContractImportCsv.fingerprintOf(content));
    }

    @Test
    @DisplayName("GIVEN no content WHEN of THEN it is rejected")
    void shouldRequireContent() {
        assertThatThrownBy(() -> ContractImportFile.of("contracts.csv", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("GIVEN a file without name or with a blank name WHEN of THEN it takes the default name")
    void shouldUseDefaultNameWhenMissing() {
        final byte[] content = fileOf(1);

        assertThat(ContractImportFile.of(null, content).getName()).isEqualTo(ContractImportFile.DEFAULT_NAME);
        assertThat(ContractImportFile.of(" ", content).getName()).isEqualTo(ContractImportFile.DEFAULT_NAME);
        assertThat(ContractImportFile.DEFAULT_NAME).isEqualTo("import.csv");
    }

    @Test
    @DisplayName("GIVEN a file name too long WHEN of THEN it is rejected")
    void shouldRejectNameTooLong() {
        final String name = "a".repeat(ContractImportFile.MAX_NAME_LENGTH + 1);
        final byte[] content = fileOf(1);

        assertThatThrownBy(() -> ContractImportFile.of(name, content)).isInstanceOf(InvalidImportFileException.class);
    }

    @Test
    @DisplayName("GIVEN exactly the maximum of lines WHEN of THEN it is accepted")
    void shouldAcceptMaximumOfLines() {
        final byte[] content = fileOf(ContractImportFile.MAX_LINES);

        assertThatCode(() -> ContractImportFile.of("contracts.csv", content)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GIVEN too many lines WHEN of THEN it is rejected")
    void shouldRejectTooManyLines() {
        final byte[] content = fileOf(ContractImportFile.MAX_LINES + 1);

        assertThatThrownBy(() -> ContractImportFile.of("contracts.csv", content))
                .isInstanceOf(InvalidImportFileException.class)
                .hasMessageContaining(String.valueOf(ContractImportFile.MAX_LINES));
    }

    @Test
    @DisplayName("GIVEN a file with a wrong header WHEN of THEN it is rejected")
    void shouldRejectInvalidCsv() {
        final byte[] content = "wrong;header\nline\n".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> ContractImportFile.of("contracts.csv", content)).isInstanceOf(InvalidImportFileException.class);
    }
}
