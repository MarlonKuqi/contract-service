package com.mk.contractservice.web.controller.v1;

import com.mk.contractservice.application.ClientApplicationService;
import com.mk.contractservice.web.dto.client.CreatePersonRequest;
import com.mk.contractservice.web.dto.client.CreatePersonResponse;
import com.mk.contractservice.web.dto.mapper.client.PersonResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Clients - Persons")
@RestController
@RequestMapping("/v1/clients/persons")
public class PersonController {

    private final ClientApplicationService clientApplicationService;
    private final PersonResponseMapper personResponseMapper;

    private static final String PATH_NEW_RESOURCE = "/v1/clients/{id}";

    public PersonController(final ClientApplicationService clientApplicationService,
                            final PersonResponseMapper personResponseMapper) {
        this.clientApplicationService = clientApplicationService;
        this.personResponseMapper = personResponseMapper;
    }

    @Operation(
            summary = "Create a person client",
            description = "Creates a new person client and returns its representation. "
                    + "On success returns 201 Created with Location header to PATH_NEW_RESOURCE."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Person created",
                    headers = {
                            @Header(name = "Location", description = "URI of the created client resource")
                    },
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreatePersonResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Malformed JSON / invalid syntax",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict (e.g. duplicate unique business key)",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Business validation failed",
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
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreatePersonResponse> create(
            @Valid @RequestBody final CreatePersonRequest req,
            final UriComponentsBuilder uriBuilder,
            final Locale locale
    ) {
        final var personCreated = clientApplicationService.createPerson(
                req.name(),
                req.email(),
                req.phone(),
                req.birthDate()
        );
        final var body = personResponseMapper.toDto(personCreated);
        final var location = uriBuilder
                .path(PATH_NEW_RESOURCE)
                .buildAndExpand(personCreated.getId())
                .toUri();
        return ResponseEntity
                .created(location)
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}