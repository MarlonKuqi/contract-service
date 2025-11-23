package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.exception.ContractNotOwnedByClientException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class ContractService {

    public void ensureContractBelongsToClient(final Contract contract, final UUID clientId) {
        if (!Objects.equals(contract.getClient().getId(), clientId)) {
            throw new ContractNotOwnedByClientException(contract.getId(), clientId);
        }
    }
}

