package com.mk.contractservice.controllers.client.shared;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientEndpoints {

    // Base paths
    public static final String API_VERSION = "/v2";
    public static final String CLIENTS_BASE = API_VERSION + "/clients";

    // Path variable names
    public static final String CLIENT_ID = "id";

    // Path variables
    public static final String PATH_VAR_ID = "/{" + CLIENT_ID + "}";

    // Composed paths
    public static final String CLIENT_BY_ID = CLIENTS_BASE + PATH_VAR_ID;
}
