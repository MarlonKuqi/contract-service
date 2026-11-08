package com.mk.contractservice.features.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ContractImportCsv")
class ContractImportCsvTest {

    private static final String HEADER = "clientId;startDate;endDate;costAmount\n";
    private static final UUID CLIENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private static byte[] bytes(final String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("GIVEN a valid file WHEN counting THEN only data lines count")
    void shouldCountDataLines() {
        assertThat(ContractImportCsv.requireValidAndCountLines(bytes(HEADER + "a\n\nb\n"))).isEqualTo(2);
    }

    @Test
    @DisplayName("GIVEN a BOM and CRLF line endings WHEN counting THEN the file is accepted")
    void shouldAcceptBomAndCrlf() {
        final byte[] content = bytes("\uFEFFclientId;startDate;endDate;costAmount\r\na\r\n");

        assertThat(ContractImportCsv.requireValidAndCountLines(content)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"", "clientId,startDate,endDate,costAmount\na\n", "wrong\na\n",
                    "clientId;startDate;endDate;costAmount\n", "clientId;startDate;endDate;costAmount\n\n"})
    @DisplayName("GIVEN no data or a wrong header WHEN counting THEN the file is rejected")
    void shouldRejectInvalidFiles(final String content) {
        assertThatThrownBy(() -> ContractImportCsv.requireValidAndCountLines(bytes(content)))
                .isInstanceOf(InvalidImportFileException.class);
    }

    @Test
    @DisplayName("GIVEN bytes that are not UTF-8 WHEN counting THEN the file is rejected")
    void shouldRejectInvalidEncoding() {
        final byte[] latin1 = (HEADER + "é\n").getBytes(StandardCharsets.ISO_8859_1);

        assertThatThrownBy(() -> ContractImportCsv.requireValidAndCountLines(latin1))
                .isInstanceOf(InvalidImportFileException.class);
    }

    @Test
    @DisplayName("GIVEN a full line WHEN parsing THEN every field is read")
    void shouldParseFullLine() {
        final ContractImportCsv.ParsedLine parsed = new ContractImportCsv.RawLine(
                2,
                CLIENT_ID + ";2026-01-01T00:00:00;2027-01-01T00:00:00;1500.50").parse();

        assertThat(parsed.clientId()).isEqualTo(CLIENT_ID);
        assertThat(parsed.startDate()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        assertThat(parsed.endDate()).isEqualTo(LocalDateTime.of(2027, 1, 1, 0, 0));
        assertThat(parsed.costAmount()).isEqualByComparingTo(new BigDecimal("1500.50"));
    }

    @Test
    @DisplayName("GIVEN empty dates WHEN parsing THEN they are null")
    void shouldParseEmptyDatesAsNull() {
        final ContractImportCsv.ParsedLine parsed = new ContractImportCsv.RawLine(2, CLIENT_ID + ";;;100").parse();

        assertThat(parsed.startDate()).isNull();
        assertThat(parsed.endDate()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-a-uuid;;;100",
            ";;;100",
            "550e8400-e29b-41d4-a716-446655440000;;;",
            "550e8400-e29b-41d4-a716-446655440000;;;abc",
            "550e8400-e29b-41d4-a716-446655440000;;;100,50",
            "550e8400-e29b-41d4-a716-446655440000;yesterday;;100",
            "550e8400-e29b-41d4-a716-446655440000;;tomorrow;100",
            "550e8400-e29b-41d4-a716-446655440000;;100",
            "550e8400-e29b-41d4-a716-446655440000;;;100;extra"
    })
    @DisplayName("GIVEN a malformed line WHEN parsing THEN it fails")
    void shouldRejectMalformedLines(final String raw) {
        final ContractImportCsv.RawLine line = new ContractImportCsv.RawLine(2, raw);

        assertThatThrownBy(line::parse).isInstanceOf(ContractImportCsv.MalformedLineException.class);
    }

    @Test
    @DisplayName("GIVEN a NUL character WHEN parsing THEN it fails without repeating it in the message")
    void shouldRejectNulCharacter() {
        final ContractImportCsv.RawLine line = new ContractImportCsv.RawLine(2, "ab\u0000c;;;100");

        assertThatThrownBy(line::parse)
                .isInstanceOf(ContractImportCsv.MalformedLineException.class)
                .hasMessageNotContaining("\u0000");
    }

    @Test
    @DisplayName("GIVEN a line starting with a NUL character WHEN parsing THEN it fails with a readable message")
    void shouldRejectLeadingNulCharacter() {
        final ContractImportCsv.RawLine line = new ContractImportCsv.RawLine(2, "\u0000abc;;;100");

        assertThatThrownBy(line::parse)
                .isInstanceOf(ContractImportCsv.MalformedLineException.class)
                .hasMessage("The line contains a NUL character");
    }

    @Test
    @DisplayName("GIVEN a malformed line with a readable client WHEN parsing fails THEN the client is known")
    void shouldKeepClientIdOfMalformedLine() {
        assertThat(new ContractImportCsv.RawLine(2, CLIENT_ID + ";;;abc").clientIdIfReadable()).contains(CLIENT_ID);
        assertThat(new ContractImportCsv.RawLine(2, "not-a-uuid;;;abc").clientIdIfReadable()).isEmpty();
    }
}
