package com.mk.contractservice.controllers.client;

import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.controllers.client.shared.ClientSwaggerTags;
import com.mk.contractservice.features.client.DeleteClient;
import com.mk.contractservice.infrastructure.security.ApplicationRoles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = ClientSwaggerTags.NAME, description = ClientSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ClientEndpoints.CLIENTS_BASE)
@RequiredArgsConstructor
public class DeleteClientController {

    private final DeleteClient deleteClient;

    @Operation(
            summary = "Delete a client",
            description = "Deletes a client and closes their active contracts with endDate set to the deletion date. "
                    + "The client is no longer returned by the API; its data and contract history are kept."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Client deleted successfully and active contracts closed"
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
    @DeleteMapping(ClientEndpoints.PATH_VAR_ID)
    @PreAuthorize(ApplicationRoles.HAS_ADMIN)
    public ResponseEntity<Void> deleteClient(@PathVariable final UUID id) {
        deleteClient.execute(new DeleteClient.Command(id));
        return ResponseEntity.noContent().build();
    }
}

