package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.shared.DomainService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

@DomainService
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;

    public Page<Contract> getActiveContractsForClient(
            final UUID clientId,
            @Nullable final LocalDateTime updatedSince,
            final Pageable pageable
    ) {
        final ContractSearchCriteria criteria = updatedSince == null
                ? ContractSearchCriteria.activeForClient(clientId)
                : ContractSearchCriteria.activeForClientUpdatedSince(clientId, updatedSince);
        return contractRepository.findByCriteria(criteria, pageable);
    }
}
