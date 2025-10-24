package com.mk.contractservice.web.dto.contract;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateContractRequest(
        OffsetDateTime startDate,  // si null => now
        OffsetDateTime endDate,    // si null => null (actif)
        @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal costAmount
) {
}
