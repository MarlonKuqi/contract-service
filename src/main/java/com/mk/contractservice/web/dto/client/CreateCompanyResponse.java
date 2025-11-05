package com.mk.contractservice.web.dto.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.CompanyIdentifier;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import java.util.UUID;

public record CreateCompanyResponse(
        UUID id,
        ClientName name,
        Email email,
        PhoneNumber phone,
        CompanyIdentifier companyIdentifier
) implements CreateClientResponse {
}