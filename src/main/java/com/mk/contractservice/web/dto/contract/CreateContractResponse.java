package com.mk.contractservice.web.dto.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateContractResponse(
        @JsonProperty("period") PeriodResponse period,
        @JsonProperty("costAmount") BigDecimal costAmount
) {
    public record PeriodResponse(
            @JsonProperty("startDate") OffsetDateTime startDate,
            @JsonProperty("endDate") OffsetDateTime endDate
    ) {}
}
