package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClientService {

    ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public Client findClientById(final UUID clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client with ID " + clientId + " not found"));
    }

}
