package com.mk.contractservice.web.controller;

import com.mk.contractservice.application.ClientApplicationService;
import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import com.mk.contractservice.web.dto.PatchClientRequest;
import com.mk.contractservice.web.dto.client.ClientResponse;
import com.mk.contractservice.web.dto.client.CreateClientRequest;
import com.mk.contractservice.web.dto.client.CreateClientResponse;
import com.mk.contractservice.web.dto.client.CreateCompanyRequest;
import com.mk.contractservice.web.dto.client.CreatePersonRequest;
import com.mk.contractservice.web.dto.client.UpdateClientRequest;
import com.mk.contractservice.web.dto.mapper.client.ClientDtoMapper;
import com.mk.contractservice.web.dto.mapper.client.CompanyResponseMapper;
import com.mk.contractservice.web.dto.mapper.client.PersonResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;
import java.util.UUID;

@Tag(name = "Clients", description = "Operations on clients (create, read, update, delete)")
@RestController
@RequestMapping(ClientController.PATH_BASE)
public class ClientController {

    public static final String VERSION = "/v2";
    public static final String PATH_BASE = VERSION + "/clients";
    public static final String PATH_ID = "/{id}";
    public static final String PATH_CLIENT = PATH_BASE + PATH_ID;

    private final ClientApplicationService service;
    private final ClientDtoMapper clientDtoMapper;
    private final PersonResponseMapper personResponseMapper;
    private final CompanyResponseMapper companyResponseMapper;

    public ClientController(final ClientApplicationService service,
                            final ClientDtoMapper clientDtoMapper,
                            final PersonResponseMapper personResponseMapper,
                            final CompanyResponseMapper companyResponseMapper) {
        this.service = service;
        this.clientDtoMapper = clientDtoMapper;
        this.personResponseMapper = personResponseMapper;
        this.companyResponseMapper = companyResponseMapper;
    }

    @Operation(
            summary = "Create a client (Person or Company)",
            description = "Creates a new client. The type is discriminated via the 'type' field in JSON. "
                    + "Use 'PERSON' for person clients (requires birthDate) or 'COMPANY' for company clients (requires companyIdentifier). "
                    + "On success returns 201 Created with Location header to " + PATH_BASE + "/{id}."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Client created successfully",
                    headers = {
                            @Header(name = "Location", description = "URI of the created client resource")
                    },
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreateClientResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Malformed JSON / invalid syntax / missing 'type' field",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Email or company identifier already exists",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Business validation failed (e.g., invalid email, phone, birthdate, or company identifier)",
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
    public ResponseEntity<CreateClientResponse> create(
            @Valid @RequestBody final CreateClientRequest request,
            final UriComponentsBuilder uriBuilder,
            final Locale locale
    ) {
        return switch (request) {
            case CreatePersonRequest personReq -> createPersonResponse(personReq, uriBuilder, locale);
            case CreateCompanyRequest companyReq -> createCompanyResponse(companyReq, uriBuilder, locale);
        };
    }

    private ResponseEntity<CreateClientResponse> createPersonResponse(
            CreatePersonRequest request,
            UriComponentsBuilder uriBuilder,
            Locale locale
    ) {
        final Person person = service.createPerson(
                request.name(),
                request.email(),
                request.phone(),
                request.birthDate()
        );
        return buildCreatedResponse(person, personResponseMapper.toDto(person), uriBuilder, locale);
    }

    private ResponseEntity<CreateClientResponse> createCompanyResponse(
            CreateCompanyRequest request,
            UriComponentsBuilder uriBuilder,
            Locale locale
    ) {
        final Company company = service.createCompany(
                request.name(),
                request.email(),
                request.phone(),
                request.companyIdentifier()
        );
        return buildCreatedResponse(company, companyResponseMapper.toDto(company), uriBuilder, locale);
    }

    private ResponseEntity<CreateClientResponse> buildCreatedResponse(
            final Client client,
            final CreateClientResponse body,
            final UriComponentsBuilder uriBuilder,
            final Locale locale
    ) {
        final var location = uriBuilder
                .path(PATH_CLIENT)
                .buildAndExpand(client.getId())
                .toUri();

        return ResponseEntity.created(location)
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @Operation(
            summary = "Read a client with all fields",
            description = "Returns a client (Person or Company) with all its fields. "
                    + "The response includes a 'type' discriminator field (PERSON or COMPANY)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Client found and returned successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClientResponse.class))
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
    @GetMapping(PATH_ID)
    public ResponseEntity<ClientResponse> read(@PathVariable final UUID id, final Locale locale) {
        final var client = service.getClientById(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                .body(clientDtoMapper.toResponse(client));
    }

    @Operation(
            summary = "Update a client (all fields except birthDate/companyIdentifier)",
            description = "Updates the common fields of a client (name, email, phone). "
                    + "birthDate and companyIdentifier cannot be updated as per business rules. "
                    + "Works for both Person and Company clients."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Client updated successfully (no content returned)"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data (validation failed)",
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
                    description = "Business validation failed (e.g., invalid email or phone format)",
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
    @PutMapping(PATH_ID)
    public ResponseEntity<Void> update(@PathVariable final UUID id, @Valid @RequestBody final UpdateClientRequest req) {
        service.updateCommonFields(
                id, ClientName.of(req.name()), Email.of(req.email()), PhoneNumber.of(req.phone())
        );
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Partially update a client",
            description = "Updates only the provided fields of a client (name, email, phone). "
                    + "Fields not included in the request are left unchanged. "
                    + "birthDate and companyIdentifier cannot be updated as per business rules. "
                    + "Works for both Person and Company clients."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Client partially updated successfully (no content returned)"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data (validation failed for provided fields)",
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
                    description = "Business validation failed (e.g., invalid email or phone format for provided fields)",
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
    @PatchMapping("/{id}")
    public ResponseEntity<Void> patch(@PathVariable final UUID id, @RequestBody final PatchClientRequest req) {
        service.patchClient(
                id,
                req.name().map(ClientName::of).orElse(null),
                req.email().map(Email::of).orElse(null),
                req.phone().map(PhoneNumber::of).orElse(null)
        );
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete a client",
            description = "Deletes a client (and its Person or Company subtype via database cascade) "
                    + "and automatically closes their active contracts by setting endDate=now. "
                    + "This ensures referential integrity and data consistency."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Client deleted successfully (including Person/Company record and contract closure)"
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
    @DeleteMapping(PATH_ID)
    public ResponseEntity<Void> delete(@PathVariable final UUID id) {
        service.deleteClientAndCloseContracts(id);
        return ResponseEntity.noContent().build();
    }
}
