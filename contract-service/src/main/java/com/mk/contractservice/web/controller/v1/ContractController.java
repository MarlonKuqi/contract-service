package com.mk.contractservice.web.controller.v1;

import com.mk.contractservice.application.ContractApplicationService;
import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.web.dto.client.ClientResponse;
import com.mk.contractservice.web.dto.contract.ContractResponse;
import com.mk.contractservice.web.dto.contract.CostUpdateRequest;
import com.mk.contractservice.web.dto.contract.CreateContractRequest;
import com.mk.contractservice.web.dto.contract.CreateContractResponse;
import com.mk.contractservice.web.dto.mapper.contract.CreateContractMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
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

@RestController
@RequestMapping("/v1")
public class ContractController {

    private final ContractApplicationService contractApplicationService;
    private final CreateContractMapper createContractMapper;

    public ContractController(final ContractApplicationService contractApplicationService, final CreateContractMapper createContractMapper) {
        this.contractApplicationService = contractApplicationService;
        this.createContractMapper = createContractMapper;
    }

    @Operation(summary = "Create a contract for a client")
    @PostMapping("/clients/{clientId}/contracts")
    public ResponseEntity<CreateContractResponse> createForClient(@PathVariable final UUID clientId,
                                                                  @Valid @RequestBody CreateContractRequest req) {

        final Contract company = createContractMapper.toEntity(req);
        final UUID contractCreatedId = contractApplicationService.createForClient(clientId, req.startDate(),   // si null → now (dans service)
                req.endDate(),
                req.costAmount());
        final ClientResponse body = clientDtoMapper.toResponse(contractCreatedId);
        return ResponseEntity
                .created(URI.create("/clients/" + created.getId()))
                .body(body);
    }

    @Operation(summary = "Create a contract for a client")
    @PostMapping("/clients/{clientId}/contracts")
    public ResponseEntity<CreateContractResponse> createForClient(@PathVariable final UUID clientId,
                                                                  @Valid @RequestBody final CreateContractRequest req) {
        final UUID contractId = contractApplicationService.createForClient(
                clientId,
                req.startDate(),
                req.endDate(),
                req.costAmount()
        );
        final URI location = URI.create("/v1/clients/" + clientId + "/contracts/" + contractId);
        final CreateContractResponse body = new CreateContractResponse(contractId);
        // 201 + Location vers la ressource créée
        final CreateContractResponse body = new CreateContractResponse(contractId);
        return ResponseEntity.created(location).body(body);
    }

    @Operation(summary = "Update only the cost amount of a contract (auto-updates lastModified internally)")
    @PatchMapping("/contracts/{contractId}/cost")
    public ResponseEntity<Void> updateCost(@PathVariable UUID contractId,
                                           @Valid @RequestBody CostUpdateRequest req) {
        boolean ok = contractApplicationService.updateCost(contractId, req.amount());
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Get all ACTIVE contracts for a client, optionally filtered by lastModified >= updatedSince")
    @GetMapping("/clients/{clientId}/contracts")
    public ResponseEntity<List<ContractResponse>> listActive(
            @PathVariable UUID clientId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime updatedSince
    ) {
        var list = contractApplicationService.getActiveContracts(clientId, updatedSince);
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Very performant endpoint: sum of costAmount of ACTIVE contracts for a client")
    @GetMapping("/clients/{clientId}/contracts/sum-active")
    public ResponseEntity<BigDecimal> sumActive(@PathVariable UUID clientId) {
        return ResponseEntity.ok(contractApplicationService.sumActiveContracts(clientId));
    }
}
