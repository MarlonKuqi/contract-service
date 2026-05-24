package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.shared.DomainService;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@DomainService
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    public Client findClientById(final UUID clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client with ID " + clientId + " not found"));
    }

}
