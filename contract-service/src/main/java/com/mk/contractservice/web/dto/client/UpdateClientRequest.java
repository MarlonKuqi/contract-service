package com.mk.contractservice.web.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateClientRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @jakarta.validation.constraints.Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "\\+?[0-9 .()/-]{7,20}") String phone
) {
}
