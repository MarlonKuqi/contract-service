package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.contract.exception.OverlappingContractException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.mk.contractservice.domain.shared.Assert.notNull;

public final class ContractCoverage {

    private record Coverer(UUID clientId, ContractType type) {

        static Coverer of(final Contract contract) {
            return new Coverer(Objects.requireNonNull(contract.clientId()), contract.type());
        }
    }

    private final Map<Coverer, List<ContractPeriod>> periodsByCoverer = new HashMap<>();
    private final LocalDateTime now;

    private ContractCoverage(final LocalDateTime now) {
        this.now = notNull(now);
    }

    public static ContractCoverage of(final Collection<Contract> contracts, final LocalDateTime now) {
        final ContractCoverage coverage = new ContractCoverage(now);
        notNull(contracts).stream()
                .filter(coverage::blocksOverlap)
                .forEach(coverage::register);
        return coverage;
    }

    public void add(final Contract contract) {
        if (!notNull(contract).type().forbidsOverlap()) {
            return;
        }
        final Coverer coverer = Coverer.of(contract);
        findAnyOverlap(coverer, contract.period()).ifPresent(period -> {
            throw new OverlappingContractException(coverer.clientId(), coverer.type(), period);
        });
        if (contract.isActiveAt(now)) {
            register(contract);
        }
    }

    private Optional<ContractPeriod> findAnyOverlap(final Coverer coverer, final ContractPeriod period) {
        return periodsByCoverer.getOrDefault(coverer, List.of())
                .stream()
                .filter(covered -> covered.overlaps(period))
                .findAny();
    }

    private boolean blocksOverlap(final Contract contract) {
        return contract.type().forbidsOverlap() && contract.isActiveAt(now);
    }

    private void register(final Contract contract) {
        periodsByCoverer.computeIfAbsent(Coverer.of(contract), _ -> new ArrayList<>()).add(contract.period());
    }
}
