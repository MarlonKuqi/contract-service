package com.mk.contractservice.domain.client.factory;

import com.mk.contractservice.domain.client.aggregate.Company;
import com.mk.contractservice.domain.client.valueobject.ClientEmail;
import com.mk.contractservice.domain.client.valueobject.ClientName;
import com.mk.contractservice.domain.client.valueobject.ClientPhoneNumber;
import com.mk.contractservice.domain.client.valueobject.CompanyIdentifier;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class CompanyFactory {

    public static Company createFromCommand(
            final String name,
            final String email,
            final String phoneNumber,
            final String companyIdentifier
    ) {
        final ClientName clientName = ClientName.of(name);
        final ClientEmail clientEmail = ClientEmail.of(email);
        final ClientPhoneNumber phone = ClientPhoneNumber.of(phoneNumber);
        final CompanyIdentifier companyId = CompanyIdentifier.of(companyIdentifier);

        return Company.of(clientName, clientEmail, phone, companyId);
    }

    public static Company buildFromDatabase(
            final UUID id,
            final String name,
            final String email,
            final String phoneNumber,
            final String companyIdentifier
    ) {
        final ClientName clientName = ClientName.reconstituteFromDatabase(name);
        final ClientEmail clientEmail = ClientEmail.reconstituteFromDatabase(email);
        final ClientPhoneNumber phone = ClientPhoneNumber.reconstituteFromDatabase(phoneNumber);
        final CompanyIdentifier companyId = CompanyIdentifier.reconstituteFromDatabase(companyIdentifier);

        return Company.reconstituteFromDatabase(id, clientName, clientEmail, phone, companyId);
    }
}

