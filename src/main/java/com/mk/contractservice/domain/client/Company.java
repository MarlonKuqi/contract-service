package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "company")
@DiscriminatorValue("COMPANY")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends Client {
    @Column(name = "company_identifier", nullable = false, updatable = false, unique = true, length = 64)
    private String companyIdentifier;

    public Company(final ClientName name, final Email email, final PhoneNumber phone, final String companyIdentifier) {
        super(name, email, phone);
        this.companyIdentifier = companyIdentifier;
    }
}

