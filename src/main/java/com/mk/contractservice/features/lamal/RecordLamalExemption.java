package com.mk.contractservice.features.lamal;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.lamal.LamalAffiliation;
import com.mk.contractservice.domain.lamal.LamalAffiliationRepository;
import com.mk.contractservice.domain.lamal.LamalExemption;
import com.mk.contractservice.domain.shared.ContractHolder;
import com.mk.contractservice.features.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public interface RecordLamalExemption {

    record Command(
            UUID clientId,
            @Nullable LocalDate startDate,
            @Nullable LocalDate endDate
    ) {
        public Command {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
        }
    }

    LamalExemption execute(Command command);

    @UseCase
    @RequiredArgsConstructor
    class Handler implements RecordLamalExemption {

        private final ClientRepository clientRepository;
        private final LamalAffiliationRepository lamalAffiliationRepository;
        private final Clock clock;

        @Override
        @Retryable(
                includes = OptimisticLockingFailureException.class,
                maxRetries = 3,
                delay = 20,
                jitter = 10
        )
        public LamalExemption execute(final Command command) {
            final ContractHolder holder = clientRepository.getById(command.clientId()).contractHolder();
            final LamalExemption exemption = LamalExemption.of(command.startDate(), command.endDate(), holder);

            final LamalAffiliation affiliation = lamalAffiliationRepository.loadForUpdate(command.clientId());
            final LamalAffiliation updated = affiliation.recordExemption(exemption, LocalDateTime.now(clock));
            if (!updated.equals(affiliation)) {
                lamalAffiliationRepository.save(updated);
            }
            return exemption;
        }
    }
}
