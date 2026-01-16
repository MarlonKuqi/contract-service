package com.mk.contractservice.domain.contract.repository;

import com.mk.contractservice.domain.contract.aggregate.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository {
    Contract save(Contract contract);

    Optional<Contract> findById(UUID contractId);

    Page<Contract> findActiveByClientIdPageable(UUID clientId, Optional<LocalDateTime> updatedSince, Pageable pageable);

    BigDecimal sumActiveByClientId(UUID clientId);

    int closeAllActiveByClientId(UUID clientId, LocalDateTime closureDate);
}

