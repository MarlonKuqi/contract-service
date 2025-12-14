package com.mk.contractservice.web.contract.mapper;

import com.mk.contractservice.application.contract.dto.ContractDto;
import com.mk.contractservice.web.contract.dto.ContractResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContractDtoMapper {

    default ContractResponse toResponse(ContractDto dto) {
        return new ContractResponse(
                dto.id(),
                dto.clientId(),
                dto.startDate(),
                dto.endDate(),
                dto.active(),
                dto.costAmount()
        );
    }
}
