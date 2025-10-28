package com.mk.contractservice.web.dto.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import java.util.UUID;

public record PersonResponse(
        UUID id,
        ClientName name,
        Email email,
        PhoneNumber phone,
        PersonBirthDate birthDate
) implements ClientResponse {
}

