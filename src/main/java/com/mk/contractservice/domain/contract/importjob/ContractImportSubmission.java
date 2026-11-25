package com.mk.contractservice.domain.contract.importjob;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

import static com.mk.contractservice.domain.shared.Assert.notBlank;
import static com.mk.contractservice.domain.shared.Assert.notNull;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ContractImportSubmission {

    String fileName;

    @Nullable
    String submittedBy;

    @Nullable
    IdempotencyKey idempotencyKey;

    String contentSha256;

    LocalDateTime submittedAt;

    public static ContractImportSubmission of(
            @Nullable final String fileName,
            @Nullable final String submittedBy,
            @Nullable final IdempotencyKey idempotencyKey,
            @Nullable final String contentSha256,
            @Nullable final LocalDateTime submittedAt
    ) {
        return new ContractImportSubmission(
                notBlank(fileName),
                notBlank(submittedBy),
                idempotencyKey,
                notNull(contentSha256),
                notNull(submittedAt)
        );
    }

    public static ContractImportSubmission reconstituteFromDatabase(
            @Nullable final String fileName,
            @Nullable final String submittedBy,
            @Nullable final IdempotencyKey idempotencyKey,
            @Nullable final String contentSha256,
            @Nullable final LocalDateTime submittedAt
    ) {
        return new ContractImportSubmission(
                notNull(fileName),
                submittedBy,
                idempotencyKey,
                notNull(contentSha256),
                notNull(submittedAt)
        );
    }

    public boolean hasContent(final String sha256) {
        return contentSha256.equals(sha256);
    }
}
