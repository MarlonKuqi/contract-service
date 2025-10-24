package com.mk.contractservice.web.dto.contract;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateContractResponse(
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        BigDecimal costAmount
) {
}
