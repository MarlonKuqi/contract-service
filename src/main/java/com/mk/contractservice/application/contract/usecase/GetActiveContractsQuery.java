package com.mk.contractservice.application.contract.usecase;

import com.mk.contractservice.domain.contract.aggregate.Contract;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public interface GetActiveContractsQuery {

    Page<Contract> execute(GetActiveContractsQueryParams query);

    record GetActiveContractsQueryParams(
            UUID clientId,
            @Nullable LocalDateTime updatedSince,
            Pageable pageable
    ) {
        public GetActiveContractsQueryParams {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
            Objects.requireNonNull(pageable, "Pageable cannot be null");
        }
    }
}

