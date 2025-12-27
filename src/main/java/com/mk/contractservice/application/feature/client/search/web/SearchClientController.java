package com.mk.contractservice.application.feature.client.search.web;

import com.mk.contractservice.application.feature.client.search.core.GetClientById;
import com.mk.contractservice.application.feature.client.shared.constants.ClientEndpoints;
import com.mk.contractservice.application.feature.client.shared.constants.ClientSwaggerTags;
import com.mk.contractservice.application.feature.client.shared.mapper.ClientDtoMapper;
import com.mk.contractservice.application.feature.client.shared.response.ClientResponse;
import com.mk.contractservice.domain.client.aggregate.Client;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.UUID;

@Tag(name = ClientSwaggerTags.NAME, description = ClientSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ClientEndpoints.CLIENTS_BASE)
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class SearchClientController {

    GetClientById getClientById;
    ClientDtoMapper clientDtoMapper;

    @Operation(
            summary = "Read a client with all fields",
            description = "Returns a client (Person or Company) with all its fields. "
                    + "The response includes a 'type' discriminator field (PERSON or COMPANY)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Client found and returned successfully",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ClientResponse.class))
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
    @GetMapping(ClientEndpoints.PATH_VAR_ID)
    public ResponseEntity<ClientResponse> getClientById(
            @PathVariable final UUID id,
            final Locale locale
    ) {
        final Client client = getClientById.execute(new GetClientById.Query(id));
        final ClientResponse response = clientDtoMapper.toResponse(client);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                .body(response);
    }
}

