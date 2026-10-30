package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.shared.RepositoryPort;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Domain port for Contract aggregate persistence.
 *
 * <p>Manages the lifecycle of {@link Contract} aggregates: creation, retrieval,
 * update, and bulk state transitions. Read-model projections (e.g., cost sums)
 * are handled by {@link ContractQueryPort}.
 *
 * <p>See {@link RepositoryPort} for architectural notes on Spring wiring,
 * the {@code Page}/{@code Pageable} compromise, and the port/adapter pattern.
 */
@RepositoryPort
public interface ContractRepository {
    Contract save(Contract contract);

    Contract getById(UUID contractId);

    Page<Contract> findByCriteria(ContractSearchCriteria criteria, Pageable pageable);

    /**
     * Keyset read for exports: contracts active at {@code activeAt} whose id is strictly greater than
     * {@code afterId} (from the first one when {@code null}), in ascending id order, at most {@code limit}.
     */
    List<Contract> findActiveAfter(LocalDateTime activeAt, @Nullable UUID afterId, int limit);

    int closeAllActiveByClientId(UUID clientId, LocalDateTime closureDate);
}
