package com.mk.contractservice.infrastructure.persistence;

import com.mk.contractservice.infrastructure.persistence.entity.ClientJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ClientJpaRepository extends JpaRepository<ClientJpaEntity, UUID> {
    boolean existsByEmail(String email);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CompanyJpaEntity c WHERE c.companyIdentifier = :companyIdentifier")
    boolean existsByCompanyIdentifier(@Param("companyIdentifier") String companyIdentifier);
}
