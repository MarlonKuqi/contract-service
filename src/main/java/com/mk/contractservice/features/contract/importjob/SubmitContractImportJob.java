package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportSubmission;
import com.mk.contractservice.domain.contract.importjob.IdempotencyKey;
import com.mk.contractservice.domain.contract.importjob.exception.IdempotencyKeyReusedException;
import com.mk.contractservice.features.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public interface SubmitContractImportJob {

    record Command(
            ContractImportFile file,
            String submittedBy,
            @Nullable String idempotencyKey
    ) {

        public Command {
            Objects.requireNonNull(file, "File cannot be null");
            Objects.requireNonNull(submittedBy, "Submitter cannot be null");
        }

        public Command(
                @Nullable final String fileName,
                final byte[] content,
                final String submittedBy,
                @Nullable final String idempotencyKey
        ) {
            this(ContractImportFile.of(fileName, content), submittedBy, idempotencyKey);
        }
    }

    ContractImportJob execute(Command command);

    @UseCase
    @RequiredArgsConstructor
    class Handler implements SubmitContractImportJob {

        private final ContractImportJobRepository jobRepository;
        private final Clock clock;

        @Override
        public ContractImportJob execute(final Command command) {
            final ContractImportFile file = command.file();
            final String fingerprint = file.fingerprint();
            final Optional<IdempotencyKey> key = Optional.ofNullable(command.idempotencyKey()).map(IdempotencyKey::of);
            return key
                    .flatMap(requested -> jobRepository.findByIdempotencyKey(command.submittedBy(), requested)
                            .map(previous -> replay(previous, requested, fingerprint)))
                    .orElseGet(() -> saveNewJob(file, command.submittedBy(), key.orElse(null), fingerprint));
        }

        private ContractImportJob saveNewJob(
                final ContractImportFile file,
                final String submittedBy,
                @Nullable final IdempotencyKey key,
                final String fingerprint
        ) {
            final ContractImportSubmission submission = ContractImportSubmission.of(
                    file.name(),
                    submittedBy,
                    key,
                    fingerprint,
                    LocalDateTime.now(clock)
            );
            return jobRepository.save(ContractImportJob.of(submission, file.totalLines()), file.content());
        }

        private static ContractImportJob replay(
                final ContractImportJob previous,
                final IdempotencyKey key,
                final String fingerprint
        ) {
            if (!previous.hasContent(fingerprint)) {
                throw new IdempotencyKeyReusedException(key.value());
            }
            return previous;
        }
    }
}
