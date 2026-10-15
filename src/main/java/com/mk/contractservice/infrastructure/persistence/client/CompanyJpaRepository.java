package com.mk.contractservice.infrastructure.persistence.client;

import com.mk.contractservice.infrastructure.persistence.client.entities.CompanyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface CompanyJpaRepository extends JpaRepository<CompanyJpaEntity, UUID> {

    @Query(
            value = "SELECT EXISTS (SELECT 1 FROM contracts.company WHERE company_identifier = :companyIdentifier)",
            nativeQuery = true
    )
    boolean existsByCompanyIdentifierIncludingDeleted(String companyIdentifier);
}
