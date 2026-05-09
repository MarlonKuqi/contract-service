package com.mk.contractservice.controllers.client;

import com.mk.contractservice.controllers.client.shared.ClientDtoMapper;
import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.controllers.client.shared.ClientResponse;
import com.mk.contractservice.controllers.client.shared.ClientSwaggerTags;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.features.client.CreateCompany;
import com.mk.contractservice.features.client.CreatePerson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;

@Tag(name = ClientSwaggerTags.NAME, description = ClientSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ClientEndpoints.CLIENTS_BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateClientController {

    CreatePerson createPerson;
    CreateCompany createCompany;
    ClientDtoMapper clientDtoMapper;

    @Operation(
            summary = "Create a client (Person or Company)",
            description = "Creates a new client. The type is discriminated via the 'type' field in JSON. "
                    + "Use 'PERSON' for person clients (requires birthDate) or 'COMPANY' for company clients (requires companyIdentifier). "
                    + "On success returns 201 Created with Location header to /clients/{id}. "
                    + "API version 2.0."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Client created successfully",
            headers = {
                    @Header(name = "Location", description = "URI of the created client resource")
            },
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ClientResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Malformed JSON / invalid syntax / missing 'type' field",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Conflict - Email or company identifier already exists",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "422",
            description = "Business validation failed (e.g., invalid email, phone, birthdate, or company identifier)",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Unexpected server error",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class))
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientResponse> create(
            @Valid @RequestBody final CreateClientRequest request,
            final UriComponentsBuilder uriBuilder,
            final Locale locale
    ) {
        return switch (request) {
            case CreatePersonRequest personReq -> createPersonResponse(personReq, uriBuilder, locale);
            case CreateCompanyRequest companyReq -> createCompanyResponse(companyReq, uriBuilder, locale);
        };
    }

    private ResponseEntity<ClientResponse> createPersonResponse(
            CreatePersonRequest request,
            UriComponentsBuilder uriBuilder,
            Locale locale
    ) {
        final CreatePerson.Command command = new CreatePerson.Command(
                request.name(),
                request.email(),
                request.phone(),
                request.birthDate()
        );
        final Person person = createPerson.execute(command);
        final ClientResponse response = clientDtoMapper.toResponse(person);

        final var location = uriBuilder
                .path(ClientEndpoints.CLIENT_BY_ID)
                .buildAndExpand(person.getId())
                .toUri();
        return ResponseEntity.created(location)
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    private ResponseEntity<ClientResponse> createCompanyResponse(
            CreateCompanyRequest request,
            UriComponentsBuilder uriBuilder,
            Locale locale
    ) {
        final CreateCompany.Command command = new CreateCompany.Command(
                request.name(),
                request.email(),
                request.phone(),
                request.companyIdentifier()
        );
        final Company company = createCompany.execute(command);
        final ClientResponse response = clientDtoMapper.toResponse(company);

        final var location = uriBuilder
                .path(ClientEndpoints.CLIENT_BY_ID)
                .buildAndExpand(company.getId())
                .toUri();
        return ResponseEntity.created(location)
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}

