package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.contract.exception.OverlappingContractException;
import com.mk.contractservice.domain.shared.DomainService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.mk.contractservice.domain.shared.Assert.notNull;

@DomainService
@RequiredArgsConstructor
public class ContractOverlapPolicy {

    private final ContractRepository contractRepository;

    public Batch startBatch(final Set<UUID> clientIds, final LocalDateTime now) {
        return new Batch(lockAndLoad(clientIds), now);
    }

    public void ensureNoOverlap(final Contract contract, final LocalDateTime now) {
        final List<Contract> others = lockAndLoad(Set.of(Objects.requireNonNull(contract.clientId()))).stream()
                .filter(other -> isAnotherContract(other, contract))
                .toList();
        new Batch(others, now).admit(contract);
    }

    private List<Contract> lockAndLoad(final Set<UUID> clientIds) {
        return contractRepository.lockAndFindAllByClientIdsAndTypes(clientIds, ContractType.forbiddingOverlap());
    }

    private static boolean isAnotherContract(final Contract other, final Contract contract) {
        return contract.id() == null || !contract.id().equals(other.id());
    }

    public static final class Batch {

        private record Coverer(UUID clientId, ContractType type) {

            static Coverer of(final Contract contract) {
                return new Coverer(Objects.requireNonNull(contract.clientId()), contract.type());
            }
        }

        private final Map<Coverer, List<ContractPeriod>> periodsByCoverer = new HashMap<>();
        private final LocalDateTime now;

        private Batch(final Collection<Contract> contracts, final LocalDateTime now) {
            this.now = notNull(now);
            notNull(contracts).stream()
                    .filter(this::blocksOverlap)
                    .forEach(this::register);
        }

        public void admit(final Contract contract) {
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
}
