package com.mk.contractservice.infrastructure.persistence.contract;

import com.mk.contractservice.infrastructure.persistence.contract.entity.ContractJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface ContractJpaRepository extends JpaRepository<ContractJpaEntity, UUID> {


    @Query("""
            SELECT c FROM ContractJpaEntity c
            WHERE c.clientId = :clientId
              AND (c.endDate IS NULL OR c.endDate > CURRENT_TIMESTAMP)
            """)
    Page<ContractJpaEntity> findActiveByClientId(@Param("clientId") UUID clientId,
                                                 Pageable pageable);

    @Query("""
            SELECT c FROM ContractJpaEntity c
            WHERE c.clientId = :clientId
              AND (c.endDate IS NULL OR c.endDate > CURRENT_TIMESTAMP)
              AND c.lastModified >= :updatedSince
            """)
    Page<ContractJpaEntity> findActiveByClientIdAndUpdatedAfter(@Param("clientId") UUID clientId,
                                                                @Param("updatedSince") LocalDateTime updatedSince,
                                                                Pageable pageable);

    @Modifying
    @Query("""
            UPDATE ContractJpaEntity c
            SET c.endDate = :closureDate, c.lastModified = :closureDate
            WHERE c.clientId = :clientId
              AND (c.endDate IS NULL OR c.endDate > :closureDate)
            """)
    int closeAllActiveByClientId(@Param("clientId") UUID clientId,
                                 @Param("closureDate") LocalDateTime closureDate);


    @Query("""
            SELECT COALESCE(SUM(c.costAmount), 0)
            FROM ContractJpaEntity c
            WHERE c.clientId = :clientId
              AND (c.endDate IS NULL OR c.endDate > CURRENT_TIMESTAMP)
            """)
    BigDecimal sumActiveByClientId(@Param("clientId") UUID clientId);
}
