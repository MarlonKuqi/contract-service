package com.mk.contractservice.application.contract.mapper;

import com.mk.contractservice.domain.contract.ContractCost;
import com.mk.contractservice.domain.contract.ContractPeriod;
import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface ContractValueObjectMappers {

    default @Nullable BigDecimal costAmountToBigDecimal(ContractCost costAmount) {
        return costAmount != null ? costAmount.value() : null;
    }

    @Named("periodToStartDate")
    default @Nullable LocalDateTime periodToStartDate(ContractPeriod period) {
        return period != null ? period.startDate() : null;
    }

    @Named("periodToEndDate")
    default @Nullable LocalDateTime periodToEndDate(ContractPeriod period) {
        return period != null ? period.endDate() : null;
    }
}

