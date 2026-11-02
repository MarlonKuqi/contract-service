package com.mk.contractservice.infrastructure.persistence.client;

import com.mk.contractservice.domain.client.ClientDetails;
import com.mk.contractservice.domain.client.ClientQueryPort;
import com.mk.contractservice.domain.client.CompanyDetails;
import com.mk.contractservice.domain.client.PersonDetails;
import com.mk.contractservice.domain.shared.exception.ClientNotFoundException;
import com.mk.contractservice.infrastructure.persistence.client.entities.ClientJpaEntity;
import com.mk.contractservice.infrastructure.persistence.client.entities.CompanyJpaEntity;
import com.mk.contractservice.infrastructure.persistence.client.entities.PersonJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaClientQueryAdapter implements ClientQueryPort {

    private final ClientJpaRepository jpa;

    @Override
    public ClientDetails getDetails(final UUID clientId) {
        return jpa.findById(clientId)
                .map(JpaClientQueryAdapter::toDetails)
                .orElseThrow(() -> ClientNotFoundException.forId(clientId));
    }

    private static ClientDetails toDetails(final ClientJpaEntity entity) {
        return switch (entity) {
            case PersonJpaEntity p -> new PersonDetails(
                    p.getId(), p.getName(), p.getEmail(), p.getPhone(), p.getBirthDate(), p.getVersion()
            );
            case CompanyJpaEntity c -> new CompanyDetails(
                    c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getCompanyIdentifier(), c.getVersion()
            );
            default -> throw new IllegalArgumentException("Unknown client entity type: " + entity.getClass().getName());
        };
    }
}
