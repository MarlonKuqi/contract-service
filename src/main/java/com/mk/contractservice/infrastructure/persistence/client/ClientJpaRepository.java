package com.mk.contractservice.infrastructure.persistence.client;

import com.mk.contractservice.infrastructure.persistence.client.entities.ClientJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ClientJpaRepository extends JpaRepository<ClientJpaEntity, UUID> {

    @Query(value = "select id from contracts.client where id in (:ids) and deleted_at is null order by id for update",
            nativeQuery = true)
    Set<UUID> lockExistingIds(@Param("ids") Set<UUID> ids);

    @Query(value = "select id from contracts.client where id in (:ids) and deleted_at is null order by id for share",
            nativeQuery = true)
    Set<UUID> lockAgainstDeletion(@Param("ids") Set<UUID> ids);

    @Query(value = "select p.birth_date from contracts.person p where p.id = :id", nativeQuery = true)
    Optional<LocalDate> findPersonBirthDateIncludingDeleted(@Param("id") UUID id);

    @Query(value = "select exists (select 1 from contracts.company c where c.id = :id)", nativeQuery = true)
    boolean existsCompanyIncludingDeleted(@Param("id") UUID id);
}
