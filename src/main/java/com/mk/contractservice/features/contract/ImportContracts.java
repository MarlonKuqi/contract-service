package com.mk.contractservice.features.contract;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFactory;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.exception.InvalidContractCostException;
import com.mk.contractservice.domain.contract.exception.InvalidContractPeriodException;
import com.mk.contractservice.domain.shared.UseCase;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import com.mk.contractservice.features.contract.ContractImportRejectedException.LineRejection;
import com.mk.contractservice.features.contract.ContractImportRejectedException.Reason;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public interface ImportContracts {

    int MAX_LINES = 1000;

    record Line(
            UUID clientId,
            @Nullable LocalDateTime startDate,
            @Nullable LocalDateTime endDate,
            BigDecimal costAmount
    ) {
        public Line {
            Objects.requireNonNull(clientId, "Client ID cannot be null");
            Objects.requireNonNull(costAmount, "Cost amount cannot be null");
        }

        Contract toContract(final LocalDateTime now) {
            return ContractFactory.createFromCommand(clientId, startDate, endDate, costAmount, now);
        }

        LineRejection toRejection(final int index, final Reason reason, final String message) {
            return new LineRejection(index, clientId, reason, message);
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
    @Transactional
    @RequiredArgsConstructor
    class Handler implements ImportContracts {

        private final ClientRepository clientRepository;
        private final ContractRepository contractRepository;
        private final Clock clock;

        private record LineProcessResult(List<Contract> contracts, List<LineRejection> rejections) {
            public boolean hasRejections() {
                return !rejections.isEmpty();
            }
        }

        @Override
        public List<Contract> execute(final Command command) {
            final LocalDateTime now = LocalDateTime.now(clock);

            final LineProcessResult processResult = processLines(command, now);

            if (processResult.hasRejections()) {
                throw new ContractImportRejectedException(processResult.rejections());
            }
            return contractRepository.insertAll(processResult.contracts());
        }

        private LineProcessResult processLines(final Command command, final LocalDateTime now) {
            final List<Contract> contracts = new ArrayList<>(command.lines().size());
            final List<LineRejection> rejections = new ArrayList<>();

            final Set<UUID> existingClientIds = clientRepository.findExistingIds(command.clientIds());

            for (int index = 0; index < command.lines().size(); index++) {
                final Line line = command.lines().get(index);
                if (!existingClientIds.contains(line.clientId())) {
                    rejections.add(line.toRejection(index, Reason.CLIENT_NOT_FOUND, ClientNotFoundException.messageFor(line.clientId())));
                    continue;
                }
                try {
                    contracts.add(line.toContract(now));
                } catch (final InvalidContractCostException ex) {
                    rejections.add(line.toRejection(index, Reason.INVALID_COST, ex.getMessage()));
                } catch (final InvalidContractPeriodException ex) {
                    rejections.add(line.toRejection(index, Reason.INVALID_PERIOD, ex.getMessage()));
                }
            }
            return new LineProcessResult(contracts, rejections);
        }
    }
}
