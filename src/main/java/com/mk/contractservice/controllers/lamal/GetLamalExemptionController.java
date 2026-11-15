package com.mk.contractservice.controllers.lamal;

import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.controllers.client.shared.ClientSwaggerTags;
import com.mk.contractservice.domain.lamal.LamalExemption;
import com.mk.contractservice.features.lamal.GetLamalExemption;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = ClientSwaggerTags.NAME, description = ClientSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ClientEndpoints.CLIENTS_BASE)
@RequiredArgsConstructor
public class GetLamalExemptionController {

    private final GetLamalExemption getLamalExemption;

    @Operation(summary = "Read the LAMal exemption of a person")
    @ApiResponse(responseCode = "200", description = "Exemption recorded for the person")
    @ApiResponse(
            responseCode = "404",
            description = "Client not found or deleted (code clientNotFound), or no exemption recorded (code lamalExemptionNotFound)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping(ClientEndpoints.LAMAL_EXEMPTION)
    public Response getLamalExemption(@PathVariable final UUID id) {
        final LamalExemption exemption = getLamalExemption.execute(new GetLamalExemption.Query(id));
        return new Response(exemption.startDate(), exemption.endDate());
    }

    @Schema(description = "LAMal exemption of a cross-border worker")
    public record Response(
            @Schema(description = "First day of the exemption", example = "2026-01-01") LocalDate startDate,

            @Schema(description = "First day without exemption (exclusive), null while the exemption is ongoing",
                    example = "2027-01-01") @Nullable LocalDate endDate
    ) {
    }
}
