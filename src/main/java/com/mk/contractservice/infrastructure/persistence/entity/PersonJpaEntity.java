package com.mk.contractservice.infrastructure.persistence.entity;

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
@Table(name = "person")
@DiscriminatorValue("PERSON")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class PersonJpaEntity extends ClientJpaEntity {

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    public PersonJpaEntity(String name, String email, String phone, LocalDate birthDate) {
        super(name, email, phone);
        this.birthDate = birthDate;
    }

    public static PersonJpaEntity create(String name, String email, String phone, LocalDate birthDate) {
        return new PersonJpaEntity(name, email, phone, birthDate);
    }
}
