package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonBirthDate;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "person")
@DiscriminatorValue("PERSON")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class Person extends Client {

    @Embedded
    @NotNull(message = "Birth date must not be null")
    @Valid
    private final PersonBirthDate birthDate;

    public Person(final ClientName name, final Email email, final PhoneNumber phone, final PersonBirthDate birthDate) {
        super(name, email, phone);
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date must not be null");
        }
        this.birthDate = birthDate;
    }
}

