package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.features.shared.UseCase;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.UUID;

public interface ReleaseContractImportJob {

    record Command(UUID jobId) {
        public Command {
            Objects.requireNonNull(jobId, "Job ID cannot be null");
        }
    }

    void execute(Command command);

    @UseCase
    @RequiredArgsConstructor
    class Handler implements ReleaseContractImportJob {

        private final ContractImportJobRepository jobRepository;

        @Override
        public void execute(final Command command) {
            final ContractImportJob loaded = jobRepository.getById(command.jobId());
            final ContractImportJob released = loaded.release();
            if (!released.equals(loaded)) {
                jobRepository.save(released);
            }
        }
    }
}
