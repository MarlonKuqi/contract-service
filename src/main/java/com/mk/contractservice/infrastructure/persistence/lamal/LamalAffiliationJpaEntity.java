package com.mk.contractservice.infrastructure.persistence.lamal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "lamal", schema = "contracts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LamalAffiliationJpaEntity {

    @Id
    @Column(name = "person_id", nullable = false, updatable = false)
    private UUID personId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
