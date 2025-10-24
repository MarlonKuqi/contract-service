package com.mk.contractservice.web.controller.v1;

import com.mk.contractservice.application.ClientApplicationService;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonName;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import com.mk.contractservice.web.dto.client.ClientResponse;
import com.mk.contractservice.web.dto.client.UpdateClientRequest;
import com.mk.contractservice.web.dto.mapper.client.ClientDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientApplicationService service;
    private final ClientDtoMapper clientDtoMapper;

    public ClientController(final ClientApplicationService service, final ClientDtoMapper clientDtoMapper) {
        this.service = service;
        this.clientDtoMapper = clientDtoMapper;
    }

    @Operation(summary = "Read a client with all fields")
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> read(@PathVariable final UUID id) {
        return service.findById(id)
                .map(c -> ResponseEntity.ok(clientDtoMapper.toResponse(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update a client (all fields except birthDate/companyIdentifier)")
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> update(@PathVariable final UUID id, @Valid @RequestBody UpdateClientRequest req) {
        final boolean ok = service.updateCommonFields(
                id, PersonName.of(req.name()), Email.of(req.email()), PhoneNumber.of(req.phone())
        );
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Delete a client (also closes their active contracts with endDate=now)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ClientResponse> delete(@PathVariable final UUID id) {
        final boolean ok = service.deleteClientAndCloseContracts(id);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
