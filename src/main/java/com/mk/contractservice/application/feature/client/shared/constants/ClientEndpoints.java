package com.mk.contractservice.application.feature.client.shared.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientEndpoints {

    // Base paths
    public static final String API_VERSION = "/v2";
    public static final String CLIENTS_BASE = API_VERSION + "/clients";

    // Path variables
    public static final String PATH_VAR_ID = "/{id}";

    // Composed paths
    public static final String CLIENT_BY_ID = CLIENTS_BASE + PATH_VAR_ID;
}
