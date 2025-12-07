package com.mk.contractservice.application.dto;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContractDto(
        UUID id,
        UUID clientId,
        LocalDateTime startDate,
        @Nullable LocalDateTime endDate,
        boolean active,
        BigDecimal costAmount
) {
}


