package com.mk.contractservice.controllers.contract;

import com.mk.contractservice.controllers.contract.shared.ContractDtoMapper;
import com.mk.contractservice.controllers.contract.shared.ContractEndpoints;
import com.mk.contractservice.controllers.contract.shared.ContractExportResponse;
import com.mk.contractservice.controllers.contract.shared.ContractResponse;
import com.mk.contractservice.controllers.contract.shared.ContractSwaggerTags;
import com.mk.contractservice.features.contract.ExportActiveContracts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Tag(name = ContractSwaggerTags.NAME, description = ContractSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ContractEndpoints.CONTRACTS_BASE)
@RequiredArgsConstructor
public class ExportContractController {

    private static final String DEFAULT_PAGE_SIZE = "100";
    private static final int MAX_PAGE_SIZE = 1000;

    private final ExportActiveContracts exportActiveContracts;
    private final ContractDtoMapper contractMapper;
    private final Clock clock;

    @Operation(
            summary = "Export all ACTIVE contracts of all clients (cursor pagination)",
            description = "Returns the contracts active at the time of each request (endDate = null or now < endDate), "
                    + "ordered by id. Start without cursor, then send back nextCursor unchanged until it is null. "
                    + "A contract whose cost changes during the export is neither skipped nor exported twice; "
                    + "a contract that expires before its page is read is not exported. "
                    + "Page size: default 100, max 1000."
    )
    @ApiResponse(
            responseCode = "200",
            description = "One page of the export",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ContractExportResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid cursor (code invalidCursor) or page size out of bounds (code invalidParameter)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping(ContractEndpoints.RELATIVE_CONTRACT_EXPORT)
    public ResponseEntity<ContractExportResponse> export(
            @RequestParam(required = false) @Nullable final String cursor,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE)
            @Positive(message = "Page size must not be less than one")
            @Max(value = MAX_PAGE_SIZE, message = "Page size must not exceed {value}") final int size,
            final Locale locale
    ) {
        final UUID after = cursor == null ? null : ContractExportCursor.decode(cursor);
        final ExportActiveContracts.Result result = exportActiveContracts.execute(
                new ExportActiveContracts.Query(after, size)
        );

        final LocalDateTime now = LocalDateTime.now(clock);
        final List<ContractResponse> items = result.contracts().stream()
                .map(contract -> contractMapper.toResponse(contract, now))
                .toList();
        final String nextCursor = result.nextAfter().map(ContractExportCursor::encode).orElse(null);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                .body(new ContractExportResponse(items, nextCursor));
    }
}
