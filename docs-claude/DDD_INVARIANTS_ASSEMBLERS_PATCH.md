# 🎯 DDD - Invariants, Assemblers et PATCH

## 1. Séparation des Assemblers ✅

### Question
> "est-ce que dans ClientAssembler on ne devrait pas appeler PersonAssembler ?"

### Réponse : OUI, absolument !

#### Problème avant

`ClientAssembler` connaissait tous les détails de `Person` ET `Company`.

```java
// ❌ AVANT - Violation SRP
public class ClientAssembler {
    public Client toDomain(ClientJpaEntity entity) {
        return switch (entity) {
            case PersonJpaEntity pe -> Person.reconstitute(
                pe.getId(),
                ClientName.of(pe.getName()),
                Email.of(pe.getEmail()),
                ...  // Détails de Person ici
            );
            case CompanyJpaEntity ce -> Company.reconstitute(...);  // Détails de Company ici
        };
    }
}
```

**Problème** : `ClientAssembler` a 2 raisons de changer (Person OU Company).

#### Solution appliquée ✅

**Pattern Strategy/Delegation** :

```
ClientAssembler (Coordinateur)
    ├─> PersonAssembler (Spécialisé Person)
    └─> CompanyAssembler (Spécialisé Company)
```

**Code** :

```java
// ✅ APRÈS - Respect SRP
@Component
public class ClientAssembler {
    private final PersonAssembler personAssembler;
    private final CompanyAssembler companyAssembler;
    
    public ClientJpaEntity toJpaEntity(Client domain) {
        return switch (domain) {
            case Person p -> personAssembler.toJpaEntity(p);
            case Company c -> companyAssembler.toJpaEntity(c);
        };
    }
    
    public Client toDomain(ClientJpaEntity entity) {
        return switch (entity) {
            case PersonJpaEntity pe -> personAssembler.toDomain(pe);
            case CompanyJpaEntity ce -> companyAssembler.toDomain(ce);
        };
    }
}
```

**PersonAssembler** (détails Person) :
```java
@Component
public class PersonAssembler {
    public Person toDomain(PersonJpaEntity entity) {
        return Person.reconstitute(
            entity.getId(),
            ClientName.of(entity.getName()),
            Email.of(entity.getEmail()),
            PhoneNumber.of(entity.getPhone()),
            PersonBirthDate.of(entity.getBirthDate())
        );
    }
    
    public PersonJpaEntity toJpaEntity(Person person) {
        PersonJpaEntity entity = PersonJpaEntity.create(...);
        if (person.getId() != null) {
            entity.setId(person.getId());
        }
        return entity;
    }
}
```

**CompanyAssembler** (détails Company) : Identique pour Company.

### Avantages

| Aspect | Avant | Après |
|--------|-------|-------|
| **SRP** | ❌ 1 classe, 2 responsabilités | ✅ 3 classes, 1 responsabilité chacune |
| **Testabilité** | ⚠️ Tester Person ET Company ensemble | ✅ Tester Person et Company séparément |
| **Maintenance** | ⚠️ Modifier Person affecte Company | ✅ Modifier Person n'affecte que PersonAssembler |
| **Réutilisabilité** | ❌ PersonAssembler pas réutilisable | ✅ PersonAssembler réutilisable ailleurs |

---

## 2. Implémentation du PATCH

### Question
> "pour le patch, de manière générale, est-ce la bonne manière d'implémenter le patch ?"

### Analyse de l'implémentation actuelle

```java
@Transactional
public Client patchClient(UUID id, ClientName name, Email email, PhoneNumber phone) {
    Client client = getClientById(id);
    
    // Early return si aucun changement
    if (name == null && email == null && phone == null) {
        return client;
    }
    
    // Création d'une nouvelle instance avec toBuilder()
    Client updatedClient = switch (client) {
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
    
    return clientRepo.save(updatedClient);
}
```

### ✅ Bonnes pratiques respectées

1. **RFC 7386 (JSON Merge Patch)** ✅
   - Modifie uniquement les champs fournis
   - Ignore les champs null/absents
   - Retourne la ressource complète

2. **Immutabilité** ✅
   - Crée une nouvelle instance
   - Instance originale inchangée

3. **Validation** ✅
   - Les invariants sont vérifiés dans le constructeur via `checkInvariants()`
   - Impossible de créer un état invalide

4. **Performance** ✅
   - Early return si aucun changement
   - Une seule instance créée (avec `toBuilder()`)

### ❌ Endpoint par ressource patchée ?

**Question** : "on ne doit pas avoir un endpoint par ressource patchée ?"

**Réponse** : **NON, pas nécessaire** dans votre cas.

#### Option A : Endpoint par champ (micro-PATCH)

```java
// ❌ Trop granulaire pour votre contexte
PATCH /clients/{id}/name
PATCH /clients/{id}/email
PATCH /clients/{id}/phone
```

**Quand utiliser** :
- ✅ Ressources très larges (100+ champs)
- ✅ Opérations métier spécifiques (ex: "valider email")
- ✅ Permissions granulaires par champ

**Votre contexte** :
- ❌ Seulement 3 champs modifiables
- ❌ Pas de logique métier spécifique par champ
- ❌ Permissions globales (pas par champ)

#### Option B : Endpoint PATCH global (votre choix) ✅

```java
// ✅ Correct pour 3 champs
PATCH /clients/{id}
{
  "name": "New Name",
  "email": "new@example.com"
  // phone absent = non modifié
}
```

**Avantages** :
- ✅ Simple
- ✅ Flexible (1 à N champs)
- ✅ Standard REST

### Invariants toujours vérifiés ?

**Question** : "l'invariant est toujours vérifié ?"

**Réponse** : **OUI** ✅

Le constructeur appelle `checkInvariants()` qui vérifie **tous** les champs :

```java
@Builder(toBuilder = true)
private Person(...) {
    super(id, name, email, phone);  // Valide les champs parent
    this.birthDate = birthDate;
    checkInvariants();  // Valide les champs Person
}

private void checkInvariants() {
    // Vérifie birthDate (champ Person)
    if (birthDate == null) {
        throw new IllegalArgumentException("Birth date must not be null");
    }
    // Les champs parent (name, email, phone) sont déjà validés dans Client
}
```

**Garantie** : Impossible de créer un `Person` invalide, même via PATCH.

---

## 3. Invariants DDD

### Question
> "je propose de vérifier les invariants à la fin de la méthode... que dit DDD concernant les invariants ?"

### Réponse : DDD exige la validation DANS l'entité

#### Principe DDD : "Always Valid"

> "An entity should never be in an invalid state."  
> — Eric Evans, Domain-Driven Design

**Règle** : Les invariants DOIVENT être vérifiés dans le constructeur de l'entité.

#### Bonne pratique : Méthode dédiée

**✅ Ce qui a été fait** :

```java
@Builder(toBuilder = true)
private Person(UUID id, ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
    super(id, name, email, phone);
    this.birthDate = birthDate;
    checkInvariants();  // ✅ Appelé à la fin
}

/**
 * Vérifie les invariants métier de Person.
 */
private void checkInvariants() {
    if (birthDate == null) {
        throw new IllegalArgumentException("Birth date must not be null");
    }
    // Futurs invariants métier ici (ex: âge minimum, cohérence dates, etc.)
}
```

#### Pourquoi à la FIN du constructeur ?

**Raison** : Tous les champs doivent être assignés AVANT de les valider.

```java
// ❌ MAUVAIS - birthDate pas encore assigné
private Person(...) {
    super(...);
    checkInvariants();     // birthDate = null ici !
    this.birthDate = birthDate;
}

// ✅ BON - birthDate assigné avant validation
private Person(...) {
    super(...);
    this.birthDate = birthDate;
    checkInvariants();     // birthDate est assigné ✅
}
```

#### Nom de la méthode

**Votre proposition** : `controlerRegles()`, `controlerInvariants()`

**Recommandation** : `checkInvariants()` ou `validateInvariants()`

**Pourquoi** :
- ✅ Standard dans la littérature DDD
- ✅ Anglais = langue du code
- ✅ Plus court et clair

**Alternatives** :
- `checkInvariants()` ✅ (le plus courant)
- `validateInvariants()` ✅
- `ensureInvariants()` ✅
- `assertInvariants()` ⚠️ (confusion avec assertions de test)

#### Invariants dans une classe séparée ?

**Question implicite** : "ils ne doivent pas être à part de la classe ?"

**Réponse** : **NON**, les invariants DOIVENT rester dans l'entité.

**Pourquoi** :
1. **Encapsulation** : L'entité connaît ses propres règles
2. **Cohésion** : Données + règles ensemble
3. **Impossibilité de bypass** : Pas de validation externe contournable

**Exception** : Invariants complexes partagés → Specification Pattern

```java
// ✅ Pour invariants simples (votre cas)
private void checkInvariants() {
    if (birthDate == null) {
        throw new IllegalArgumentException(...);
    }
}

// ✅ Pour invariants complexes partagés (si besoin futur)
public class AgeSpecification {
    public boolean isSatisfiedBy(Person person) {
        return person.getAge() >= 18;
    }
}
```

---

## Synthèse des changements

### 1. ✅ Séparation des Assemblers

**Fichiers créés** :
- `PersonAssembler.java` - Mapping Person ↔ PersonJpaEntity
- `CompanyAssembler.java` - Mapping Company ↔ CompanyJpaEntity

**Fichier modifié** :
- `ClientAssembler.java` - Délègue aux assemblers spécialisés

**Avantage** : Meilleure séparation des responsabilités (SRP).

### 2. ✅ Validation des invariants

**Fichiers modifiés** :
- `Person.java` - Méthode `checkInvariants()` appelée à la fin du constructeur
- `Company.java` - Méthode `checkInvariants()` appelée à la fin du constructeur

**Avantage** : Code auto-documenté, évolution facilitée.

### 3. ✅ PATCH bien implémenté

**Confirmation** :
- ✅ Bonnes pratiques REST respectées
- ✅ Invariants toujours vérifiés
- ✅ Immutabilité garantie
- ✅ Performance optimisée

**Pas de changement nécessaire** : L'implémentation actuelle est correcte.

---

## Principes DDD appliqués

### ✅ Always Valid

Les entités ne peuvent jamais exister dans un état invalide.

```java
// ✅ Impossible de créer un Person sans birthDate
Person person = Person.of(name, email, phone, null);  // ❌ Exception levée
```

### ✅ Encapsulation

Les règles métier sont dans l'entité, pas dans le service.

```java
// ❌ MAUVAIS - Validation dans le service
public void updatePerson(...) {
    if (birthDate == null) throw new Exception();  // ❌ Logique hors de l'entité
    person.setBirthDate(birthDate);
}

// ✅ BON - Validation dans l'entité
public void updatePerson(...) {
    Person updated = person.withBirthDate(birthDate);  // ✅ checkInvariants() appelé
}
```

### ✅ Immutability

Modifications = nouvelles instances.

```java
// ✅ Pattern immutable
Client updated = client.toBuilder().name(newName).build();
```

### ✅ Single Responsibility

Chaque assembler ne gère qu'un seul type d'entité.

```java
// ✅ PersonAssembler ne connaît que Person
// ✅ CompanyAssembler ne connaît que Company
// ✅ ClientAssembler coordonne
```

---

## Checklist finale

- [x] ✅ Assemblers séparés par type (PersonAssembler, CompanyAssembler)
- [x] ✅ Invariants validés dans `checkInvariants()`
- [x] ✅ Méthode `checkInvariants()` appelée à la FIN du constructeur
- [x] ✅ PATCH implémenté correctement (RFC 7386)
- [x] ✅ Immutabilité garantie avec `toBuilder()`
- [x] ✅ Performance optimisée (1 seule instance créée)
- [x] ✅ Documentation des invariants (Javadoc)

**Conclusion** : Architecture DDD solide et conforme aux best practices ! 🎯

**Date** : 2025-01-16
**Statut** : ✅ ARCHITECTURE OPTIMALE

