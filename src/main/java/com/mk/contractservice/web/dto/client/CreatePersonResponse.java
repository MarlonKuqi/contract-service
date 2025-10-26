package com.mk.contractservice.web.dto.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePersonResponse(
        UUID id,
        ClientName name,
        Email email,
        PhoneNumber phone,
        LocalDate birthDate
) implements CreateClientResponse {
}