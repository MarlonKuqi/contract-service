package com.mk.contractservice.application.contract.usecase;

import java.util.Objects;
import java.util.UUID;

public interface CloseActiveContractsUseCase {
    void execute(CloseActiveContractsCommand command);

    record CloseActiveContractsCommand(UUID clientId) {
        public CloseActiveContractsCommand {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }
}
