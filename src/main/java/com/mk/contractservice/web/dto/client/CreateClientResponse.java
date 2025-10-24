package com.mk.contractservice.web.dto.client;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(oneOf = {CreatePersonResponse.class, CreateCompanyResponse.class})
public sealed interface CreateClientResponse permits CreatePersonResponse, CreateCompanyResponse {
}