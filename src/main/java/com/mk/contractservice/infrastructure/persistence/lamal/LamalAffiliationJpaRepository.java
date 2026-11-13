package com.mk.contractservice.infrastructure.persistence.lamal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface LamalAffiliationJpaRepository extends JpaRepository<LamalAffiliationJpaEntity, UUID> {

    @Modifying
    @Query(value = """
            insert into contracts.lamal (person_id)
            select p.id from contracts.person p where p.id in (:ids) order by p.id
            on conflict (person_id) do nothing
            """, nativeQuery = true)
    void insertMissingForPersons(@Param("ids") Set<UUID> ids);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    List<LamalAffiliationJpaEntity> findAllByPersonIdInOrderByPersonId(Set<UUID> personIds);
}
