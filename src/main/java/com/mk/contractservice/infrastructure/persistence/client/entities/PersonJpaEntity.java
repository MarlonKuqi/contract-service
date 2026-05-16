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

import java.time.LocalDate;

@Entity
@Table(name = "person", schema = "contracts")
@DiscriminatorValue("PERSON")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class PersonJpaEntity extends ClientJpaEntity {

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    public PersonJpaEntity(final String name, final String email, final String phone, final LocalDate birthDate) {
        super(name, email, phone);
        this.birthDate = birthDate;
    }

    public static PersonJpaEntity create(final String name, final String email, final String phone, final LocalDate birthDate) {
        return new PersonJpaEntity(name, email, phone, birthDate);
    }
}
