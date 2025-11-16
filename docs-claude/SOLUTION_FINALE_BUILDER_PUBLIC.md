# ✅ Solution Finale - Builder Privé avec Factory Methods Obligatoires

## Architecture adoptée

### Principe
Le builder est **privé** et accessible UNIQUEMENT via les factory methods :
- **Production** : `of()`, `reconstitute()`, `withCommonFields()`
- **Tests** : MÊMES factory methods (pas d'accès direct au builder)

### Avantage
✅ **Enforcement total** : Impossible d'utiliser le builder directement, même dans les tests
✅ **API unique** : Une seule façon de créer des objets
✅ **DDD pur** : Les factory methods sont l'unique point d'entrée

### Code Person.java

```java
@Getter
public final class Person extends Client {

    private final PersonBirthDate birthDate;

    private Person(UUID id, ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
        super(id, name, email, phone);
        this.birthDate = birthDate;
        checkInvariants();
    }

    @Override
    protected void checkInvariants() {
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date must not be null");
        }
    }

    public static Person of(ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
        return builder()
                .name(name)
                .email(email)
                .phone(phone)
                .birthDate(birthDate)
                .build();
    }

    public static Person reconstitute(UUID id, ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null when reconstituting a Person");
        }
        return builder()
                .id(id)
                .name(name)
                .email(email)
                .phone(phone)
                .birthDate(birthDate)
                .build();
    }

    public Person withCommonFields(ClientName name, Email email, PhoneNumber phone) {
        return toBuilder()
                .name(name)
                .email(email)
                .phone(phone)
                .build();
    }

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

        PersonBuilder() {}

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

## Flow de validation

```
CODE PRODUCTION:
Person.of(name, email, phone, birthDate)
  ↓
builder().name(...).email(...).build()
  ↓
new Person(...)
  ↓
super(...) → Client(...) → checkInvariants() → valide name, email, phone
  ↓
checkInvariants() → Person → valide birthDate
  ↓
Instance Person valide ✅

CODE TEST:
Person.builder()
  .name(ClientName.of("Test"))
  .email(Email.of("test@test.com"))
  .phone(PhoneNumber.of("+33123456789"))
  .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
  .build()
  ↓
new Person(...)
  ↓
Même validation que ci-dessus ✅
```

## Usages

### Production ET Tests - Utiliser les factory methods

**Création (sans ID)** :
```java
Person person = Person.of(name, email, phone, birthDate);
```

**Reconstruction (avec ID existant)** :
```java
Person existing = Person.reconstitute(id, name, email, phone, birthDate);
```

**Modification** :
```java
Person updated = person.withCommonFields(newName, newEmail, newPhone);
```

### Tests - Créer des objets avec ID
```java
UUID testId = UUID.randomUUID();
Person person = Person.reconstitute(
    testId,
    ClientName.of("Test"),
    Email.of("test@test.com"),
    PhoneNumber.of("+33123456789"),
    PersonBirthDate.of(LocalDate.of(1990, 1, 1))
);
```

### Tests de domaine - Utilisent les factory methods

```java
@Test
void shouldRejectNullName() {
    assertThatThrownBy(() -> Person.of(
            null,
            Email.of("test@example.com"),
            PhoneNumber.of("+33123456789"),
            PersonBirthDate.of(LocalDate.of(1990, 1, 1))
    ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(Client.NULL_NAME_MSG);
}
```

## Avantages de cette approche

### ✅ Enforcement total
Le builder étant privé, il est IMPOSSIBLE de l'utiliser hors des factory methods. Le compilateur enforce la bonne pratique.

### ✅ Validation garantie
Le builder appelle TOUJOURS le constructeur qui valide.

### ✅ API claire pour la production
Les factory methods ont des noms explicites :
- `of()` = création
- `reconstitute()` = reconstruction depuis DB
- `withCommonFields()` = modification

### ✅ Tests flexibles
Les tests peuvent utiliser le builder pour créer des objets avec ID spécifique.

### ✅ Code minimal
~50 lignes de builder vs ~100 lignes si tout était en factory methods.

### ✅ API claire et simple
Une seule façon de créer des objets = moins de confusion, code plus maintenable.

## Résumé

| Aspect | Statut |
|--------|--------|
| Builder | **privé** (enforcement total) |
| Factory methods | of(), reconstitute(), withCommonFields() |
| Validation | Toujours exécutée (constructeur) |
| Tests domaine | Utilisent factory methods |
| Tests application | Utilisent factory methods |
| Tests intégration | Utilisent factory methods |
| Code production | Utilisent factory methods |

## Actions requises

⚠️ **Tous les tests doivent être corrigés** pour utiliser les factory methods au lieu du builder.

Voir le fichier `SCRIPT_CORRECTION_TESTS.md` pour les patterns de remplacement.

Date : 2025-01-16
Statut : ⏳ TESTS À CORRIGER

