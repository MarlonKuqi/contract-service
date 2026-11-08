package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.features.shared.ContractImportCsv;
import com.mk.contractservice.features.shared.InvalidImportFileException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContractImportFile {

    public static final int MAX_LINES = 500_000;
    public static final int MAX_NAME_LENGTH = 255;
    public static final String DEFAULT_NAME = "import.csv";

    private final String name;
    private final byte[] content;
    private final int totalLines;

    public static ContractImportFile of(@Nullable final String rawName, final byte[] content) {
        Objects.requireNonNull(content, "Content cannot be null");
        final String name = rawName == null || rawName.isBlank() ? DEFAULT_NAME : rawName;
        if (name.length() > MAX_NAME_LENGTH) {
            throw new InvalidImportFileException("The file name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        final int totalLines = ContractImportCsv.requireValidAndCountLines(content);
        if (totalLines > MAX_LINES) {
            throw new InvalidImportFileException(
                    "The file must not exceed " + MAX_LINES + " contracts, but had: " + totalLines);
        }
        return new ContractImportFile(name, content, totalLines);
    }

    public String fingerprint() {
        return ContractImportCsv.fingerprintOf(content);
    }
}
