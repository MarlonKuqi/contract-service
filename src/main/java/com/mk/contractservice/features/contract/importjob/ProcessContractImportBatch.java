package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFactory;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.exception.InvalidContractCostException;
import com.mk.contractservice.domain.contract.exception.InvalidContractPeriodException;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobLine;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportRejectionReason;
import com.mk.contractservice.domain.shared.UseCase;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import com.mk.contractservice.features.shared.ContractImportCsv.MalformedLineException;
import com.mk.contractservice.features.shared.ContractImportCsv.ParsedLine;
import com.mk.contractservice.features.shared.ContractImportCsv.RawLine;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public interface ProcessContractImportBatch {

    record Command(UUID jobId, List<RawLine> lines) {
        public Command {
            Objects.requireNonNull(jobId, "Job ID cannot be null");
            lines = List.copyOf(Objects.requireNonNull(lines, "Lines cannot be null"));
        }

        Set<UUID> clientIds() {
            return lines.stream()
                    .map(RawLine::clientIdIfReadable)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toSet());
        }
    }

    void execute(Command command);

    @UseCase
    @Transactional
    @RequiredArgsConstructor
    class Handler implements ProcessContractImportBatch {

        private final ClientRepository clientRepository;
        private final ContractRepository contractRepository;
        private final ContractImportJobRepository jobRepository;
        private final Clock clock;

        private record AcceptedLine(int lineNumber, Contract contract) {
        }

        @Override
        public void execute(final Command command) {
            final Instant instant = clock.instant();
            final LocalDateTime now = LocalDateTime.ofInstant(instant, clock.getZone());
            final UUID jobId = command.jobId();
            final ContractImportJob job = jobRepository.getById(jobId);
            final Set<UUID> existingClientIds = clientRepository.lockExistingIds(command.clientIds());

            final List<AcceptedLine> accepted = new ArrayList<>();
            final List<ContractImportJobLine> lines = new ArrayList<>(command.lines().size());
            for (final RawLine line : command.lines()) {
                processLine(jobId, line, existingClientIds, now, accepted, lines);
            }

            final List<Contract> contracts = contractRepository.insertAll(accepted.stream().map(AcceptedLine::contract).toList());
            for (int index = 0; index < contracts.size(); index++) {
                final Contract contract = contracts.get(index);
                lines.add(ContractImportJobLine.imported(jobId, accepted.get(index).lineNumber(), contract.getClientId(), contract.getId()));
            }
            jobRepository.insertLines(lines);

            final int rejected = lines.size() - contracts.size();
            jobRepository.save(job.recordBatch(contracts.size(), rejected, instant.plus(ClaimContractImportJob.LEASE)));
        }

        private void processLine(
                final UUID jobId,
                final RawLine line,
                final Set<UUID> existingClientIds,
                final LocalDateTime now,
                final List<AcceptedLine> accepted,
                final List<ContractImportJobLine> rejections
        ) {
            final int number = line.lineNumber();
            final ParsedLine parsed;
            try {
                parsed = line.parse();
            } catch (final MalformedLineException ex) {
                rejections.add(ContractImportJobLine.rejected(jobId, number, line.clientIdIfReadable().orElse(null),
                        ContractImportRejectionReason.INVALID_LINE_FORMAT, ex.getMessage()));
                return;
            }
            final UUID clientId = parsed.clientId();
            if (!existingClientIds.contains(clientId)) {
                rejections.add(ContractImportJobLine.rejected(jobId, number, clientId, ContractImportRejectionReason.CLIENT_NOT_FOUND, ClientNotFoundException.messageFor(clientId)));
                return;
            }
            try {
                accepted.add(new AcceptedLine(number, ContractFactory.createFromCommand(
                        clientId, parsed.startDate(), parsed.endDate(), parsed.costAmount(), now)));
            } catch (final InvalidContractCostException ex) {
                rejections.add(ContractImportJobLine.rejected(jobId, number, clientId, ContractImportRejectionReason.INVALID_COST, ex.getMessage()));
            } catch (final InvalidContractPeriodException ex) {
                rejections.add(ContractImportJobLine.rejected(jobId, number, clientId, ContractImportRejectionReason.INVALID_PERIOD, ex.getMessage()));
            }
        }
    }
}
