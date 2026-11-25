package com.mk.contractservice.domain.contract.importjob.exception;

import java.util.UUID;

public class ContractImportJobNotFoundException extends RuntimeException {

    public ContractImportJobNotFoundException(final UUID jobId) {
        super("Contract import job not found with id: " + jobId);
    }
}
