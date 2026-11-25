package com.mk.contractservice.infrastructure.persistence.contract;

import com.mk.contractservice.domain.contract.ContractType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ContractJpaRepository extends JpaRepository<ContractJpaEntity, UUID>, JpaSpecificationExecutor<ContractJpaEntity> {

    List<ContractJpaEntity> findAllByClientIdInAndTypeIn(Collection<UUID> clientIds, Collection<ContractType> types);
}
