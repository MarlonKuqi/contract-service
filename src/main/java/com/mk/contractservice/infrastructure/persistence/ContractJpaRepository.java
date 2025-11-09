package com.mk.contractservice.infrastructure.persistence;

import com.mk.contractservice.infrastructure.persistence.entity.ContractJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ContractJpaRepository extends JpaRepository<ContractJpaEntity, UUID> {

    @Query("SELECT c FROM ContractJpaEntity c JOIN FETCH c.client WHERE c.id = :id")
    Optional<ContractJpaEntity> findByIdWithClient(@Param("id") UUID id);

    @Query("""
            SELECT c FROM ContractJpaEntity c
            JOIN FETCH c.client
            WHERE c.client.id = :clientId
              AND (c.endDate IS NULL OR c.endDate > :now)
            """)
    Page<ContractJpaEntity> findActiveContractsPageable(@Param("clientId") UUID clientId,
                                                        @Param("now") LocalDateTime now,
                                                        Pageable pageable);

    @Query("""
            SELECT c FROM ContractJpaEntity c
            JOIN FETCH c.client
            WHERE c.client.id = :clientId
              AND (c.endDate IS NULL OR c.endDate > :now)
              AND c.lastModified >= :updatedSince
            """)
    Page<ContractJpaEntity> findActiveContractsUpdatedAfterPageable(@Param("clientId") UUID clientId,
                                                                    @Param("now") LocalDateTime now,
                                                                    @Param("updatedSince") LocalDateTime updatedSince,
                                                                    Pageable pageable);

    @Modifying
    @Query("""
            UPDATE ContractJpaEntity c
            SET c.endDate = :now, c.lastModified = CURRENT_TIMESTAMP
            WHERE c.client.id = :clientId
              AND (c.endDate IS NULL OR c.endDate > :now)
            """)
    void closeAllActiveContracts(@Param("clientId") UUID clientId, @Param("now") LocalDateTime now);

    @Query("""
            SELECT COALESCE(SUM(c.costAmount), 0)
            FROM ContractJpaEntity c
            WHERE c.client.id = :clientId
              AND (c.endDate IS NULL OR c.endDate > :now)
            """)
    BigDecimal sumActiveContracts(@Param("clientId") UUID clientId, @Param("now") LocalDateTime now);
}
