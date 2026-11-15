package com.mk.contractservice.features.lamal;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.lamal.LamalAffiliationRepository;
import com.mk.contractservice.domain.lamal.LamalExemption;
import com.mk.contractservice.domain.lamal.exception.LamalExemptionNotFoundException;
import com.mk.contractservice.features.shared.QueryUseCase;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.UUID;

public interface GetLamalExemption {

    record Query(UUID clientId) {
        public Query {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }

    LamalExemption execute(Query query);

    @QueryUseCase
    @RequiredArgsConstructor
    class Handler implements GetLamalExemption {

        private final ClientRepository clientRepository;
        private final LamalAffiliationRepository lamalAffiliationRepository;

        @Override
        public LamalExemption execute(final Query query) {
            clientRepository.ensureExists(query.clientId());
            return lamalAffiliationRepository.findExemption(query.clientId())
                    .orElseThrow(() -> new LamalExemptionNotFoundException(query.clientId()));
        }
    }
}
