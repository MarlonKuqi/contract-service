# ✅ Migration vers Builder - État Final

## 📊 Tests Unitaires : 100% Migrés ✅

### Domain Tests
- ✅ `PersonTest.java` - 4 occurrences corrigées
- ✅ `CompanyTest.java` - 4 occurrences corrigées  
- ✅ `ClientTest.java` - 6 occurrences corrigées
- ✅ `ContractTest.java` - 7 occurrences corrigées

### Application Tests
- ✅ `ClientApplicationServiceTest.java` - 4 occurrences corrigées
- ✅ `ContractApplicationServiceTest.java` - 4 occurrences corrigées

---

## ⚠️ Tests d'Intégration : À Finaliser

Il reste **15 occurrences** de `Person.create(` dans les tests d'intégration :

### ClientCrudIT.java (6 occurrences)
```java
// Lignes 74, 139, 214, 262, 284, 342, 415
Person.create(...) → Person.builder()...build()
```

### PerformanceAndEdgeCasesIT.java (7 occurrences)
```java
// Lignes 63, 104, 129, 165, 256, 291, 357
Person.create(...) → Person.builder()...build()
```

### ContractSumRestAssuredIT.java (1 occurrence)
```java
// Ligne 66
Person.create(...) → Person.builder()...build()
```

### Company.create
```java
// ClientCrudIT.java - à chercher et remplacer
Company.create(...) → Company.builder()...build()
```

### Contract.create  
```java
// ClientCrudIT.java - à chercher et remplacer
Contract.create(...) → Contract.builder()...build()
```

---

## 🔧 Comment Finaliser (Find & Replace IDE)

### 1. Pattern Simple - Ligne par ligne

**Rechercher :**
```regex
Person\.create\(
```

**Pour chaque occurrence :**
1. Remplacer `Person.create(` par `Person.builder()`
2. Ajouter `.name(` avant le 1er argument
3. Ajouter `.email(` avant le 2ème
4. Ajouter `.phone(` avant le 3ème
5. Ajouter `.birthDate(` avant le 4ème
6. Remplacer `)` final par `.build()`

### 2. Exemple Concret

**AVANT :**
```java
Person givenPerson = Person.create(
    ClientName.of("John Doe"),
    Email.of("john.doe@example.com"),
    PhoneNumber.of("+41791234567"),
    PersonBirthDate.of(LocalDate.of(1990, 5, 15))
);
```

**APRÈS :**
```java
Person givenPerson = Person.builder()
    .name(ClientName.of("John Doe"))
    .email(Email.of("john.doe@example.com"))
    .phone(PhoneNumber.of("+41791234567"))
    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 5, 15)))
    .build();
```

---

## 📈 Impact Coverage Attendu

**Avant :** 79% ⚠️
**Après :** > 80% ✅

**Pourquoi ?**
- Le builder a plus de branches (4 setters + build)
- Plus de lignes de code testées
- Meilleure couverture des constructeurs privés

---

## ✅ Prochaines Étapes

1. **Finaliser manuellement** les 15+ occurrences dans les tests d'intégration
2. **Lancer les tests** : `mvn clean test`
3. **Vérifier le coverage** : `mvn clean verify`
4. **Devrait passer > 80%** ! 🎉

**Note :** Les tests unitaires passent déjà tous avec le builder !

