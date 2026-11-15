package com.mk.contractservice.features.client;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientEmail;
import com.mk.contractservice.domain.client.ClientName;
import com.mk.contractservice.domain.client.ClientPhoneNumber;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.lamal.LamalAffiliationRepository;
import com.mk.contractservice.domain.shared.Residence;
import com.mk.contractservice.features.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public interface PatchClient {

    record Command(
            UUID clientId,
            long expectedVersion,
            @Nullable String name,
            @Nullable String email,
            @Nullable String phoneNumber,
            @Nullable Residence residence
    ) {
        public Command {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }

    Client execute(Command command);

    @UseCase
    @RequiredArgsConstructor
    class Handler implements PatchClient {

        private final ClientRepository clientRepository;
        private final LamalAffiliationRepository lamalAffiliationRepository;
        private final Clock clock;

        @Override
        public Client execute(final Command command) {
            final Client client = clientRepository.getById(command.clientId());
            clientRepository.ensureVersion(command.clientId(), command.expectedVersion());
            final ClientName name = command.name() == null ? null : ClientName.of(command.name());
            final ClientEmail email = command.email() == null ? null : ClientEmail.of(command.email());
            final ClientPhoneNumber phone = command.phoneNumber() == null ? null : ClientPhoneNumber.of(command
                    .phoneNumber());

            Client updatedClient = client;
            if (name != null) {
                updatedClient = updatedClient.changeName(name);
            }
            if (email != null) {
                updatedClient = updatedClient.changeEmail(email);
            }
            if (phone != null) {
                updatedClient = updatedClient.changePhone(phone);
            }
            final Residence residence = command.residence();
            if (residence != null && updatedClient instanceof Person person) {
                updatedClient = person.changeResidence(residence);
            }
            if (updatedClient.equals(client)) {
                return client;
            }
            if (!updatedClient.contractHolder().equals(client.contractHolder())) {
                lamalAffiliationRepository.loadForUpdate(command.clientId())
                        .ensureAcceptsHolder(updatedClient.contractHolder(), LocalDateTime.now(clock));
            }
            return clientRepository.save(updatedClient);
        }
    }
}
