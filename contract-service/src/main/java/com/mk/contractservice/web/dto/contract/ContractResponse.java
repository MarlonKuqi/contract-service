package com.mk.contractservice.web.dto.contract;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ContractResponse(
        UUID id,
        UUID clientId,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        BigDecimal costAmount
) {
}
