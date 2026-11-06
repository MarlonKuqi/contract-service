package com.mk.contractservice.domain.contract;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContractImportRejectionReason {
    INVALID_LINE_FORMAT("invalidLineFormat"),
    INVALID_COST("invalidContractCost"),
    INVALID_PERIOD("invalidContractPeriod"),
    CLIENT_NOT_FOUND("clientNotFound");

    private final String code;
}
