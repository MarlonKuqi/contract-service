package com.mk.contractservice.application.contract.usecase;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public interface SumActiveContractsQuery {

    BigDecimal execute(SumActiveContractsQueryParams query);

    record SumActiveContractsQueryParams(UUID clientId) {
        public SumActiveContractsQueryParams {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }
}

