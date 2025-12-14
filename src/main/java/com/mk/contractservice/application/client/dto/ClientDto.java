package com.mk.contractservice.application.client.dto;

import java.util.UUID;

public sealed interface ClientDto permits PersonDto, CompanyDto {
    UUID id();

    String name();

    String email();

    String phone();
}

