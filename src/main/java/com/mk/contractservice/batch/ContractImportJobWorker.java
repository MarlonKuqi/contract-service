package com.mk.contractservice.batch;

import com.mk.contractservice.features.contract.importjob.ClaimContractImportJob;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.contract-import.worker-enabled", matchIfMissing = true)
@RequiredArgsConstructor
public class ContractImportJobWorker {

    private final ClaimContractImportJob claimContractImportJob;
    private final ContractImportJobRunner contractImportJobRunner;

    @Scheduled(fixedDelayString = "${app.contract-import.poll-delay}")
    public void runNextJob() {
        claimContractImportJob.execute().ifPresent(job -> contractImportJobRunner.run(job.getId()));
    }
}
