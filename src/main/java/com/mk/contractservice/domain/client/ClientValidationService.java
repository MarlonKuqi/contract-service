package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.client.exception.CompanyIdentifierAlreadyExistsException;
import com.mk.contractservice.domain.client.exception.EmailAlreadyExistsException;
import com.mk.contractservice.domain.client.exception.PhoneAlreadyExistsException;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientValidationService {

    private final ClientRepository clientRepository;

    public void ensureEmailIsUnique(final String email) {
        if (clientRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("A Client with email '" + email + "' already exists", email);
        }
    }

    public void ensurePhoneIsUnique(final String phoneNumber) {
        if (clientRepository.existsByPhoneNumber(phoneNumber)) {
            throw new PhoneAlreadyExistsException(
                    "A client with phone number '" + phoneNumber + "' already exists",
                    phoneNumber
            );
        }
    }

    public void ensureCompanyIdentifierIsUnique(final String identifier) {
        if (clientRepository.existsByCompanyIdentifier(identifier)) {
            throw new CompanyIdentifierAlreadyExistsException(
                    "A company with identifier '" + identifier + "' already exists",
                    identifier
            );
        }
    }

    public void ensureClientExists(final UUID clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw ClientNotFoundException.forId(clientId);
        }
    }
}

