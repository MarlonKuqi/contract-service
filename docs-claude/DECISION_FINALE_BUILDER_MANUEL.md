# ✅ SOLUTION DÉFINITIVE - Builder Manuel

## Parcours et leçons apprises

Nous avons tenté TOUTES les approches possibles avec Lombok pour éviter le builder manuel.

### Tentatives échouées

1. ❌ `@Builder` simple → Incompatible avec l'héritage
2. ❌ `@SuperBuilder` → Bypass la validation du constructeur
3. ❌ `@FieldDefaults` → Conflits avec l'héritage
4. ❌ Champs `final` partout → Incompatible avec Lombok + héritage
5. ❌ Surcharge du builder généré → Impossible d'accéder aux champs

### Conclusion

**Il n'existe AUCUNE solution Lombok compatible avec :**
- ✅ Héritage
- ✅ Validation stricte
- ✅ Principe DDD "Always Valid"

## Solution retenue : Builder manuel

### Architecture finale

```
Client (abstract)
  ├─ Person (builder manuel ~50 lignes)
  └─ Company (builder manuel ~50 lignes)
```

### Code minimal mais complet

#### Client.java

```java
@Getter
public abstract sealed class Client permits Person, Company {
    
    public static final String NULL_NAME_MSG = "ClientName must not be null";
    public static final String NULL_EMAIL_MSG = "Email must not be null";
    public static final String NULL_PHONE_MSG = "PhoneNumber must not be null";

    private UUID id;
    private ClientName name;
    private Email email;
    private PhoneNumber phone;

    protected Client(UUID id, ClientName name, Email email, PhoneNumber phone) {
        // Validation TOUJOURS appelée
        if (name == null) throw new IllegalArgumentException(NULL_NAME_MSG);
        if (email == null) throw new IllegalArgumentException(NULL_EMAIL_MSG);
        if (phone == null) throw new IllegalArgumentException(NULL_PHONE_MSG);
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
}
```

#### Person.java

```java
@Getter
public final class Person extends Client {

    private final PersonBirthDate birthDate;

    private Person(UUID id, ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
        super(id, name, email, phone);  // ✅ Validation Client
        this.birthDate = birthDate;
        checkInvariants();  // ✅ Validation Person
    }

    private void checkInvariants() {
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date must not be null");
        }
    }

    // Factory methods
    public static Person of(ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
        return new Person(null, name, email, phone, birthDate);
    }

    public static Person reconstitute(UUID id, ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
        if (id == null) throw new IllegalArgumentException("ID must not be null when reconstituting");
        return new Person(id, name, email, phone, birthDate);
    }

    public Person withCommonFields(ClientName name, Email email, PhoneNumber phone) {
        return new Person(this.getId(), name, email, phone, this.birthDate);
    }

    // Builder manuel
    public static PersonBuilder builder() {
        return new PersonBuilder();
    }

    public PersonBuilder toBuilder() {
        return new PersonBuilder()
                .id(this.getId())
                .name(this.getName())
                .email(this.getEmail())
                .phone(this.getPhone())
                .birthDate(this.birthDate);
    }

    public static class PersonBuilder {
        private UUID id;
        private ClientName name;
        private Email email;
        private PhoneNumber phone;
        private PersonBirthDate birthDate;

        public PersonBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public PersonBuilder name(ClientName name) {
            this.name = name;
            return this;
        }

        public PersonBuilder email(Email email) {
            this.email = email;
            return this;
        }

        public PersonBuilder phone(PhoneNumber phone) {
            this.phone = phone;
            return this;
        }

        public PersonBuilder birthDate(PersonBirthDate birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        public Person build() {
            return new Person(id, name, email, phone, birthDate);
        }
    }
}
```

#### Company.java

Identique à Person, avec `companyIdentifier` au lieu de `birthDate`.

## Avantages du builder manuel

### ✅ Validation garantie à 100%

```java
Person person = Person.builder()
    .name(null)  // ← null
    .build();    // ← Lance IllegalArgumentException ✅
```

Le constructeur privé est **TOUJOURS** appelé par `build()`.

### ✅ Contrôle total

- On sait exactement ce qui se passe
- Pas de "magie" Lombok
- Debuggable facilement
- Maintenable

### ✅ toBuilder() fonctionne

```java
Person updated = person.toBuilder()
    .email(newEmail)
    .build();
```

Copie automatique de tous les champs.

### ✅ DDD "Always Valid" respecté

Impossible de créer un objet invalide.

## Coût accepté

### Lignes de code

| Fichier | Lignes |
|---------|--------|
| Person.java | ~110 lignes |
| Company.java | ~110 lignes |
| **Total** | ~220 lignes |

**Dont** :
- 50 lignes de builder par classe
- 60 lignes de logique métier par classe

### Trade-off

**220 lignes de code** pour garantir :
- ✅ Validation à 100%
- ✅ Principe DDD respecté
- ✅ Code fiable et testable
- ✅ Maintenabilité excellente

**vs**

**2 annotations Lombok** mais :
- ❌ Validation ignorée
- ❌ Principe DDD violé
- ❌ Objets invalides possibles
- ❌ Bugs en production

## Principe appliqué

> "Explicit is better than implicit."  
> — The Zen of Python

Le builder manuel est **explicite**, **clair**, et **fiable**.

## Checklist finale

- [x] Builder manuel dans Person
- [x] Builder manuel dans Company
- [x] Constructeurs privés avec validation
- [x] `checkInvariants()` appelé
- [x] Factory methods : `of()`, `reconstitute()`
- [x] Méthode de modification : `withCommonFields()`
- [x] `toBuilder()` implémenté
- [x] Tests de validation passent ✅
- [x] Code compile sans erreur ✅
- [x] DDD "Always Valid" respecté ✅

## Conclusion

**Builder manuel = SEULE solution viable pour DDD strict.**

Le coût de 50 lignes de code par classe est **largement** compensé par :
- Validation garantie
- Fiabilité totale
- Respect des principes DDD
- Maintenabilité

**Lombok est excellent pour les DTOs, pas pour le domain model DDD.**

**Date** : 2025-01-16  
**Statut** : ✅ PRODUCTION-READY  
**Décision** : DÉFINITIVE ET IRRÉVOCABLE

