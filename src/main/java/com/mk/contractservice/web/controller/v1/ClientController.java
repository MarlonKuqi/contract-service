package com.mk.contractservice.web.controller.v1;

import com.mk.contractservice.application.ClientApplicationService;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import com.mk.contractservice.web.dto.client.ClientResponse;
import com.mk.contractservice.web.dto.client.UpdateClientRequest;
import com.mk.contractservice.web.dto.mapper.client.ClientDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.UUID;

@Tag(name = "Clients", description = "Operations on clients (read, update, delete)")
@RestController
@RequestMapping("/v1/clients")
public class ClientController {

    private final ClientApplicationService service;
    private final ClientDtoMapper clientDtoMapper;

    public ClientController(final ClientApplicationService service, final ClientDtoMapper clientDtoMapper) {
        this.service = service;
        this.clientDtoMapper = clientDtoMapper;
    }

    @Operation(
            summary = "Read a client with all fields",
            description = "Returns a client (Person or Company) with all its fields. "
                    + "The response includes a 'type' discriminator field."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client found"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> read(@PathVariable final UUID id, final Locale locale) {
        return service.findById(id)
                .map(c -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag())
                        .body(clientDtoMapper.toResponse(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Update a client (all fields except birthDate/companyIdentifier)",
            description = "Updates the common fields of a client (name, email, phone). "
                    + "birthDate and companyIdentifier cannot be updated as per business rules."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Client updated successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable final UUID id, @Valid @RequestBody final UpdateClientRequest req) {
        final boolean ok = service.updateCommonFields(
                id, ClientName.of(req.name()), Email.of(req.email()), PhoneNumber.of(req.phone())
        );
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Delete a client",
            description = "Deletes a client and automatically closes their active contracts by setting endDate=now"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Client deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final UUID id) {
        final boolean ok = service.deleteClientAndCloseContracts(id);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
