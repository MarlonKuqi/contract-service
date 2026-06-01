package com.mk.contractservice.controllers.contract.shared;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContractEndpoints {

    // Base paths
    public static final String API_VERSION = "/v2";
    public static final String CONTRACTS_BASE = API_VERSION + "/contracts";

    // Path variable names
    public static final String CONTRACT_ID = "contractId";

    // Path variables
    public static final String PATH_VAR_CONTRACT_ID = "/{" + CONTRACT_ID + "}";

    // Relative paths (for use in @RequestMapping controllers that already have base path)
    public static final String RELATIVE_CONTRACT_COST = PATH_VAR_CONTRACT_ID + "/cost";

    // Composed paths (full paths for tests and API documentation)
    public static final String CONTRACT_BY_ID = CONTRACTS_BASE + PATH_VAR_CONTRACT_ID;
    public static final String CONTRACT_COST = CONTRACTS_BASE + RELATIVE_CONTRACT_COST;
    public static final String CONTRACT_TOTAL = "/total";
}
