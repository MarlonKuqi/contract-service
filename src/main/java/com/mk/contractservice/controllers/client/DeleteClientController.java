package com.mk.contractservice.controllers.client;

import com.mk.contractservice.controllers.client.shared.ClientEndpoints;
import com.mk.contractservice.controllers.client.shared.ClientSwaggerTags;
import com.mk.contractservice.features.client.DeleteClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = ClientSwaggerTags.NAME, description = ClientSwaggerTags.DESCRIPTION)
@RestController
@RequestMapping(ClientEndpoints.CLIENTS_BASE)
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class DeleteClientController {

    DeleteClient deleteClient;

    @Operation(
            summary = "Delete a client",
            description = "Deletes a client (and its Person or Company subtype via database cascade) "
                    + "and automatically closes their active contracts by setting endDate=now. "
                    + "This ensures referential integrity and data consistency."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Client deleted successfully (including Person/Company record and contract closure)"
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
    public ResponseEntity<Void> deleteClient(@PathVariable final UUID id) {
        deleteClient.execute(new DeleteClient.Command(id));
        return ResponseEntity.noContent().build();
    }
}

