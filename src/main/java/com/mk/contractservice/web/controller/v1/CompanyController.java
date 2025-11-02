package com.mk.contractservice.web.controller.v1;

import com.mk.contractservice.application.ClientApplicationService;
import com.mk.contractservice.domain.client.Company;
import com.mk.contractservice.web.dto.client.CreateCompanyRequest;
import com.mk.contractservice.web.dto.client.CreateCompanyResponse;
import com.mk.contractservice.web.dto.mapper.client.CompanyResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Clients - Companies", description = "Create company clients")
@RestController
@RequestMapping("/v1/clients/companies")
public class CompanyController {

    private final ClientApplicationService clientApplicationService;
    private final CompanyResponseMapper companyResponseMapper;

    private static final String PATH_NEW_RESOURCE = "/v1/clients/{id}";

    public CompanyController(final ClientApplicationService clientApplicationService,
                             final CompanyResponseMapper companyResponseMapper) {
        this.clientApplicationService = clientApplicationService;
        this.companyResponseMapper = companyResponseMapper;
    }

    @Operation(
            summary = "Create a company client",
            description = "Creates a new company client and returns its representation. "
                    + "On success returns 201 Created with Location header."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Company created",
                    headers = {
                            @Header(name = "Location", description = "URI of the created client resource")
                    },
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreateCompanyResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Email already exists",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateCompanyResponse> create(
            @Valid @RequestBody final CreateCompanyRequest req,
            final UriComponentsBuilder uriBuilder
    ) {
        final Company created = clientApplicationService.createCompany(
                req.name(),
                req.email(),
                req.phone(),
                req.companyIdentifier()
        );
        final CreateCompanyResponse body = companyResponseMapper.toDto(created);

        final var location = uriBuilder
                .path(PATH_NEW_RESOURCE)
                .buildAndExpand(created.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(body);
    }
}
