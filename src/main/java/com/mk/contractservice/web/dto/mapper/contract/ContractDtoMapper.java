package com.mk.contractservice.web.dto.mapper.contract;

import com.mk.contractservice.application.dto.ContractDto;
import com.mk.contractservice.web.dto.contract.ContractResponse;
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
