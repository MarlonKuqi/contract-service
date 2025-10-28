package com.mk.contractservice.application;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
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

    @Transactional
    public Contract createForClient(final UUID clientId, final OffsetDateTime start, final OffsetDateTime end, final BigDecimal amount) {
        final Client client = clientRepo.findById(clientId).orElseThrow(() ->
                new ClientNotFoundException("Client not found: " + clientId));

        final ContractPeriod period = ContractPeriod.of(start, end);

        final Contract contract = new Contract(client, period, ContractCost.of(amount));

        return contractRepo.save(contract);
    }

    @Transactional
    public boolean updateCost(final UUID contractId, BigDecimal newAmount) {
        return contractRepo.findById(contractId)
                .map(c -> {
                    c.changeCost(ContractCost.of(newAmount));
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<Contract> getActiveContracts(final UUID clientId, OffsetDateTime updatedSince) {
        OffsetDateTime now = OffsetDateTime.now();
        return contractRepo.findActiveByClientId(clientId, now, updatedSince);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumActiveContracts(final UUID clientId) {
        OffsetDateTime now = OffsetDateTime.now();
        return contractRepo.sumActiveByClientId(clientId, now);
    }

    @Transactional
    public void closeActiveContractsByClientId(final UUID clientId) {
        final OffsetDateTime now = OffsetDateTime.now();
        contractRepo.closeAllActiveByClientId(clientId, now);
    }
}