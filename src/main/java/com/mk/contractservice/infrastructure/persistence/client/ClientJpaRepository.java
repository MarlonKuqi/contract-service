package com.mk.contractservice.infrastructure.persistence.client;

import com.mk.contractservice.infrastructure.persistence.client.entities.ClientJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientJpaRepository extends JpaRepository<ClientJpaEntity, UUID> {
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
