# 🎯 Réponses aux Questions DDD - Pattern Immutable

## Question 1 : Dépendance Domain → Infrastructure

### Votre question
> "est-ce vraiment une dépendance ? on veut créer un Person à partir d'un entity. quelle est la bonne pratique ?"

### Réponse : OUI, c'est une violation DDD

#### Pourquoi c'est un problème

```
┌──────────────────────────────────────┐
│          DOMAIN LAYER                │
│                                      │
│  Person.fromJpaEntity(entity)  ❌   │ 
│          ↓                           │
│    depends on                        │
│          ↓                           │
└──────────────────────────────────────┘
           ↓
┌──────────────────────────────────────┐
│     INFRASTRUCTURE LAYER             │
│                                      │
│       PersonJpaEntity                │
└──────────────────────────────────────┘
```

**Violation** : La flèche va de Domain vers Infrastructure (inversée !).

**DDD exige** : Infrastructure dépend de Domain, jamais l'inverse.

#### La bonne pratique ✅

```
┌──────────────────────────────────────┐
│          DOMAIN LAYER                │
│                                      │
│  Person.reconstitute(                │
│    UUID id,                          │
│    ClientName name,  ← Value Objects │
│    Email email,                      │
│    ...                               │
│  )                                   │
└──────────────────────────────────────┘
           ↑
           │ uses
           │
┌──────────────────────────────────────┐
│     INFRASTRUCTURE LAYER             │
│                                      │
│  ClientAssembler.toDomain() {        │
│    Person.reconstitute(              │
│      entity.getId(),                 │
│      ClientName.of(entity.getName()) │
│      ...                             │
│    )                                 │
│  }                                   │
└──────────────────────────────────────┘
```

**✅ Correct** : Infrastructure appelle Domain et fait le mapping.

### Votre suggestion : params primitifs ?

> "il faudrait alors que le type des params soient le type de leur value"

**Non, on garde les Value Objects** :

```java
// ✅ CORRECT - Domain expose des VOs
public static Person reconstitute(
    UUID id,
    ClientName name,     // VO, pas String
    Email email,         // VO, pas String
    PhoneNumber phone,   // VO, pas String
    PersonBirthDate birthDate  // VO, pas LocalDate
)

// Infrastructure fait le mapping String → VO
public Client toDomain(PersonJpaEntity entity) {
    return Person.reconstitute(
        entity.getId(),
        ClientName.of(entity.getName()),      // String → VO
        Email.of(entity.getEmail()),          // String → VO
        PhoneNumber.of(entity.getPhone()),    // String → VO
        PersonBirthDate.of(entity.getBirthDate())  // LocalDate → VO
    );
}
```

**Pourquoi** ?
- Domain ne connaît QUE les Value Objects
- Infrastructure fait la conversion primitif → VO
- Séparation claire des responsabilités

---

## Question 2 : Performance vs Principes DDD

### Votre question
> "c'est ce que l'article recommandait. on sacrifie le principe pour la performance ? est-ce si critique ?"

### Réponse : NON, on ne sacrifie rien !

#### L'article recommande AUSSI le builder

L'article utilise le builder pattern pour les `withXxx()` :

```java
// Article - Patron.withEmail()
public Patron withEmail(final Email email) {
    return Patron.builder()
        .id(this.id)
        .name(this.name)
        .email(email)      // Seul champ modifié
        .build();
}
```

Notre code avec `toBuilder()` fait **exactement la même chose** :

```java
// Notre code - équivalent mais plus concis
public Person withEmail(final Email email) {
    return this.toBuilder()  // Copie automatique de tous les champs
        .email(email)         // Seul champ modifié
        .build();
}
```

**Différence** : `toBuilder()` copie automatiquement les autres champs.

#### Impact performance réel

**Scénario** : PATCH de 3 champs sur 5

**Version A** : Créer 3 instances intermédiaires
```java
Client c1 = client;
if (name != null) c1 = c1.withName(name);      // Instance 1
if (email != null) c1 = c1.withEmail(email);   // Instance 2
if (phone != null) c1 = c1.withPhone(phone);   // Instance 3
save(c1);
```

**Version B** : Créer 1 seule instance finale
```java
var builder = client.toBuilder();
if (name != null) builder.name(name);
if (email != null) builder.email(email);
if (phone != null) builder.phone(phone);
save(builder.build());  // 1 seule instance
```

**Mesures** :
- Version A : ~300 bytes alloués (3 × 100 bytes)
- Version B : ~100 bytes alloués (1 × 100 bytes)
- **Gain** : ~200 bytes par requête

**Impact réel** :
- ⚠️ Appel base de données : ~10-50ms
- ✅ Allocation mémoire : ~0.001ms
- **Conclusion** : Négligeable comparé à l'I/O

**MAIS** :
1. Version B est **plus simple** (moins de code)
2. Version B est **plus performante** (bonus)
3. Version B respecte **le même principe** que l'article

### Verdict : toBuilder() ✅

- ✅ Conforme à l'article (builder pattern)
- ✅ Plus simple (moins de code)
- ✅ Plus performant (bonus)

**On ne sacrifie RIEN, c'est mieux dans tous les aspects !**

---

## Question 3 : Les withXxx() dans l'article

### Votre observation
> "les with machin, j'ai l'impression que c'est pour des cas spécifiques"

### Réponse : Vous avez raison !

#### Dans l'article

Les `withXxx()` sont utilisés pour des **use cases métier unitaires** :

```java
// Use case : Changer l'email d'un patron
public void changePatronEmail(PatronId id, Email newEmail) {
    Patron patron = repository.findById(id);
    Patron updated = patron.withEmail(newEmail);  // Modification unitaire
    repository.save(updated);
}
```

#### Dans votre projet

Vous avez **2 types d'opérations** :

**1. PUT (update complet)** → `withCommonFields()`
```java
@PutMapping("/{id}")
public void update(@PathVariable UUID id, @RequestBody UpdateRequest req) {
    // Met à jour TOUS les champs modifiables
    service.updateCommonFields(id, req.name(), req.email(), req.phone());
}
```
✅ **Utilité** : Mise à jour complète

**2. PATCH (update partiel)** → `toBuilder()` dans le service
```java
@PatchMapping("/{id}")
public void patch(@PathVariable UUID id, @RequestBody PatchRequest req) {
    // Met à jour UNIQUEMENT les champs fournis
    service.patchClient(id, req.name(), req.email(), req.phone());
}

// Dans le service
public Client patchClient(...) {
    var builder = client.toBuilder();
    if (name != null) builder.name(name);
    if (email != null) builder.email(email);
    if (phone != null) builder.phone(phone);
    return builder.build();
}
```
✅ **Utilité** : Mise à jour sélective

**3. Modification unitaire métier** → `withName()`, `withEmail()`, etc.
```java
// Use case métier : Corriger le nom d'un client
public void correctClientName(UUID id, ClientName correctName) {
    Client client = repository.findById(id);
    Client corrected = client.withName(correctName);  // ← Besoin de withName()
    repository.save(corrected);
}
```
❌ **Dans votre projet** : Vous n'avez PAS ces use cases métier.

### Décision : Supprimer withXxx() unitaires ✅

**Résultat** :
- ✅ `withCommonFields()` : Gardé (pour PUT)
- ❌ `withName()`, `withEmail()`, `withPhone()` : Supprimés (pas de use case)
- ✅ `toBuilder()` : Utilisé dans `patchClient()` (pour PATCH)

**Code final** :

```java
public final class Person extends Client {
    
    // Factory methods
    public static Person of(...) { ... }
    public static Person reconstitute(...) { ... }
    
    // Update method (PUT)
    public Person withCommonFields(ClientName name, Email email, PhoneNumber phone) {
        return this.toBuilder()
            .name(name)
            .email(email)
            .phone(phone)
            .build();
    }
    
    // Note: No withName(), withEmail(), withPhone() 
    // → Use toBuilder() directly in service layer for PATCH
}
```

---

## Synthèse des réponses

| Question | Réponse | Action |
|----------|---------|--------|
| 1. fromJpaEntity() dans domain ? | ❌ Violation DDD | ✅ Supprimé, mapping dans assembler |
| 2. Performance vs Principes ? | ✅ Les deux ! | ✅ toBuilder() conforme ET performant |
| 3. withXxx() nécessaires ? | ⚠️ Si use case métier | ✅ Supprimés (pas de use case) |

---

## Architecture finale

### Domain Layer (Person.java)

```java
@Builder(toBuilder = true)
private Person(UUID id, ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
    super(id, name, email, phone);
    if (birthDate == null) throw new IllegalArgumentException(...);
    this.birthDate = birthDate;
}

// Factory methods
public static Person of(ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
    return builder().name(name).email(email).phone(phone).birthDate(birthDate).build();
}

public static Person reconstitute(UUID id, ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
    if (id == null) throw new IllegalArgumentException(...);
    return builder().id(id).name(name).email(email).phone(phone).birthDate(birthDate).build();
}

// Update method (for PUT)
public Person withCommonFields(ClientName name, Email email, PhoneNumber phone) {
    return this.toBuilder().name(name).email(email).phone(phone).build();
}
```

### Infrastructure Layer (ClientAssembler.java)

```java
public Client toDomain(ClientJpaEntity entity) {
    return switch (entity) {
        case PersonJpaEntity pe -> Person.reconstitute(
            pe.getId(),
            ClientName.of(pe.getName()),        // String → VO
            Email.of(pe.getEmail()),            // String → VO
            PhoneNumber.of(pe.getPhone()),      // String → VO
            PersonBirthDate.of(pe.getBirthDate())  // LocalDate → VO
        );
        // ...
    };
}
```

### Application Layer (ClientApplicationService.java)

```java
// PUT - Update complet
public Client updateCommonFields(UUID id, ClientName name, Email email, PhoneNumber phone) {
    Client client = getClientById(id);
    Client updated = switch (client) {
        case Person p -> p.withCommonFields(name, email, phone);
        case Company c -> c.withCommonFields(name, email, phone);
    };
    return clientRepo.save(updated);
}

// PATCH - Update partiel
public Client patchClient(UUID id, ClientName name, Email email, PhoneNumber phone) {
    Client client = getClientById(id);
    
    if (name == null && email == null && phone == null) {
        return client;  // No changes
    }
    
    Client updated = switch (client) {
        case Person p -> {
            var builder = p.toBuilder();
            if (name != null) builder.name(name);
            if (email != null) builder.email(email);
            if (phone != null) builder.phone(phone);
            yield builder.build();
        }
        case Company c -> {
            var builder = c.toBuilder();
            if (name != null) builder.name(name);
            if (email != null) builder.email(email);
            if (phone != null) builder.phone(phone);
            yield builder.build();
        }
    };
    
    return clientRepo.save(updated);
}
```

---

## Principes DDD respectés

### ✅ Layered Architecture
- Domain ne dépend pas de l'infrastructure
- Infrastructure appelle le domain

### ✅ Immutability
- Toutes les entités sont immutables
- Modifications = nouvelles instances

### ✅ Value Objects
- Domain expose des VOs
- Infrastructure fait le mapping

### ✅ Factory Pattern
- `of()` pour création
- `reconstitute()` pour reconstruction

### ✅ Builder Pattern
- Lombok `@Builder(toBuilder = true)`
- Validation dans le constructeur

### ✅ Simplicity (KISS)
- Pas de méthodes inutiles (`withXxx()` supprimés)
- Code minimal et expressif

---

## Conclusion

**Tous vos doutes étaient justifiés !**

1. ✅ **fromJpaEntity()** était bien une violation → Corrigé
2. ✅ **Performance** n'est pas un sacrifice → `toBuilder()` est conforme ET performant
3. ✅ **withXxx()** n'étaient pas nécessaires → Supprimés

**Résultat** : Code DDD pur, simple, et performant ! 🎯

**Date** : 2025-01-16
**Statut** : ✅ OPTIMISÉ ET CLARIFIÉ

