package com.mk.contractservice.domain.contract.importjob.exception;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class ContractImportJobCancelledException extends RuntimeException {

    public ContractImportJobCancelledException(@Nullable final UUID jobId) {
        super("Contract import job " + jobId + " was cancelled");
    }
}
