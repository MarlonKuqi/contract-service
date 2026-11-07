package com.mk.contractservice.domain.contract.importjob.exception;

import com.mk.contractservice.domain.contract.importjob.ContractImportJobStatus;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class ContractImportJobTransitionNotAllowedException extends RuntimeException {

    public ContractImportJobTransitionNotAllowedException(@Nullable final UUID jobId, final String transition, final ContractImportJobStatus status) {
        super("Contract import job " + jobId + " cannot " + transition + " while " + status);
    }
}
