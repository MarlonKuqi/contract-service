package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.features.shared.UseCase;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public interface FinishContractImportJob {

    record Command(UUID jobId) {
        public Command {
            Objects.requireNonNull(jobId, "Job ID cannot be null");
        }
    }

    void execute(Command command);

    @UseCase
    @RequiredArgsConstructor
    class Handler implements FinishContractImportJob {

        private final ContractImportJobRepository jobRepository;
        private final Clock clock;

        @Override
        public void execute(final Command command) {
            final LocalDateTime now = LocalDateTime.now(clock);
            final ContractImportJob loaded = jobRepository.getById(command.jobId());
            final ContractImportJob finished = loaded.finish(now);
            if (!finished.equals(loaded)) {
                jobRepository.save(finished);
            }
        }
    }
}
