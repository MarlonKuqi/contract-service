# ✅ Progression de la correction des tests

## Fichiers corrigés

### ✅ Tests d'application
- **ClientApplicationServiceTest.java** - ✅ TERMINÉ (14 occurrences corrigées)
- **ContractApplicationServiceTest.java** - ✅ TERMINÉ (1 occurrence corrigée)

### ✅ Tests de domaine  
- **ClientTest.java** - ✅ TERMINÉ (déjà corrigé précédemment)
- **PersonTest.java** - ✅ TERMINÉ (déjà corrigé précédemment)
- **CompanyTest.java** - ✅ TERMINÉ (déjà corrigé précédemment)

## Fichiers restants à corriger

### ⏳ Tests d'intégration

1. **PerformanceAndEdgeCasesIT.java** - 7 occurrences
2. **ContractSumRestAssuredIT.java** - 1 occurrence  
3. **ContractPaginationIT.java** - 1 occurrence
4. **ContractLifecycleIT.java** - 1 occurrence
5. **ContractIsActiveConsistencyIT.java** - 1 occurrence
6. **ClientCrudIT.java** - 9 occurrences

**Total restant** : ~20 occurrences dans 6 fichiers

## Pattern de remplacement

### Avec ID (objets existants en base)
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

### Sans ID (nouveaux objets)
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

## Prochaines étapes

Les tests d'intégration doivent être corrigés un par un. La plupart utilisent `Person.builder()` sans ID, donc ils peuvent utiliser `Person.of()`.

Pour les objets sauvegardés directement (`clientRepository.save(Person.builder()...)`), il faut utiliser `Person.of()` car l'ID sera généré par JPA.

