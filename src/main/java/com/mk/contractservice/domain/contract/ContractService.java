package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.exception.ContractNotFoundException;
import com.mk.contractservice.domain.exception.ContractNotOwnedByClientException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ContractService {

    private final ContractRepository contractRepository;

    public ContractService(final ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    public Contract getContractForClient(final UUID clientId, final UUID contractId) {
        final Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException(contractId));

        ensureContractBelongsToClient(contract, clientId);

        return contract;
    }

    public void ensureContractBelongsToClient(final Contract contract, final UUID clientId) {
        if (!contract.getClient().getId().equals(clientId)) {
            throw new ContractNotOwnedByClientException(contract.getId(), clientId);
        }
    }
}

