package com.mk.contractservice.controllers.lamal;

import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.controllers.client.shared.ClientSwaggerTags;
import com.mk.contractservice.features.lamal.RecordLamalExemption;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = ClientSwaggerTags.NAME, description = ClientSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ClientEndpoints.CLIENTS_BASE)
@RequiredArgsConstructor
public class RecordLamalExemptionController {

    private final RecordLamalExemption recordLamalExemption;

    @Operation(
            summary = "Record or replace the LAMal exemption of a cross-border worker",
            description = "Records the cantonal decision exempting a person living in a bordering country (FR, DE, IT, AT) from LAMal, from startDate (inclusive, may be in the past) to endDate (exclusive, optional). " + "Replaces the exemption already recorded: send it again with an endDate to close it. Sending the recorded exemption again changes nothing. " + "Refused if the exemption covers an active LAMal of the person. No If-Match: the exemption is replaced as a whole."
    )
    @ApiResponse(responseCode = "204", description = "Exemption recorded")
    @ApiResponse(
            responseCode = "404",
            description = "Client not found or deleted (code clientNotFound)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "The exemption covers an active LAMal of the person (code lamalExemptionConflict)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "422",
            description = "Invalid payload (validationError), end date not after the start date (invalidLamalExemptionPeriod), company or person living in Switzerland (lamalExemptionNotAllowed)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PutMapping(ClientEndpoints.LAMAL_EXEMPTION)
    public ResponseEntity<Void> recordLamalExemption(
            @PathVariable final UUID id,
            @Valid @RequestBody final Request request
    ) {
        recordLamalExemption.execute(new RecordLamalExemption.Command(id, request.startDate(), request.endDate()));
        return ResponseEntity.noContent().build();
    }

    @Schema(description = "LAMal exemption granted by the canton of work", example = """
            {
              "startDate": "2026-01-01",
              "endDate": null
            }
            """)
    public record Request(
            @NotNull(message = "Start date is required") @Schema(description = "First day of the exemption",
                    example = "2026-01-01", format = "date",
                    requiredMode = Schema.RequiredMode.REQUIRED) @Nullable LocalDate startDate,

            @Schema(description = "First day without exemption (exclusive), null while the exemption is ongoing",
                    example = "2027-01-01", format = "date") @Nullable LocalDate endDate
    ) {
    }
}
