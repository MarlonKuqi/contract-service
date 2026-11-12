package com.mk.contractservice.controllers.contract.shared;

import com.mk.contractservice.features.contract.importjob.ExportContractImportJobRejections.RejectedLine;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContractImportRejectionCsv {

    public static final String SEPARATOR = ";";
    public static final String HEADER = "lineNumber" + SEPARATOR + "rejectionCode" + SEPARATOR + "rejectionMessage" + SEPARATOR + "line";

    private static final String LINE_END = "\r\n";
    private static final String QUOTE = "\"";
    private static final String TEXT_MARKER = "'";
    private static final String FORMULA_TRIGGERS = "=+-@\t\r";

    public static String write(final List<RejectedLine> lines) {
        final StringBuilder csv = new StringBuilder(HEADER).append(LINE_END);
        for (final RejectedLine line : lines) {
            csv.append(line.lineNumber())
                    .append(SEPARATOR)
                    .append(ContractImportCodes.of(line.reason()))
                    .append(SEPARATOR)
                    .append(quoted(line.message()))
                    .append(SEPARATOR)
                    .append(quoted(line.line()))
                    .append(LINE_END);
        }
        return csv.toString();
    }

    private static String quoted(final String value) {
        return QUOTE + asText(value).replace(QUOTE, QUOTE + QUOTE) + QUOTE;
    }

    private static String asText(final String value) {
        if (!value.isEmpty() && FORMULA_TRIGGERS.indexOf(value.charAt(0)) >= 0) {
            return TEXT_MARKER + value;
        }
        return value;
    }
}
