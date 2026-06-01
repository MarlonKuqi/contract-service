package com.mk.contractservice.domain.shared.exception;

import java.util.UUID;

public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(String message) {
        super(message);
    }

    public static ClientNotFoundException forId(UUID clientId) {
        return new ClientNotFoundException("Client not found with id: " + clientId);
    }
}

