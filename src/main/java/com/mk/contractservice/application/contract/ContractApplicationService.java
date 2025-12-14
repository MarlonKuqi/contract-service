package com.mk.contractservice.application.contract;

import com.mk.contractservice.application.contract.dto.ContractDto;
import com.mk.contractservice.application.contract.mapper.ContractMapper;
import com.mk.contractservice.domain.client.ClientRepository;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractCost;
import com.mk.contractservice.domain.contract.ContractPeriod;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.domain.contract.ContractService;
import com.mk.contractservice.domain.exception.ClientNotFoundException;
import com.mk.contractservice.domain.exception.ContractNotFoundException;
import org.jspecify.annotations.Nullable;
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
    private final ContractMapper contractMapper;

    public ContractApplicationService(final ContractRepository contractRepo,
                                      final ClientRepository clientRepo,
                                      final ContractService contractService,
                                      final ContractMapper contractMapper) {
        this.contractRepo = contractRepo;
        this.clientRepo = clientRepo;
        this.contractService = contractService;
        this.contractMapper = contractMapper;
    }

    @Transactional
    @CacheEvict(value = "contractSums", key = "#clientId")
    public ContractDto createForClient(final UUID clientId, final LocalDateTime start, final LocalDateTime end, final BigDecimal amount) {
        if (!clientRepo.existsById(clientId)) {
            throw new ClientNotFoundException("Client not found: " + clientId);
        }
        final ContractPeriod period = ContractPeriod.of(start, end);
        final ContractCost cost = ContractCost.of(amount);
        final Contract savedContract = contractService.createAndPersistContract(clientId, period, cost);
        return contractMapper.toDto(savedContract);
    }

    @Transactional
    @CacheEvict(value = "contractSums", key = "#clientId")
    public ContractDto updateCost(final UUID clientId, final UUID contractId, final BigDecimal newAmount) {
        final Contract contract = getContractForClient(clientId, contractId);
        final Contract updatedContract = contract.changeCost(ContractCost.of(newAmount));
        final Contract savedContract = contractRepo.save(updatedContract);
        return contractMapper.toDto(savedContract);
    }

    @Transactional(readOnly = true)
    public ContractDto getContractById(final UUID clientId, final UUID contractId) {
        final Contract contract = getContractForClient(clientId, contractId);
        return contractMapper.toDto(contract);
    }

    private Contract getContractForClient(final UUID clientId, final UUID contractId) {
        final Contract contract = contractRepo.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException(contractId));
        contractService.ensureContractBelongsToClient(contract, clientId);
        return contract;
    }

    @Transactional(readOnly = true)
    public Page<ContractDto> getActiveContractsPageable(final UUID clientId, @Nullable final LocalDateTime updatedSince, final Pageable pageable) {
        final Page<Contract> contracts = contractRepo.findActiveByClientIdPageable(clientId, updatedSince, pageable);
        return contracts.map(contractMapper::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "contractSums", key = "#clientId")
    public BigDecimal sumActiveContracts(final UUID clientId) {
        return contractRepo.sumActiveByClientId(clientId);
    }

    @Transactional
    @CacheEvict(value = "contractSums", key = "#clientId")
    public void closeActiveContractsByClientId(final UUID clientId) {
        contractRepo.closeAllActiveByClientId(clientId);
    }
}