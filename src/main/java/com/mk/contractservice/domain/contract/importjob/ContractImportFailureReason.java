package com.mk.contractservice.domain.contract.importjob;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContractImportFailureReason {
    UNEXPECTED_ERROR("unexpectedError"),
    INCOMPLETE_PROCESSING("incompleteProcessing"),
    TOO_MANY_ATTEMPTS("tooManyAttempts");

    private final String code;
}
