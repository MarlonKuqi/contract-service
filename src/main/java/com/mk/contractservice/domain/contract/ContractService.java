package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.exception.ContractNotOwnedByClientException;
import com.mk.contractservice.domain.exception.ExpiredContractException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class ContractService {

    public void ensureContractBelongsToClient(final Contract contract, final UUID clientId) {
        if (!Objects.equals(contract.getClient().getId(), clientId)) {
            throw new ContractNotOwnedByClientException(contract.getId(), clientId);
        }
    }

    public void ensureContractIsActive(final Contract contract) {
        if (contract.isInactive()) {
            throw new ExpiredContractException(contract.getId());
        }
    }

    public Contract closeContract(final Contract contract) {
        ensureContractIsActive(contract);

        final LocalDateTime now = LocalDateTime.now();
        return contract.close(now);
    }
}

