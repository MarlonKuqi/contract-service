package com.mk.contractservice.domain.shared;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

import static com.mk.contractservice.domain.shared.Assert.notNull;

public sealed interface ContractHolder permits ContractHolder.Person, ContractHolder.Company {

    ClientKind kind();

    boolean isNotCrossBorder();

    static Person person(@Nullable final LocalDate birthDate, @Nullable final Residence residence) {
        return new Person(notNull(birthDate), notNull(residence));
    }

    static Company company() {
        return new Company();
    }

    record Person(LocalDate birthDate, Residence residence) implements ContractHolder {

        public Person {
            notNull(birthDate);
            notNull(residence);
        }

        @Override
        public ClientKind kind() {
            return ClientKind.PERSON;
        }

        @Override
        public boolean isNotCrossBorder() {
            return residence.isNotCrossBorder();
        }
    }

    record Company() implements ContractHolder {

        @Override
        public ClientKind kind() {
            return ClientKind.COMPANY;
        }

        @Override
        public boolean isNotCrossBorder() {
            return true;
        }
    }
}
