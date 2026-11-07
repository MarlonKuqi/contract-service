package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public interface CancelContractImportJob {

    record Command(UUID jobId, String cancelledBy) {
        public Command {
            Objects.requireNonNull(jobId, "Job ID cannot be null");
            Objects.requireNonNull(cancelledBy, "Counselor cannot be null");
        }
    }

    record Result(ContractImportJob job, int closedContracts) {
    }

    Result execute(Command command);

    @UseCase
    @Transactional
    @RequiredArgsConstructor
    class Handler implements CancelContractImportJob {

        private final ContractImportJobRepository jobRepository;
        private final ContractRepository contractRepository;
        private final Clock clock;

        @Override
        public Result execute(final Command command) {
            final LocalDateTime now = LocalDateTime.now(clock);
            final ContractImportJob loaded = jobRepository.lockById(command.jobId());
            final ContractImportJob cancelled = loaded.cancel(now, command.cancelledBy());
            if (cancelled == loaded) {
                return new Result(loaded, 0);
            }
            final ContractImportJob saved = jobRepository.save(cancelled);
            final int closedContracts = contractRepository.closeAllActiveImportedBy(command.jobId(), now);
            return new Result(saved, closedContracts);
        }
    }
}
