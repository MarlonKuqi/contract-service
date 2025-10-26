package com.mk.contractservice.domain.client;


import com.mk.contractservice.domain.valueobject.Email;
import com.mk.contractservice.domain.valueobject.ClientName;
import com.mk.contractservice.domain.valueobject.PhoneNumber;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "person")
@DiscriminatorValue("PERSON")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Person extends Client {
    @Column(name = "birth_date", nullable = false, updatable = false)
    private LocalDate birthDate;

    public Person(final ClientName name, final Email email, final PhoneNumber phone, final LocalDate birthDate) {
        super(name, email, phone);
        this.birthDate = birthDate;
    }
}

