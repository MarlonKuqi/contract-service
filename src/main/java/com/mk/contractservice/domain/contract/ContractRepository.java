package com.mk.contractservice.domain.contract;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository {
    Contract save(Contract contract);

    Optional<Contract> findById(UUID contractId);

    Page<Contract> findActiveByClientIdPageable(UUID clientId, LocalDateTime now, LocalDateTime updatedSince, Pageable pageable);

    void closeAllActiveByClientId(UUID clientId, LocalDateTime now);

    BigDecimal sumActiveByClientId(UUID clientId, LocalDateTime now);
}
