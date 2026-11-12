package com.mk.contractservice.domain.shared;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

import static com.mk.contractservice.domain.shared.Assert.notNull;

public sealed interface ContractHolder permits ContractHolder.Person, ContractHolder.Company {

    ClientKind kind();

    static Person person(@Nullable final LocalDate birthDate) {
        return new Person(notNull(birthDate));
    }

    static Company company() {
        return new Company();
    }

    record Person(LocalDate birthDate) implements ContractHolder {

        public Person {
            notNull(birthDate);
        }

        @Override
        public ClientKind kind() {
            return ClientKind.PERSON;
        }
    }

    record Company() implements ContractHolder {

        @Override
        public ClientKind kind() {
            return ClientKind.COMPANY;
        }
    }
}
