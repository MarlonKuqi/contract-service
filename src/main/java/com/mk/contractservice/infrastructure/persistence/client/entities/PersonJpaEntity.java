package com.mk.contractservice.infrastructure.persistence.client.entities;

import com.mk.contractservice.domain.shared.Residence;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class PersonJpaEntity extends ClientJpaEntity {

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "residence", nullable = false, length = 2)
    private Residence residence;

    public PersonJpaEntity(final String name,
            final String email,
            final String phone,
            final LocalDate birthDate,
            final Residence residence) {
        super(name, email, phone);
        this.birthDate = birthDate;
        this.residence = residence;
    }
}
