package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.shared.DomainService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@DomainService
@RequiredArgsConstructor
public class ContractOverlapPolicy {

    private final ContractRepository contractRepository;

    public ContractCoverage coverageOf(final Set<UUID> clientIds, final LocalDateTime now) {
        if (clientIds.isEmpty()) {
            return ContractCoverage.of(List.of(), now);
        }
        contractRepository.lockCoverageOf(clientIds);
        return ContractCoverage.of(contractRepository.findAllByClientIdsAndTypes(clientIds,
                ContractType.forbiddingOverlap()), now);
    }

    public void ensureNoOverlap(final Contract contract, final LocalDateTime now) {
        if (contract.type().forbidsOverlap()) {
            coverageOf(Set.of(Objects.requireNonNull(contract.clientId())), now).add(contract);
        }
    }
}
