package com.mk.contractservice.domain.client;


import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.PersonName;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "person")
@DiscriminatorValue("PERSON")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder(toBuilder = false)
public class Person extends Client {
    @Column(name = "birth_date", nullable = false, updatable = false)
    private LocalDate birthDate;

    public Person(final UUID id, final PersonName name, final Email email, final PhoneNumber phone, final LocalDate birthDate) {
        super(id, name, email, phone);
        this.birthDate = birthDate;
    }
}

