package com.mk.contractservice.controllers.contract.shared;

import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import com.mk.contractservice.domain.contract.importjob.ContractImportFailureReason;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContractImportCodes {

    public static String of(final ContractImportRejectionReason reason) {
        return switch (reason) {
            case INVALID_LINE_FORMAT -> "invalidLineFormat";
            case INVALID_COST -> "invalidContractCost";
            case INVALID_PERIOD -> "invalidContractPeriod";
            case CLIENT_NOT_FOUND -> "clientNotFound";
            case TYPE_NOT_ALLOWED -> "contractTypeNotAllowed";
            case INVALID_FRANCHISE -> "invalidContractFranchise";
            case OVERLAPPING_CONTRACT -> "contractOverlap";
        };
    }

    public static String of(final ContractImportFailureReason reason) {
        return switch (reason) {
            case UNEXPECTED_ERROR -> "unexpectedError";
            case INCOMPLETE_PROCESSING -> "incompleteProcessing";
            case TOO_MANY_ATTEMPTS -> "tooManyAttempts";
        };
    }
}
