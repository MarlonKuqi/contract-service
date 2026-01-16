package com.mk.contractservice.infrastructure.web.contract;

import com.mk.contractservice.application.feature.contract.search.GetContractById;
import com.mk.contractservice.application.feature.contract.search.GetTotalActiveContractsByClient;
import com.mk.contractservice.application.feature.contract.search.ListActiveContractsByClient;
import com.mk.contractservice.domain.contract.aggregate.Contract;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractDtoMapper;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractEndpoints;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractResponse;
import com.mk.contractservice.infrastructure.web.contract.shared.ContractSwaggerTags;
import com.mk.contractservice.infrastructure.web.contract.shared.PagedContractResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Tag(name = ContractSwaggerTags.NAME, description = ContractSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ContractEndpoints.CONTRACTS_BASE)
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class SearchContractController {

    GetContractById getContractById;
    ListActiveContractsByClient listActiveContractsByClient;
    GetTotalActiveContractsByClient getTotalActiveContractsByClient;
    ContractDtoMapper contractMapper;

    @Operation(
            summary = "Get a contract by ID",
            description = "Returns a contract by its unique identifier."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Contract found and returned successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ContractResponse.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Contract not found",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping(ContractEndpoints.PATH_VAR_CONTRACT_ID)
    public ResponseEntity<ContractResponse> getById(
            @PathVariable final UUID contractId,
            final Locale locale
    ) {
        final Contract contract = getContractById.execute(
                new GetContractById.Query(contractId)
        );

        final ContractResponse response = contractMapper.toResponse(contract);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                .body(response);
    }

    @Operation(
            summary = "List all ACTIVE contracts for a client (paginated)",
            description = "Returns all active contracts (current date < end date or endDate = null). "
                    + "Can be filtered by lastModified >= updatedSince. "
                    + "Supports pagination (default size: 20, max: 100). "
                    + "Use query params: ?page=0&size=20&sort=lastModified,desc. "
                    + "Returns 200 OK if all resources are present, 206 Partial Content with Content-Range header if paginated."
    )
    @ApiResponse(
            responseCode = "200",
            description = "All active contracts returned (no pagination needed)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PagedContractResponse.class))
    )
    @ApiResponse(
            responseCode = "206",
            description = "Partial list of active contracts (paginated)",
            headers = {
                    @Header(name = "Content-Range", description = "Range of resources returned (e.g., contracts 0-19/50)")
            },
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PagedContractResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid query parameters (e.g., invalid date format)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Client not found",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping
    public ResponseEntity<PagedContractResponse> listActive(
            @RequestParam final UUID clientId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime updatedSince,
            final Pageable pageable,
            final Locale locale
    ) {
        final Page<Contract> contracts = listActiveContractsByClient.execute(
                new ListActiveContractsByClient.Query(
                        clientId,
                        Optional.ofNullable(updatedSince),
                        pageable
                )
        );

        final Page<ContractResponse> responsePage = contracts.map(contractMapper::toResponse);

        final PagedContractResponse response = new PagedContractResponse(
                responsePage.getContent(),
                responsePage.getNumber(),
                responsePage.getSize(),
                responsePage.getTotalElements(),
                responsePage.getTotalPages(),
                responsePage.isFirst(),
                responsePage.isLast()
        );

        final long totalElements = responsePage.getTotalElements();
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
        if (totalElements > 0) {
            final long firstElement = (long) responsePage.getNumber() * responsePage.getSize();
            final long lastElement = Math.min(firstElement + responsePage.getNumberOfElements() - 1, totalElements - 1);
            final boolean isPartialContent = totalElements > responsePage.getNumberOfElements() || responsePage.getNumber() > 0;

            if (isPartialContent) {
                responseBuilder = ResponseEntity.status(206);
                responseBuilder.header("Content-Range", String.format("contracts %d-%d/%d", firstElement, lastElement, totalElements));
            }
        }

        return responseBuilder
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                .body(response);
    }

    @Operation(
            summary = "Calculate total cost of all ACTIVE contracts for a client",
            description = "Returns the total of costAmount for all active contracts. "
                    + "Only contracts with endDate = null or endDate >= now are included. "
                    + "Returns 0 if no active contracts."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Total calculated successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = BigDecimal.class, example = "4500.00"))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Client not found",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping(ContractEndpoints.CONTRACT_TOTAL)
    public ResponseEntity<BigDecimal> getTotalActive(
            @RequestParam final UUID clientId,
            final Locale locale
    ) {
        final BigDecimal total = getTotalActiveContractsByClient.execute(new GetTotalActiveContractsByClient.Query(clientId));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                .body(total);
    }
}

