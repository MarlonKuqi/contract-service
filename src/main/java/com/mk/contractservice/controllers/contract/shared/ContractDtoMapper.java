package com.mk.contractservice.controllers.contract.shared;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractDetails;
import org.mapstruct.Mapper;

import java.time.LocalDateTime;
import java.util.Objects;

@Mapper(componentModel = "spring")
public interface ContractDtoMapper {

    default ContractResponse toResponse(Contract contract, LocalDateTime referenceDate) {
        return new ContractResponse(
                Objects.requireNonNull(contract.id()),
                contract.clientId(),
                contract.type(),
                contract.period().startDate(),
                contract.period().endDate(),
                contract.isActiveAt(referenceDate),
                contract.costAmount().value()
        );
    }

    default ContractResponse toResponse(ContractDetails contract) {
        return new ContractResponse(
                contract.id(),
                contract.clientId(),
                contract.type(),
                contract.startDate(),
                contract.endDate(),
                contract.active(),
                contract.costAmount()
        );
    }
}
