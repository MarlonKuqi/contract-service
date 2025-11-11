package com.mk.contractservice.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
@Schema(description = "Request to partially update a client")
public record PatchClientRequest(
        @Schema(description = "Client's name (optional)")
        Optional<String> name,

        @Schema(description = "Client's email address (optional)")
        Optional<String> email,

        @Schema(description = "Client's phone number in international format (optional)")
        Optional<String> phone
) {
}

