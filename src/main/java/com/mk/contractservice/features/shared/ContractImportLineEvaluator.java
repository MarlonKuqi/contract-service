package com.mk.contractservice.features.shared;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractCost;
import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import com.mk.contractservice.domain.contract.ContractOverlapPolicy;
import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.contract.exception.ContractTypeNotAllowedException;
import com.mk.contractservice.domain.contract.exception.ContractValidationException;
import com.mk.contractservice.domain.contract.exception.InvalidContractCostException;
import com.mk.contractservice.domain.contract.exception.InvalidContractPeriodException;
import com.mk.contractservice.domain.contract.exception.OverlappingContractException;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContractImportLineEvaluator {

    public sealed interface Outcome permits Accepted, Rejected {
    }

    public record Accepted(Contract contract) implements Outcome {
    }

    public record Rejected(ContractImportRejectionReason reason, String message) implements Outcome {
    }

    public static Map<UUID, Client> indexById(final List<Client> clients) {
        return clients.stream()
                .collect(Collectors.<Client, UUID, Client>toMap(client -> Objects.requireNonNull(client.id()),
                        Function.identity()));
    }

    public static Outcome evaluate(
            final UUID clientId,
            final ContractType type,
            @Nullable final LocalDateTime startDate,
            @Nullable final LocalDateTime endDate,
            final BigDecimal costAmount,
            final Map<UUID, Client> clientsById,
            final ContractOverlapPolicy.Batch overlapBatch,
            final LocalDateTime now
    ) {
        final Client client = clientsById.get(clientId);
        if (client == null) {
            return new Rejected(ContractImportRejectionReason.CLIENT_NOT_FOUND,
                    ClientNotFoundException.messageFor(clientId));
        }
        try {
            final Contract contract = Contract.of(
                    clientId,
                    client.kind(),
                    type,
                    ContractPeriod.of(startDate, endDate, now),
                    ContractCost.of(costAmount),
                    now
            );
            overlapBatch.admit(contract);
            return new Accepted(contract);
        } catch (final ContractValidationException ex) {
            return new Rejected(reasonOf(ex), ex.getMessage());
        } catch (final OverlappingContractException ex) {
            return new Rejected(ContractImportRejectionReason.OVERLAPPING_CONTRACT, ex.getMessage());
        }
    }

    private static ContractImportRejectionReason reasonOf(final ContractValidationException ex) {
        return switch (ex) {
            case InvalidContractCostException _ -> ContractImportRejectionReason.INVALID_COST;
            case InvalidContractPeriodException _ -> ContractImportRejectionReason.INVALID_PERIOD;
            case ContractTypeNotAllowedException _ -> ContractImportRejectionReason.TYPE_NOT_ALLOWED;
        };
    }
}
