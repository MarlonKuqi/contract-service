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

    public static String write(final List<RejectedLine> lines) {
        final StringBuilder csv = new StringBuilder(HEADER).append(LINE_END);
        for (final RejectedLine line : lines) {
            csv.append(line.lineNumber()).append(SEPARATOR)
                    .append(line.reason().getCode()).append(SEPARATOR)
                    .append(quoted(line.message())).append(SEPARATOR)
                    .append(quoted(line.line())).append(LINE_END);
        }
        return csv.toString();
    }

    private static String quoted(final String value) {
        return QUOTE + value.replace(QUOTE, QUOTE + QUOTE) + QUOTE;
    }
}
