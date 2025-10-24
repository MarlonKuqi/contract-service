package com.mk.contractservice.domain.contract;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository {
    Contract save(Contract contract);

    Optional<Contract> findById(final UUID id);

    List<Contract> findActiveByClientId(final UUID clientId, OffsetDateTime now, OffsetDateTime updatedSince);

    void closeAllActiveByClientId(final UUID clientId, OffsetDateTime now);

    BigDecimal sumActiveByClientId(final UUID clientId, OffsetDateTime now);
}
