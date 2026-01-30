package com.mk.contractservice.infrastructure.persistence.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ContractJpaRepository extends JpaRepository<ContractJpaEntity, UUID>,
        JpaSpecificationExecutor<ContractJpaEntity> {
}
