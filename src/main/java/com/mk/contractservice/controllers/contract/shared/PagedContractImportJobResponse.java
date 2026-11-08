package com.mk.contractservice.controllers.contract.shared;

import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Paginated list of import jobs, newest first")
public record PagedContractImportJobResponse(
        @Schema(description = "Import jobs of the current page") List<ContractImportJobResponse> content,

        @Schema(description = "Current page number (0-based)", example = "0") int pageNumber,

        @Schema(description = "Number of items per page", example = "20") int pageSize,

        @Schema(description = "Total number of import jobs", example = "3") long totalElements,

        @Schema(description = "Total number of pages", example = "1") int totalPages
) {

    public static PagedContractImportJobResponse from(final Page<ContractImportJob> page) {
        return new PagedContractImportJobResponse(
                page.getContent().stream().map(ContractImportJobResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
