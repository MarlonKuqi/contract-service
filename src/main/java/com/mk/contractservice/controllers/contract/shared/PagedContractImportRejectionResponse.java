package com.mk.contractservice.controllers.contract.shared;

import com.mk.contractservice.domain.contract.importjob.ContractImportRejection;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

@Schema(description = "Paginated list of the lines rejected by an import job")
public record PagedContractImportRejectionResponse(
        @Schema(description = "Rejected lines of the current page, ordered by line number")
        List<Rejection> content,

        @Schema(description = "Current page number (0-based)", example = "0")
        int pageNumber,

        @Schema(description = "Number of items per page", example = "20")
        int pageSize,

        @Schema(description = "Total number of rejected lines", example = "2")
        long totalElements,

        @Schema(description = "Total number of pages", example = "1")
        int totalPages
) {

    public record Rejection(
            @Schema(description = "Line number in the file, the header being line 1", example = "42")
            int lineNumber,

            @Schema(description = "Client of the line, null if unreadable")
            UUID clientId,

            @Schema(description = "invalidLineFormat, invalidContractCost, invalidContractPeriod or clientNotFound",
                    example = "invalidContractCost")
            String code,

            @Schema(description = "Reason of the rejection")
            String message
    ) {
    }

    public static PagedContractImportRejectionResponse from(final Page<ContractImportRejection> page) {
        return new PagedContractImportRejectionResponse(
                page.getContent().stream()
                        .map(rejection -> new Rejection(
                                rejection.lineNumber(),
                                rejection.clientId(),
                                ContractImportCodes.of(rejection.reason()),
                                rejection.message()))
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
