package com.mk.contractservice.application;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractApplicationService {

    private final ContractRepository contractRepo;
    private final ClientRepository clientRepo;

    public ContractApplicationService(ContractRepository contractRepo,
                                      ClientRepository clientRepo) {
        this.contractRepo = contractRepo;
        this.clientRepo = clientRepo;
    }

    /**
     * Create a contract for a given client.
     *
     * <p>Business rules (enforced by ContractPeriod):
     * <ul>
     *   <li>If start is null => defaults to now</li>
     *   <li>endDate can be null (active contract)</li>
     *   <li>If endDate is provided, it must be after startDate</li>
     * </ul>
     *
     * @param clientId the client UUID
     * @param start the start date (null defaults to now)
     * @param end the end date (null means active contract)
     * @param amount the cost amount
     * @return the persisted Contract entity with generated ID and validated period
     * @throws IllegalArgumentException if client not found or period validation fails
     */
    @Transactional
    public Contract createForClient(final UUID clientId, final OffsetDateTime start, final OffsetDateTime end, final BigDecimal amount) {
        final Client client = clientRepo.findById(clientId).orElseThrow(() ->
                new IllegalArgumentException("Client not found: " + clientId));

        final ContractPeriod period = ContractPeriod.of(start, end);

        final Contract contract = new Contract(client, period, ContractCost.of(amount));

        return contractRepo.save(contract);
    }

    /**
     * Update only the cost amount; lastModified is updated internally by the entity.
     */
    @Transactional
    public boolean updateCost(final UUID contractId, BigDecimal newAmount) {
        return contractRepo.findById(contractId)
                .map(c -> {
                    c.changeCost(ContractCost.of(newAmount));
                    return true;
                })
                .orElse(false);
    }

    /**
     * Return ACTIVE contracts for a client (end is null or in the future).
     * Optional filter on lastModified >= updatedSince.
     *
     * @return List of active Contract entities (mapping to DTO is done by the controller)
     */
    @Transactional(readOnly = true)
    public List<Contract> getActiveContracts(final UUID clientId, OffsetDateTime updatedSince) {
        OffsetDateTime now = OffsetDateTime.now();
        return contractRepo.findActiveByClientId(clientId, now, updatedSince);
    }

    /**
     * Sum of costAmount for all ACTIVE contracts of a client.
     * Very performant endpoint (DB aggregation).
     */
    @Transactional(readOnly = true)
    public BigDecimal sumActiveContracts(final UUID clientId) {
        OffsetDateTime now = OffsetDateTime.now();
        return contractRepo.sumActiveByClientId(clientId, now);
    }
}