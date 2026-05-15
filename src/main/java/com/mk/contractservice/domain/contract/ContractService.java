package com.mk.contractservice.domain.contract;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;

    @Transactional(readOnly = true)
    public Page<Contract> getActiveContractsForClient(
            final UUID clientId,
            @Nullable final LocalDateTime updatedSince,
            final Pageable pageable
    ) {
        return contractRepository.findActiveByClientIdPageable(clientId, updatedSince, pageable);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalCostAmountForClient(final UUID clientId) {
        return contractRepository.calculateTotalCostAmountForClient(clientId);
    }
}
