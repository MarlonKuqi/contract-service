package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.contract.exception.ContractImportRejectedException;
import com.mk.contractservice.domain.contract.exception.ContractImportRejectedException.LineRejection;
import com.mk.contractservice.domain.contract.exception.InvalidImportSizeException;
import com.mk.contractservice.domain.lamal.LamalAffiliationRepository;
import com.mk.contractservice.domain.lamal.LamalAffiliations;
import com.mk.contractservice.features.shared.ContractImportLineEvaluator;
import com.mk.contractservice.features.shared.ContractImportLineEvaluator.Accepted;
import com.mk.contractservice.features.shared.ContractImportLineEvaluator.Rejected;
import com.mk.contractservice.features.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public interface ImportContracts {

    int MAX_LINES = 1000;

    record Line(
            UUID clientId,
            ContractType type,
            @Nullable Integer franchise,
            @Nullable LocalDateTime startDate,
            @Nullable LocalDateTime endDate,
            BigDecimal costAmount
    ) {
        public Line {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
            Objects.requireNonNull(type, "Contract type cannot be null");
            Objects.requireNonNull(costAmount, "Cost amount cannot be null");
        }
    }

    record Command(List<Line> lines) {
        public Command {
            Objects.requireNonNull(lines, "Lines cannot be null");
            if (lines.isEmpty()) {
                throw new InvalidImportSizeException("An import must contain at least one contract");
            }
            if (lines.size() > MAX_LINES) {
                throw new InvalidImportSizeException(
                        "An import must not exceed " + MAX_LINES + " contracts, but had: " + lines.size());
            }
            lines = List.copyOf(lines);
        }

        Set<UUID> clientIds() {
            return lines.stream().map(Line::clientId).collect(Collectors.toSet());
        }
    }

    List<Contract> execute(Command command);

    @UseCase
    @RequiredArgsConstructor
    class Handler implements ImportContracts {

        private final ClientRepository clientRepository;
        private final ContractRepository contractRepository;
        private final LamalAffiliationRepository lamalAffiliationRepository;
        private final Clock clock;

        private record LineProcessResult(List<Contract> contracts, List<LineRejection> rejections) {
            boolean hasRejections() {
                return !rejections.isEmpty();
            }
        }

        @Override
        @Retryable(
                includes = OptimisticLockingFailureException.class,
                maxRetries = 3,
                delay = 20,
                jitter = 10
        )
        public List<Contract> execute(final Command command) {
            final LocalDateTime now = LocalDateTime.now(clock);

            final LineProcessResult processResult = processLines(command, now);

            if (processResult.hasRejections()) {
                throw new ContractImportRejectedException(processResult.rejections());
            }
            return contractRepository.saveAll(processResult.contracts());
        }

        private LineProcessResult processLines(final Command command, final LocalDateTime now) {
            final List<Contract> contracts = new ArrayList<>(command.lines().size());
            final List<LineRejection> rejections = new ArrayList<>();

            final Map<UUID, Client> clientsById = ContractImportLineEvaluator.indexById(
                    clientRepository.findAllByIds(clientRepository.lockAgainstDeletion(command.clientIds())));
            final LamalAffiliations lamalAffiliations = LamalAffiliations.of(
                    lamalAffiliationRepository.loadForAdmission(clientsById.keySet()));

            for (int index = 0; index < command.lines().size(); index++) {
                final Line line = command.lines().get(index);
                switch (ContractImportLineEvaluator.evaluate(line.clientId(),
                        line.type(),
                        line.franchise(),
                        line.startDate(),
                        line.endDate(),
                        line.costAmount(),
                        clientsById,
                        lamalAffiliations,
                        now)) {
                    case Accepted(Contract contract) -> contracts.add(contract);
                    case Rejected(ContractImportRejectionReason reason, String message) -> rejections.add(
                            new LineRejection(index, line.clientId(), reason, message));
                }
            }
            return new LineProcessResult(contracts, rejections);
        }
    }
}
