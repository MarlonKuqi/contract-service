package com.mk.contractservice.application.contract;

import com.mk.contractservice.application.contract.usecase.CloseActiveContractsUseCase;
import com.mk.contractservice.domain.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class CloseActiveContractsUseCaseImpl implements CloseActiveContractsUseCase {

    ContractRepository contractRepository;

    @Override
    public void execute(CloseActiveContractsCommand command) {
        log.debug("Closing all active contracts for client: {}", command.clientId());
        contractRepository.closeAllActiveByClientId(command.clientId());
        log.info("All active contracts closed for client: {}", command.clientId());
    }
}

