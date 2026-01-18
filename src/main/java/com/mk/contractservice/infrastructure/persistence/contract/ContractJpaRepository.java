package com.mk.contractservice.infrastructure.persistence.contract;

import com.mk.contractservice.infrastructure.persistence.contract.entity.ContractJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ContractJpaRepository extends JpaRepository<ContractJpaEntity, UUID>,
        JpaSpecificationExecutor<ContractJpaEntity> {

    @Modifying
    @Query("""
            UPDATE ContractJpaEntity c
            SET c.endDate = :closureDate, c.lastModified = :closureDate
            WHERE c.clientId = :clientId
              AND (c.endDate IS NULL OR c.endDate > :closureDate)
            """)
    int closeAllActiveByClientId(@Param("clientId") UUID clientId,
                                 @Param("closureDate") LocalDateTime closureDate);
}
