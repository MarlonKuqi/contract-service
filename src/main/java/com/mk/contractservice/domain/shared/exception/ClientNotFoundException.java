package com.mk.contractservice.domain.shared.exception;

import java.util.UUID;

public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(String message) {
        super(message);
    }

    public static ClientNotFoundException forId(UUID clientId) {
        return new ClientNotFoundException(messageFor(clientId));
    }

    public static String messageFor(UUID clientId) {
        return "Client not found with id: " + clientId;
    }
}

