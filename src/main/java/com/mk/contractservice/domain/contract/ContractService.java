package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.contract.exception.ContractNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ContractService {

    ContractRepository contractRepository;
    ContractValidationService contractValidationService;

    @Transactional(readOnly = true)
    public Contract getContractForClient(final UUID clientId, final UUID contractId) {
        final Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException(contractId));
        contractValidationService.ensureContractBelongsToClient(contract, clientId);
        return contract;
    }

    @Transactional(readOnly = true)
    public Page<Contract> getActiveContractsForClient(
            final UUID clientId,
            final LocalDateTime updatedSince,
            final Pageable pageable
    ) {
        return contractRepository.findActiveByClientIdPageable(clientId, updatedSince, pageable);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalCostAmountForClient(final UUID clientId) {
        return contractRepository.calculateTotalCostAmountForClient(clientId);
    }
}
