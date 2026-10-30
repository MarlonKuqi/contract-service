package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.client.exception.CompanyIdentifierAlreadyExistsException;
import com.mk.contractservice.domain.shared.DomainService;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class CompanyIdentifierUniqueness {

    private final ClientRepository clientRepository;

    public void ensureIsUnique(final CompanyIdentifier identifier) {
        final String value = identifier.getValue();
        if (clientRepository.existsByCompanyIdentifier(value)) {
            throw new CompanyIdentifierAlreadyExistsException(
                    "A company with identifier '" + value + "' already exists",
                    value
            );
        }
    }
}
