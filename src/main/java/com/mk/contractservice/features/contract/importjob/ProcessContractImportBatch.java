package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobLine;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.domain.lamal.LamalAffiliationRepository;
import com.mk.contractservice.domain.lamal.LamalAffiliations;
import com.mk.contractservice.features.shared.ContractImportCsv.MalformedLineException;
import com.mk.contractservice.features.shared.ContractImportCsv.ParsedLine;
import com.mk.contractservice.features.shared.ContractImportCsv.RawLine;
import com.mk.contractservice.features.shared.ContractImportLineEvaluator;
import com.mk.contractservice.features.shared.ContractImportLineEvaluator.Accepted;
import com.mk.contractservice.features.shared.ContractImportLineEvaluator.Rejected;
import com.mk.contractservice.features.shared.UseCase;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    @RequiredArgsConstructor
    class Handler implements ProcessContractImportBatch {

        private final ClientRepository clientRepository;
        private final ContractRepository contractRepository;
        private final LamalAffiliationRepository lamalAffiliationRepository;
        private final ContractImportJobRepository jobRepository;
        private final Clock clock;

        private sealed interface LineOutcome permits ImportedLine, RejectedLine {
        }

        private record ImportedLine(int lineNumber, Contract contract) implements LineOutcome {
        }

        private record RejectedLine(ContractImportJobLine line) implements LineOutcome {
        }

        @Override
        public void execute(final Command command) {
            final Instant instant = clock.instant();
            final LocalDateTime now = LocalDateTime.ofInstant(instant, clock.getZone());
            final UUID jobId = command.jobId();
            final ContractImportJob job = jobRepository.getById(jobId);
            final Set<UUID> lockedClientIds = clientRepository.lockExistingIds(command.clientIds());
            final Map<UUID, Client> clientsById = ContractImportLineEvaluator.indexById(
                    clientRepository.findAllByIds(lockedClientIds));
            final LamalAffiliations lamalAffiliations = LamalAffiliations.of(
                    lamalAffiliationRepository.loadForAdmission(lockedClientIds));

            final List<ImportedLine> imported = new ArrayList<>();
            final List<ContractImportJobLine> lines = new ArrayList<>(command.lines().size());
            for (final RawLine line : command.lines()) {
                switch (evaluate(jobId, line, clientsById, lamalAffiliations, now)) {
                    case ImportedLine importedLine -> imported.add(importedLine);
                    case RejectedLine(ContractImportJobLine rejectedLine) -> lines.add(rejectedLine);
                }
            }

            final List<Contract> contracts = contractRepository.saveAll(imported.stream()
                    .map(ImportedLine::contract)
                    .toList());
            for (int index = 0; index < contracts.size(); index++) {
                final Contract contract = contracts.get(index);
                lines.add(ContractImportJobLine.imported(jobId,
                        imported.get(index).lineNumber(),
                        contract.clientId(),
                        contract.id()));
            }
            jobRepository.saveLines(lines);

            final int rejected = lines.size() - contracts.size();
            jobRepository.save(job.recordBatch(contracts.size(), rejected, instant.plus(ClaimContractImportJob.LEASE)));
        }

        private static LineOutcome evaluate(
                final UUID jobId,
                final RawLine line,
                final Map<UUID, Client> clientsById,
                final LamalAffiliations lamalAffiliations,
                final LocalDateTime now
        ) {
            final int number = line.lineNumber();
            final ParsedLine parsed;
            try {
                parsed = line.parse();
            } catch (final MalformedLineException ex) {
                return rejected(jobId,
                        number,
                        line.clientIdIfReadable().orElse(null),
                        ContractImportRejectionReason.INVALID_LINE_FORMAT,
                        ex.getMessage());
            }
            return switch (ContractImportLineEvaluator.evaluate(
                    parsed.clientId(),
                    parsed.type(),
                    parsed.franchise(),
                    parsed.startDate(),
                    parsed.endDate(),
                    parsed.costAmount(),
                    clientsById,
                    lamalAffiliations,
                    now)) {
                case Accepted(Contract contract) -> new ImportedLine(number, contract);
                case Rejected(ContractImportRejectionReason reason, String message) -> rejected(jobId,
                        number,
                        parsed.clientId(),
                        reason,
                        message);
            };
        }

        private static RejectedLine rejected(
                final UUID jobId,
                final int lineNumber,
                @Nullable final UUID clientId,
                final ContractImportRejectionReason reason,
                final String message
        ) {
            return new RejectedLine(ContractImportJobLine.rejected(jobId, lineNumber, clientId, reason, message));
        }
    }
}
