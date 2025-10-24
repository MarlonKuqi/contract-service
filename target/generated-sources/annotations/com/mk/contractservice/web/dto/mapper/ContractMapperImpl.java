package com.mk.contractservice.web.dto.mapper;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.web.dto.contract.ContractResponse;
import com.mk.contractservice.web.dto.mapper.contract.ContractMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-20T01:10:43+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class ContractMapperImpl implements ContractMapper {

    @Override
    public ContractResponse toDto(Contract c) {
        if ( c == null ) {
            return null;
        }

        UUID id = null;
        OffsetDateTime startDate = null;
        OffsetDateTime endDate = null;

        id = c.getId();
        startDate = c.getStartDate();
        endDate = c.getEndDate();

        UUID clientId = c.getClient().getId();
        BigDecimal costAmount = c.getCostAmount().value();

        ContractResponse contractResponse = new ContractResponse( id, clientId, startDate, endDate, costAmount );

        return contractResponse;
    }
}
