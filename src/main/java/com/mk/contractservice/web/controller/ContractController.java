package com.mk.contractservice.web.controller;

import com.mk.contractservice.application.ContractApplicationService;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.web.dto.contract.ContractResponse;
import com.mk.contractservice.web.dto.contract.CostUpdateRequest;
import com.mk.contractservice.web.dto.contract.CreateContractRequest;
import com.mk.contractservice.web.dto.contract.CreateContractResponse;
import com.mk.contractservice.web.dto.contract.PagedContractResponse;
import com.mk.contractservice.web.dto.mapper.contract.ContractMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Tag(name = "Contracts", description = "Operations on contracts (create, read, update cost)")
@RestController
@RequestMapping(ContractController.PATH_BASE)
public class ContractController {
    public static final String VERSION = "/v2";
    public static final String PATH_BASE = VERSION + "/contracts";
    public static final String PATH_ID = "/{contractId}";
    public static final String PATH_CONTRACT = PATH_BASE + PATH_ID;
    public static final String PATH_SUM = "/sum";
    public static final String PATH_COST = PATH_ID + "/cost";
    public static final String PATH_CONTRACT_COST = PATH_CONTRACT + "/cost";

    private final ContractApplicationService contractApplicationService;
    private final ContractMapper contractMapper;

    public ContractController(final ContractApplicationService contractApplicationService,
                              final ContractMapper contractMapper) {
        this.contractApplicationService = contractApplicationService;
        this.contractMapper = contractMapper;
    }

    @Operation(
            summary = "Create a contract for a client",
            description = "Creates a new contract for a client. "
                    + "startDate defaults to now if not provided. "
                    + "endDate null means active contract."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Contract created successfully",
                    headers = {
                            @Header(name = "Location", description = "URI of the created contract resource")
                    },
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreateContractResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Malformed JSON / invalid syntax",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Client not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Business validation failed (e.g., invalid cost amount, date range)",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PostMapping
    public ResponseEntity<CreateContractResponse> create(
            @RequestParam final UUID clientId,
            @Valid @RequestBody final CreateContractRequest req,
            final UriComponentsBuilder uriBuilder,
            final Locale locale
    ) {
        final Contract contract = contractApplicationService.createForClient(
                clientId,
                req.startDate(),
                req.endDate(),
                req.costAmount()
        );

        final var location = uriBuilder
                .path(PATH_CONTRACT)
                .buildAndExpand(contract.getId())
                .toUri();

        final CreateContractResponse body = new CreateContractResponse(
                contract.getId(),
                new CreateContractResponse.PeriodResponse(
                        contract.getPeriod().startDate(),
                        contract.getPeriod().endDate()
                ),
                contract.getCostAmount().value()
        );

        return ResponseEntity.created(location)
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                .body(body);
    }

    @Operation(
            summary = "Get all ACTIVE contracts for a client (paginated)",
            description = "Returns all active contracts (current date < end date or endDate = null). "
                    + "Can be filtered by lastModified >= updatedSince. "
                    + "Supports pagination (default size: 20, max: 100). "
                    + "Use query params: ?page=0&size=20&sort=lastModified,desc"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of active contracts (paginated)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PagedContractResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid query parameters (e.g., invalid date format)",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Client not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping
    public ResponseEntity<PagedContractResponse> listActive(
            @RequestParam final UUID clientId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime updatedSince,
            final Pageable pageable,
            final Locale locale
    ) {
        final Page<Contract> contracts = contractApplicationService.getActiveContractsPageable(clientId, updatedSince, pageable);
        final Page<ContractResponse> responsePage = contracts.map(contractMapper::toDto);

        final PagedContractResponse response = new PagedContractResponse(
                responsePage.getContent(),
                responsePage.getNumber(),
                responsePage.getSize(),
                responsePage.getTotalElements(),
                responsePage.getTotalPages(),
                responsePage.isFirst(),
                responsePage.isLast()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                .body(response);
    }

    @Operation(
            summary = "Get a specific contract by ID",
            description = "Retrieves a single contract by its ID. "
                    + "Validates that the contract belongs to the specified client."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contract found"),
            @ApiResponse(responseCode = "403", description = "Contract does not belong to this client"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    @GetMapping(PATH_ID)
    public ResponseEntity<ContractResponse> getById(
            @RequestParam final UUID clientId,
            @PathVariable final UUID contractId,
            final Locale locale
    ) {
        final Contract contract = contractApplicationService.getContractById(clientId, contractId);
        final ContractResponse response = contractMapper.toDto(contract);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                .body(response);
    }

    @Operation(
            summary = "Sum of costAmount of ACTIVE contracts for a client",
            description = "Returns the sum of costAmount for all active contracts of a client. "
                    + "Returns 0.00 if no active contracts exist."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Sum calculated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BigDecimal.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Client not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @GetMapping(PATH_SUM)
    public ResponseEntity<BigDecimal> sumActive(
            @RequestParam final UUID clientId,
            final Locale locale
    ) {
        final BigDecimal sum = contractApplicationService.sumActiveContracts(clientId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                .body(sum);
    }

    @Operation(
            summary = "Update the cost amount of a contract",
            description = "Updates only the costAmount field. The lastModified field is automatically updated internally. "
                    + "Business rule: Only active contracts (not expired) can be updated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cost updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "403", description = "Contract does not belong to this client"),
            @ApiResponse(responseCode = "404", description = "Contract not found"),
            @ApiResponse(responseCode = "422", description = "Contract is expired and cannot be modified")
    })
    @PatchMapping(PATH_COST)
    public ResponseEntity<Void> updateCost(
            @RequestParam final UUID clientId,
            @PathVariable final UUID contractId,
            @Valid @RequestBody final CostUpdateRequest req
    ) {
        contractApplicationService.updateCost(clientId, contractId, req.amount());
        return ResponseEntity.noContent().build();
    }
}
