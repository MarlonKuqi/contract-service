package com.mk.contractservice.web.controller.v1;

import com.mk.contractservice.application.ContractApplicationService;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.web.dto.contract.ContractResponse;
import com.mk.contractservice.web.dto.contract.CostUpdateRequest;
import com.mk.contractservice.web.dto.contract.CreateContractRequest;
import com.mk.contractservice.web.dto.contract.CreateContractResponse;
import com.mk.contractservice.web.dto.mapper.contract.ContractMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.util.List;
import java.util.UUID;

@Tag(name = "Contracts", description = "Operations on contracts (create, read, update cost)")
@RestController
@RequestMapping("/v1/clients/{clientId}/contracts")
public class ContractController {

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
            @ApiResponse(responseCode = "201", description = "Contract created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @PostMapping
    public ResponseEntity<CreateContractResponse> create(
            @PathVariable final UUID clientId,
            @Valid @RequestBody final CreateContractRequest req,
            final UriComponentsBuilder uriBuilder
    ) {
        final Contract contract = contractApplicationService.createForClient(
                clientId,
                req.startDate(),
                req.endDate(),
                req.costAmount()
        );

        final var location = uriBuilder
                .path("/v1/clients/{clientId}/contracts/{contractId}")
                .buildAndExpand(clientId, contract.getId())
                .toUri();

        final CreateContractResponse body = new CreateContractResponse(
                new CreateContractResponse.PeriodResponse(
                        contract.getPeriod().startDate(),
                        contract.getPeriod().endDate()
                ),
                contract.getCostAmount().value()
        );
        return ResponseEntity.created(location).body(body);
    }

    @Operation(
            summary = "Get all ACTIVE contracts for a client",
            description = "Returns all active contracts (current date < end date or endDate = null). "
                    + "Can be filtered by lastModified >= updatedSince."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of active contracts"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @GetMapping
    public ResponseEntity<List<ContractResponse>> listActive(
            @PathVariable final UUID clientId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime updatedSince
    ) {
        final List<Contract> contracts = contractApplicationService.getActiveContracts(clientId, updatedSince);
        final List<ContractResponse> response = contracts.stream()
                .map(contractMapper::toDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Sum of costAmount of ACTIVE contracts for a client",
            description = "Very performant endpoint that returns the sum of costAmount for all active contracts of a client."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sum calculated successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @GetMapping("/sum")
    public ResponseEntity<BigDecimal> sumActive(@PathVariable final UUID clientId) {
        final BigDecimal sum = contractApplicationService.sumActiveContracts(clientId);
        return ResponseEntity.ok(sum);
    }

    @Operation(
            summary = "Update the cost amount of a contract",
            description = "Updates only the costAmount field. The lastModified field is automatically updated internally."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cost updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Contract not found")
    })
    @PatchMapping("/{contractId}/cost")
    public ResponseEntity<Void> updateCost(
            @PathVariable final UUID clientId,
            @PathVariable final UUID contractId,
            @Valid @RequestBody final CostUpdateRequest req
    ) {
        final boolean ok = contractApplicationService.updateCost(clientId, contractId, req.amount());
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
