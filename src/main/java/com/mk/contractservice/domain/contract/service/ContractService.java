package com.mk.contractservice.domain.contract.service;

import com.mk.contractservice.domain.contract.repository.ContractRepository;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.exception.ContractNotOwnedByClientException;
import com.mk.contractservice.domain.contract.valueobject.ContractCost;
import com.mk.contractservice.domain.contract.valueobject.ContractPeriod;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ContractService {

    ContractRepository contractRepository;

    public ContractService(final ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    public void ensureContractBelongsToClient(final Contract contract, final UUID clientId) {
        if (!Objects.equals(contract.getClientId(), clientId)) {
            throw new ContractNotOwnedByClientException(contract.getId(), clientId);
        }
    }

    @Transactional
    public Contract createAndPersistContract(final UUID clientId, final ContractPeriod period, final ContractCost cost) {
        final Contract contract = Contract.of(clientId, period, cost);
        return contractRepository.save(contract);
    }
}

