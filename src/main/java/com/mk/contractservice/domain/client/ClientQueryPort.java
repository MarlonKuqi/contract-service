package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.shared.RepositoryPort;

import java.util.UUID;

@RepositoryPort
public interface ClientQueryPort {

    ClientDetails getDetails(UUID clientId);
}
