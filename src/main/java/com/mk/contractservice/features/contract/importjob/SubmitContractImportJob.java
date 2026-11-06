package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportSubmission;
import com.mk.contractservice.domain.contract.importjob.IdempotencyKey;
import com.mk.contractservice.domain.contract.importjob.exception.IdempotencyKeyReusedException;
import com.mk.contractservice.domain.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public interface SubmitContractImportJob {

    record Command(
            @Nullable String fileName,
            byte[] content,
            String submittedBy,
            @Nullable String idempotencyKey
    ) {

        public Command {
            Objects.requireNonNull(content, "Content cannot be null");
            Objects.requireNonNull(submittedBy, "Submitter cannot be null");
        }
    }

    ContractImportJob execute(Command command);

    @UseCase
    @Transactional
    @RequiredArgsConstructor
    class Handler implements SubmitContractImportJob {

        private final ContractImportJobRepository jobRepository;
        private final Clock clock;

        @Override
        public ContractImportJob execute(final Command command) {
            final ContractImportFile file = ContractImportFile.of(command.fileName(), command.content());
            final String fingerprint = file.fingerprint();
            final Optional<IdempotencyKey> key = Optional.ofNullable(command.idempotencyKey()).map(IdempotencyKey::of);
            return key
                    .flatMap(requested -> jobRepository.findByIdempotencyKey(command.submittedBy(), requested)
                            .map(previous -> replay(previous, requested, fingerprint)))
                    .orElseGet(() -> insertNewJob(file, command.submittedBy(), key.orElse(null), fingerprint));
        }

        private ContractImportJob insertNewJob(
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
            return jobRepository.insert(ContractImportJob.of(submission, file.totalLines()), file.content());
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
