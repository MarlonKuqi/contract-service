package com.mk.contractservice.domain.contract.service;

import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.domain.contract.exception.ContractNotOwnedByClientException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContractValidationService {

    public void ensureContractBelongsToClient(final Contract contract, final UUID clientId) {
        if (!Objects.equals(contract.getClientId(), clientId)) {
            throw new ContractNotOwnedByClientException(contract.getId(), clientId);
        }
    }
}
