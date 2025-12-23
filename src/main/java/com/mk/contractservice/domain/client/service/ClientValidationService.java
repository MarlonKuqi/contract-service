package com.mk.contractservice.domain.client.service;

import com.mk.contractservice.domain.client.exception.ClientAlreadyExistsException;
import com.mk.contractservice.domain.client.exception.CompanyIdentifierAlreadyExistsException;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClientValidationService {

    ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public void ensureEmailIsUnique(final ClientEmail email) {
        if (clientRepository.existsByEmail(email)) {
            throw new ClientAlreadyExistsException("Client already exists", email.value());
        }
    }

    @Transactional(readOnly = true)
    public void ensureCompanyIdentifierIsUnique(final CompanyIdentifier identifier) {
        if (clientRepository.existsByCompanyIdentifier(identifier)) {
            throw new CompanyIdentifierAlreadyExistsException(
                    "A company with identifier '" + identifier.value() + "' already exists",
                    identifier.value()
            );
        }
    }
}

