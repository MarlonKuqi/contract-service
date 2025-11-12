package com.mk.contractservice.application;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractService;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.domain.valueobject.ContractCost;
import com.mk.contractservice.domain.valueobject.ContractPeriod;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ContractApplicationService {

    private final ContractRepository contractRepo;
    private final ClientRepository clientRepo;
    private final ContractService contractService;

    public ContractApplicationService(ContractRepository contractRepo,
                                      ClientRepository clientRepo,
                                      ContractService contractService) {
        this.contractRepo = contractRepo;
        this.clientRepo = clientRepo;
        this.contractService = contractService;
    }

    @Transactional
    @CacheEvict(value = "contractSums", key = "#clientId")
    public Contract createForClient(final UUID clientId, final LocalDateTime start, final LocalDateTime end, final BigDecimal amount) {
        final Client client = clientRepo.findById(clientId).orElseThrow(() ->
                new ClientNotFoundException("Client not found: " + clientId));

        final ContractPeriod period = ContractPeriod.of(start, end);

        final Contract contract = Contract.builder()
                .client(client)
                .period(period)
                .costAmount(ContractCost.of(amount))
                .build();

        return contractRepo.save(contract);
    }

    @Transactional
    @CacheEvict(value = "contractSums", key = "#clientId")
    public void updateCost(final UUID clientId, final UUID contractId, BigDecimal newAmount) {
        final Contract contract = contractService.getContractForClient(clientId, contractId);
        
        contract.changeCost(ContractCost.of(newAmount));
        contractRepo.save(contract);
    }

    @Transactional(readOnly = true)
    public Contract getContractById(final UUID clientId, final UUID contractId) {
        return contractService.getContractForClient(clientId, contractId);
    }

    @Transactional(readOnly = true)
    public Page<Contract> getActiveContractsPageable(final UUID clientId, LocalDateTime updatedSince, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        return contractRepo.findActiveByClientIdPageable(clientId, now, updatedSince, pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "contractSums", key = "#clientId")
    public BigDecimal sumActiveContracts(final UUID clientId) {
        LocalDateTime now = LocalDateTime.now();
        return contractRepo.sumActiveByClientId(clientId, now);
    }

    @Transactional
    @CacheEvict(value = "contractSums", key = "#clientId")
    public void closeActiveContractsByClientId(final UUID clientId) {
        final LocalDateTime now = LocalDateTime.now();
        contractRepo.closeAllActiveByClientId(clientId, now);
    }
}