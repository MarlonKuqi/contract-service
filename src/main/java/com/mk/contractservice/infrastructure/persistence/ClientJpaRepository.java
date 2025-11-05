package com.mk.contractservice.infrastructure.persistence;

import com.mk.contractservice.domain.client.Client;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientJpaRepository extends JpaRepository<Client, UUID> {
    boolean existsByEmail_Value(String value);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Company c WHERE c.companyIdentifier.value = :companyIdentifier")
    boolean existsByCompanyIdentifier(@Param("companyIdentifier") String companyIdentifier);
}
