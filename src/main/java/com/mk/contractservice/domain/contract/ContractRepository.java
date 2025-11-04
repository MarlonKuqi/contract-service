package com.mk.contractservice.domain.contract;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository {
    Contract save(Contract contract);

    Optional<Contract> findById(final UUID id);

    List<Contract> findActiveByClientId(UUID clientId, LocalDateTime now, LocalDateTime updatedSince);

    void closeAllActiveByClientId(UUID clientId, LocalDateTime now);

    BigDecimal sumActiveByClientId(UUID clientId, LocalDateTime now);
}
