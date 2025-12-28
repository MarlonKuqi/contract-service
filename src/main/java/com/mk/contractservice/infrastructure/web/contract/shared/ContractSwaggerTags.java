package com.mk.contractservice.infrastructure.web.contract.shared;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContractSwaggerTags {

    public static final String NAME = "Contracts";
    public static final String DESCRIPTION = "Operations on contracts (create, read, update, renew, close)";
}

