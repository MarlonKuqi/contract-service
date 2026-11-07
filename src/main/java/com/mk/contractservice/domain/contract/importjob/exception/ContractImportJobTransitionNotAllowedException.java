package com.mk.contractservice.domain.contract.importjob.exception;

import com.mk.contractservice.domain.contract.importjob.ContractImportJobState;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class ContractImportJobTransitionNotAllowedException extends RuntimeException {

    public ContractImportJobTransitionNotAllowedException(@Nullable final UUID jobId, final String transition, final ContractImportJobState.Status status) {
        super("Contract import job " + jobId + " cannot " + transition + " while " + status);
    }
}
