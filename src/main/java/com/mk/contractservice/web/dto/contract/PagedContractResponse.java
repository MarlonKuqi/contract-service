package com.mk.contractservice.web.dto.contract;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated list of contracts")
public record PagedContractResponse(
        @Schema(description = "List of contracts in the current page")
        List<ContractResponse> content,

        @Schema(description = "Current page number (0-based)", example = "0")
        int pageNumber,

        @Schema(description = "Number of items per page", example = "20")
        int pageSize,

        @Schema(description = "Total number of contracts across all pages", example = "100")
        long totalElements,

        @Schema(description = "Total number of pages", example = "5")
        int totalPages,

        @Schema(description = "True if this is the first page", example = "true")
        boolean isFirst,

        @Schema(description = "True if this is the last page", example = "false")
        boolean isLast
) {
}

