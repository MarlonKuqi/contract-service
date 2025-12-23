package com.mk.contractservice.application.contract.usecase;

import com.mk.contractservice.domain.contract.aggregate.Contract;

import java.util.Objects;
import java.util.UUID;

public interface GetContractByIdQuery {

    Contract execute(GetContractQuery query);

    record GetContractQuery(
            UUID clientId,
            UUID contractId
    ) {
        public GetContractQuery {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
            Objects.requireNonNull(contractId, "Contract ID cannot be null");
        }
    }
}

