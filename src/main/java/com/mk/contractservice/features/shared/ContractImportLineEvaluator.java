package com.mk.contractservice.features.shared;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractCost;
import com.mk.contractservice.domain.contract.ContractCoverage;
import com.mk.contractservice.domain.contract.ContractImportRejectionReason;
import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.contract.ContractType;
import com.mk.contractservice.domain.contract.exception.ContractTypeNotAllowedException;
import com.mk.contractservice.domain.contract.exception.ContractValidationException;
import com.mk.contractservice.domain.contract.exception.InvalidContractCostException;
import com.mk.contractservice.domain.contract.exception.InvalidContractFranchiseException;
import com.mk.contractservice.domain.contract.exception.InvalidContractPeriodException;
import com.mk.contractservice.domain.contract.exception.OverlappingContractException;
import com.mk.contractservice.domain.lamal.LamalAffiliation;
import com.mk.contractservice.domain.lamal.exception.LamalExemptionConflictException;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ContractImportLineEvaluator {

    public sealed interface Outcome permits Accepted, Rejected {
    }

    public record Accepted(Contract contract) implements Outcome {
    }

    public record Rejected(ContractImportRejectionReason reason, String message) implements Outcome {
    }

    private final Map<UUID, Client> clientsById;
    private final Map<UUID, LamalAffiliation> affiliationsByPersonId;

    private ContractImportLineEvaluator(final List<Client> clients, final List<LamalAffiliation> affiliations) {
        this.clientsById = clients.stream()
                .collect(Collectors.toMap(client -> Objects.requireNonNull(client.id()), Function.identity()));
        this.affiliationsByPersonId = affiliations.stream()
                .collect(Collectors.toMap(LamalAffiliation::personId,
                        Function.identity(),
                        (first, second) -> first,
                        HashMap::new));
    }

    public static ContractImportLineEvaluator of(final List<Client> clients,
            final List<LamalAffiliation> affiliations) {
        return new ContractImportLineEvaluator(clients, affiliations);
    }

    public Outcome evaluate(
            final UUID clientId,
            final ContractType type,
            @Nullable final Integer franchise,
            @Nullable final LocalDateTime startDate,
            @Nullable final LocalDateTime endDate,
            final BigDecimal costAmount,
            final LocalDateTime now
    ) {
        final Client client = clientsById.get(clientId);
        if (client == null) {
            return new Rejected(ContractImportRejectionReason.CLIENT_NOT_FOUND,
                    ClientNotFoundException.messageFor(clientId));
        }
        try {
            final ContractPeriod period = ContractPeriod.of(startDate, endDate, now);
            final ContractCost cost = ContractCost.of(costAmount);
            final ContractCoverage coverage = ContractCoverage.of(type, franchise);
            final Contract contract = Contract.of(clientId, client.contractHolder(), coverage, period, cost, now);
            if (contract.isLamal()) {
                admit(clientId, contract, now);
            }
            return new Accepted(contract);
        } catch (final ContractValidationException ex) {
            return new Rejected(reasonOf(ex), ex.getMessage());
        } catch (final OverlappingContractException ex) {
            return new Rejected(ContractImportRejectionReason.OVERLAPPING_CONTRACT, ex.getMessage());
        } catch (final LamalExemptionConflictException ex) {
            return new Rejected(ContractImportRejectionReason.LAMAL_EXEMPTION_CONFLICT, ex.getMessage());
        }
    }

    private void admit(final UUID personId, final Contract lamal, final LocalDateTime now) {
        final LamalAffiliation affiliation = affiliationsByPersonId.get(personId);
        if (affiliation == null) {
            throw new IllegalStateException("No LAMal affiliation loaded for person " + personId);
        }
        affiliationsByPersonId.put(personId, affiliation.admit(lamal, now));
    }

    private static ContractImportRejectionReason reasonOf(final ContractValidationException ex) {
        return switch (ex) {
            case InvalidContractCostException _ -> ContractImportRejectionReason.INVALID_COST;
            case InvalidContractPeriodException _ -> ContractImportRejectionReason.INVALID_PERIOD;
            case ContractTypeNotAllowedException _ -> ContractImportRejectionReason.TYPE_NOT_ALLOWED;
            case InvalidContractFranchiseException _ -> ContractImportRejectionReason.INVALID_FRANCHISE;
        };
    }
}
