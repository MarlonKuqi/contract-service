package com.mk.contractservice.features.shared;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContractImportCsv {

    public static final String SEPARATOR = ";";
    public static final String HEADER = "clientId" + SEPARATOR + "startDate" + SEPARATOR + "endDate" + SEPARATOR + "costAmount";

    private static final String BYTE_ORDER_MARK = "\uFEFF";
    private static final int COLUMNS = 4;
    private static final char NUL = '\0';

    public record RawLine(int lineNumber, String raw) {

        private static final int CLIENT_ID = 0;
        private static final int START_DATE = 1;
        private static final int END_DATE = 2;
        private static final int COST_AMOUNT = 3;

        public Optional<UUID> clientIdIfReadable() {
            try {
                return Optional.of(UUID.fromString(columns()[CLIENT_ID]));
            } catch (final IllegalArgumentException _) {
                return Optional.empty();
            }
        }

        public ParsedLine parse() {
            final String[] columns = checkedColumns();
            try {
                return new ParsedLine(
                        UUID.fromString(columns[CLIENT_ID]),
                        optionalDate(columns[START_DATE]),
                        optionalDate(columns[END_DATE]),
                        new BigDecimal(columns[COST_AMOUNT])
                );
            } catch (final IllegalArgumentException | DateTimeParseException ex) {
                throw new MalformedLineException("Unreadable value: " + ex.getMessage());
            }
        }

        private String[] checkedColumns() {
            if (raw.indexOf(NUL) >= 0) {
                throw new MalformedLineException("The line contains a NUL character");
            }
            final String[] columns = columns();
            if (columns.length != COLUMNS) {
                throw new MalformedLineException(
                        "Expected " + COLUMNS + " columns (" + HEADER + "), but found " + columns.length);
            }
            return columns;
        }

        private String[] columns() {
            return raw.split(SEPARATOR, -1);
        }

        private static @Nullable LocalDateTime optionalDate(final String value) {
            return value.isEmpty() ? null : LocalDateTime.parse(value);
        }
    }

    public record ParsedLine(
            UUID clientId,
            @Nullable LocalDateTime startDate,
            @Nullable LocalDateTime endDate,
            BigDecimal costAmount
    ) {
    }

    public static class MalformedLineException extends RuntimeException {

        public MalformedLineException(final String message) {
            super(message);
        }

        @Override
        public String getMessage() {
            return Objects.requireNonNull(super.getMessage());
        }
    }

    public static int requireValidAndCountLines(final byte[] content) {
        try (BufferedReader reader = readerOf(content)) {
            final String header = reader.readLine();
            if (header == null || !HEADER.equals(header.replace(BYTE_ORDER_MARK, ""))) {
                throw new InvalidImportFileException("The first line of the file must be: " + HEADER);
            }
            int dataLines = 0;
            String line = reader.readLine();
            while (line != null) {
                if (!line.isBlank()) {
                    dataLines++;
                }
                line = reader.readLine();
            }
            if (dataLines == 0) {
                throw new InvalidImportFileException("The file must contain at least one contract");
            }
            return dataLines;
        } catch (final IOException _) {
            throw new InvalidImportFileException("The file must be encoded in UTF-8");
        }
    }

    public static Map<Integer, String> linesAt(final byte[] content, final Set<Integer> lineNumbers) {
        final Map<Integer, String> lines = new HashMap<>();
        try (BufferedReader reader = readerOf(content)) {
            int lineNumber = 1;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (lineNumbers.contains(lineNumber)) {
                    lines.put(lineNumber, line);
                }
                lineNumber++;
            }
            return lines;
        } catch (final IOException ex) {
            throw new IllegalStateException("The stored file can no longer be read", ex);
        }
    }

    public static String fingerprintOf(final byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (final NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private static BufferedReader readerOf(final byte[] content) {
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content),
                StandardCharsets.UTF_8.newDecoder()));
    }
}
