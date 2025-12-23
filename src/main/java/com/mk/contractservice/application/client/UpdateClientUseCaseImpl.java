package com.mk.contractservice.application.client;

import com.mk.contractservice.application.client.usecase.UpdateClientUseCase;
import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import com.mk.contractservice.domain.client.service.ClientService;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class UpdateClientUseCaseImpl implements UpdateClientUseCase {

    ClientRepository clientRepository;
    ClientService clientService;

    @Override
    public Client execute(UpdateClientCommand command) {
        final Client client = clientService.findClientById(command.clientId());

        final ClientName name = ClientName.of(command.name());
        final ClientEmail email = ClientEmail.of(command.email());
        final ClientPhoneNumber phone = ClientPhoneNumber.of(command.phoneNumber());

        final Client updatedClient = client.withCommonFields(name, email, phone);
        return clientRepository.save(updatedClient);
    }
}

