package com.mk.contractservice.infrastructure.persistence.client.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;


@Entity
@Table(name = "company")
@DiscriminatorValue("COMPANY")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class CompanyJpaEntity extends ClientJpaEntity {

    @Column(name = "company_identifier", nullable = false, length = 64, unique = true)
    String companyIdentifier;

    public CompanyJpaEntity(String name, String email, String phone, String companyIdentifier) {
        super(name, email, phone);
        this.companyIdentifier = companyIdentifier;
    }

    public static CompanyJpaEntity create(String name, String email, String phone, String companyIdentifier) {
        return new CompanyJpaEntity(name, email, phone, companyIdentifier);
    }
}
