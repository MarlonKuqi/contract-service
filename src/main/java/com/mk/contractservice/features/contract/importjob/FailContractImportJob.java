package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportFailureReason;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public interface FailContractImportJob {

    record Command(UUID jobId, ContractImportFailureReason reason) {
        public Command {
            Objects.requireNonNull(jobId, "Job ID cannot be null");
            Objects.requireNonNull(reason, "Failure reason cannot be null");
        }
    }

    void execute(Command command);

    @UseCase
    @Transactional
    @RequiredArgsConstructor
    class Handler implements FailContractImportJob {

        private final ContractImportJobRepository jobRepository;
        private final Clock clock;

        @Override
        public void execute(final Command command) {
            final LocalDateTime now = LocalDateTime.now(clock);
            jobRepository.save(jobRepository.getById(command.jobId()).fail(command.reason(), now));
        }
    }
}
