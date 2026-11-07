package com.mk.contractservice.domain.contract.importjob;

import com.mk.contractservice.domain.shared.InvalidDomainObjectError;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ContractImportProgress {

    int totalLines;

    int processedLines;

    int importedCount;

    int rejectedCount;

    public static ContractImportProgress notStarted(final int totalLines) {
        if (totalLines < 1) {
            throw new InvalidDomainObjectError("An import job must have at least one line, but had: " + totalLines);
        }
        return new ContractImportProgress(totalLines, 0, 0, 0);
    }

    public static ContractImportProgress reconstituteFromDatabase(
            final int totalLines,
            final int processedLines,
            final int importedCount,
            final int rejectedCount
    ) {
        return new ContractImportProgress(totalLines, processedLines, importedCount, rejectedCount);
    }

    public ContractImportProgress advance(final int imported, final int rejected) {
        if (imported < 0 || rejected < 0) {
            throw new InvalidDomainObjectError("A batch cannot remove lines: imported " + imported + ", rejected " + rejected);
        }
        final int processed = processedLines + imported + rejected;
        if (processed > totalLines) {
            throw new InvalidDomainObjectError("Processed " + processed + " lines out of " + totalLines);
        }
        return new ContractImportProgress(totalLines, processed, importedCount + imported, rejectedCount + rejected);
    }

    public boolean isComplete() {
        return processedLines == totalLines;
    }
}
