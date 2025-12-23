package com.mk.contractservice.application.client;

import com.mk.contractservice.application.client.usecase.GetClientByIdQuery;
import com.mk.contractservice.domain.client.aggregate.Client;
import com.mk.contractservice.domain.client.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class GetClientByIdQueryImpl implements GetClientByIdQuery {

    ClientService clientService;

    @Override
    public Client execute(GetClientQuery query) {
        return clientService.findClientById(query.clientId());
    }
}

