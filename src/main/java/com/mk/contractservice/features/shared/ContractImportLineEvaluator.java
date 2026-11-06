package com.mk.contractservice.features.shared;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractFactory;
import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import com.mk.contractservice.domain.contract.exception.InvalidContractCostException;
import com.mk.contractservice.domain.contract.exception.InvalidContractPeriodException;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContractImportLineEvaluator {

    public sealed interface Outcome permits Accepted, Rejected {
    }

    public record Accepted(Contract contract) implements Outcome {
    }

    public record Rejected(ContractImportRejectionReason reason, String message) implements Outcome {
    }

    public static Outcome evaluate(
            final UUID clientId,
            @Nullable final LocalDateTime startDate,
            @Nullable final LocalDateTime endDate,
            final BigDecimal costAmount,
            final Set<UUID> existingClientIds,
            final LocalDateTime now
    ) {
        if (!existingClientIds.contains(clientId)) {
            return new Rejected(ContractImportRejectionReason.CLIENT_NOT_FOUND, ClientNotFoundException.messageFor(clientId));
        }
        try {
            return new Accepted(ContractFactory.createFromCommand(clientId, startDate, endDate, costAmount, now));
        } catch (final InvalidContractCostException ex) {
            return new Rejected(ContractImportRejectionReason.INVALID_COST, ex.getMessage());
        } catch (final InvalidContractPeriodException ex) {
            return new Rejected(ContractImportRejectionReason.INVALID_PERIOD, ex.getMessage());
        }
    }
}
