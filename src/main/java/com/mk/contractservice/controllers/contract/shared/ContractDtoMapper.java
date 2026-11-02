package com.mk.contractservice.controllers.contract.shared;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractDetails;
import org.mapstruct.Mapper;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface ContractDtoMapper {

    default ContractResponse toResponse(Contract contract, LocalDateTime referenceDate) {
        return new ContractResponse(
                contract.getId(),
                contract.getClientId(),
                contract.getPeriod().getStartDate(),
                contract.getPeriod().getEndDate(),
                contract.isActiveAt(referenceDate),
                contract.getCostAmount().getValue()
        );
    }

    default ContractResponse toResponse(ContractDetails contract) {
        return new ContractResponse(
                contract.id(),
                contract.clientId(),
                contract.startDate(),
                contract.endDate(),
                contract.active(),
                contract.costAmount()
        );
    }
}

