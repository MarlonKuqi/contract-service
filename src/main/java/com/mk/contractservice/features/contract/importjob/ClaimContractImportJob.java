package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface ClaimContractImportJob {

    Duration LEASE = Duration.ofMinutes(5);

    Optional<ContractImportJob> execute();

    @UseCase
    @Transactional
    @RequiredArgsConstructor
    class Handler implements ClaimContractImportJob {

        private final ContractImportJobRepository jobRepository;
        private final Clock clock;

        @Override
        public Optional<ContractImportJob> execute() {
            final Instant now = clock.instant();
            return jobRepository
                    .findNextToRun(now)
                    .map(job -> jobRepository.save(job.start(now.plus(LEASE))));
        }
    }
}
