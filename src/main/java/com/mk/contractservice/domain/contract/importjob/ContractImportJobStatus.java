package com.mk.contractservice.domain.contract.importjob;

public enum ContractImportJobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED;

    public boolean isNotRunning() {
        return this != RUNNING;
    }
}
