# Script de correction des tests

## Remplacement à faire

### Pattern 1: Builder avec ID → reconstitute()
```java
// AVANT
Person.builder()
    .id(uuid)
    .name(ClientName.of("Test"))
    .email(Email.of("test@test.com"))
    .phone(PhoneNumber.of("+33123456789"))
    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
    .build()

// APRÈS
Person.reconstitute(
    uuid,
    ClientName.of("Test"),
    Email.of("test@test.com"),
    PhoneNumber.of("+33123456789"),
    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
)
```

### Pattern 2: Builder sans ID → of()
```java
// AVANT
Person.builder()
    .name(ClientName.of("Test"))
    .email(Email.of("test@test.com"))
    .phone(PhoneNumber.of("+33123456789"))
    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
    .build()

// APRÈS
Person.of(
    ClientName.of("Test"),
    Email.of("test@test.com"),
    PhoneNumber.of("+33123456789"),
    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
)
```

### Pattern 3: Mock avec builder → of()
```java
// AVANT
when(service.create()).thenAnswer(inv -> Person.builder()
    .name(inv.getArgument(0))
    .email(inv.getArgument(1))
    .phone(inv.getArgument(2))
    .birthDate(inv.getArgument(3))
    .build());

// APRÈS
when(service.create()).thenAnswer(inv -> Person.of(
    inv.getArgument(0),
    inv.getArgument(1),
    inv.getArgument(2),
    inv.getArgument(3)
));
```

## Fichiers à corriger

1. ClientApplicationServiceTest.java
2. ContractApplicationServiceTest.java  
3. ClientCrudIT.java
4. PerformanceAndEdgeCasesIT.java
5. ContractSumRestAssuredIT.java
6. ContractPaginationIT.java
7. ContractLifecycleIT.java
8. ContractIsActiveConsistencyIT.java

Total: ~30-40 occurrences à corriger manuellement

