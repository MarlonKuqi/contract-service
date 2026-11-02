package com.mk.contractservice.infrastructure.persistence.client;

import com.mk.contractservice.infrastructure.persistence.client.entities.ClientJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import java.util.UUID;

public interface ClientJpaRepository extends JpaRepository<ClientJpaEntity, UUID> {

    @Query("select client.id from ClientJpaEntity client where client.id in :ids")
    Set<UUID> findExistingIds(@Param("ids") Set<UUID> ids);
}
