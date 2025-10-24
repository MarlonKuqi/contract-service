package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonName;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "company")
@DiscriminatorValue("COMPANY")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder(toBuilder = false)
public class Company extends Client {
    @Column(name = "company_identifier", nullable = false, updatable = false, unique = true, length = 64)
    private String companyIdentifier;

    public Company(final UUID id, final PersonName name, final Email email, final PhoneNumber phone, final String companyIdentifier) {
        super(id, name, email, phone);
        this.companyIdentifier = companyIdentifier;
    }
}

