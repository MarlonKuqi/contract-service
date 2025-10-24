package com.mk.contractservice.infrastructure.persistence;

import com.mk.contractservice.domain.client.Client;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ClientJpaRepository extends JpaRepository<Client, UUID> {
}
