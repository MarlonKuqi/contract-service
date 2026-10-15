package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.client.exception.CompanyIdentifierAlreadyExistsException;
import com.mk.contractservice.domain.shared.DomainService;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@DomainService
@RequiredArgsConstructor
public class ClientValidationService {

    private final ClientRepository clientRepository;

    public void ensureCompanyIdentifierIsUnique(final CompanyIdentifier identifier) {
        final String value = identifier.getValue();
        if (clientRepository.existsByCompanyIdentifier(value)) {
            throw new CompanyIdentifierAlreadyExistsException(
                    "A company with identifier '" + value + "' already exists",
                    value
            );
        }
    }

    public void ensureClientExists(final UUID clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw ClientNotFoundException.forId(clientId);
        }
    }
}
