package com.mk.contractservice.application.client;

import com.mk.contractservice.application.client.usecase.DeleteClientUseCase;
import com.mk.contractservice.domain.client.event.ClientDeletedEvent;
import com.mk.contractservice.domain.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class DeleteClientUseCaseImpl implements DeleteClientUseCase {

    ClientRepository clientRepository;
    ApplicationEventPublisher eventPublisher;

    @Override
    public void execute(DeleteClientCommand command) {
        clientRepository.deleteById(command.clientId());
        eventPublisher.publishEvent(ClientDeletedEvent.of(command.clientId()));
    }
}

