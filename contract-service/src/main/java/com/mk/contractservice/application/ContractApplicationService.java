package com.mk.contractservice.application;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.valueobject.MoneyAmount;
import com.mk.contractservice.web.dto.contract.ContractResponse;
import com.mk.contractservice.web.dto.mapper.contract.ContractMapper;
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
    private final ContractMapper contractMapper;

    public ContractApplicationService(ContractRepository contractRepo,
                                      ClientRepository clientRepo,
                                      ContractMapper contractMapper) {
        this.contractRepo = contractRepo;
        this.clientRepo = clientRepo;
        this.contractMapper = contractMapper;
    }

    /**
     * Create a contract for a given client.
     * - If start is null => set to now
     * - end can be null (active contract)
     * - costAmount is mandatory (validated en amont par le controller)
     */
    @Transactional
    public UUID createForClient(final UUID clientId, OffsetDateTime start, OffsetDateTime end, BigDecimal amount) {
        // 1) Charger le client (404 si absent)
        Client client = clientRepo.findById(clientId).orElseThrow(() ->
                new IllegalArgumentException("Client not found: " + clientId));

        // 2) Normaliser les dates
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startDate = (start != null) ? start : now;

        // (Optionnel) Garde-fou : start < end si end fourni
        if (end != null && !end.isAfter(startDate)) {
            throw new IllegalArgumentException("endDate must be after startDate");
        }

        // 3) Créer et persister l'entité
        Contract contract = new Contract(
                client,
                startDate,
                end,
                MoneyAmount.of(amount)
        );

        Contract saved = contractRepo.save(contract);
        return saved.getId();
    }

    /**
     * Update only the cost amount; lastModified is updated internally by the entity.
     */
    @Transactional
    public boolean updateCost(final UUID contractId, BigDecimal newAmount) {
        return contractRepo.findById(contractId)
                .map(c -> {
                    c.changeCost(MoneyAmount.of(newAmount));
                    // JPA dirty checking + @Transactional feront le flush
                    return true;
                })
                .orElse(false);
    }

    /**
     * Return ACTIVE contracts for a client (end is null or in the future).
     * Optional filter on lastModified >= updatedSince.
     */
    @Transactional(readOnly = true)
    public List<ContractResponse> getActiveContracts(final UUID clientId, OffsetDateTime updatedSince) {
        OffsetDateTime now = OffsetDateTime.now();
        var contracts = contractRepo.findActiveByClientId(clientId, now, updatedSince);
        return contracts.stream().map(contractMapper::toDto).toList();
    }

    /**
     * Very performant endpoint (baseline): sum of ACTIVE contracts for a client.
     * (Si tu ajoutes une materialized view, remplace l’implémentation ici par une lecture de la MV.)
     */
    @Transactional(readOnly = true)
    public BigDecimal sumActiveContracts(final UUID clientId) {
        OffsetDateTime now = OffsetDateTime.now();
        return contractRepo.sumActiveByClientId(clientId, now);
    }
}