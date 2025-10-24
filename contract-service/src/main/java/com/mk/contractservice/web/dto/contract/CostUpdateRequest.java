package com.mk.contractservice.web.dto.contract;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CostUpdateRequest(
        @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal amount
) {
}
