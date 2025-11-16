# 🎯 Builder vs toBuilder - Guide complet

## Différence entre `builder()` et `toBuilder()`

### `builder()` - Construction from scratch

**Définition** : Méthode statique qui crée un nouveau builder vide.

**Usage** : Création d'une nouvelle instance en spécifiant tous les champs.

```java
// Création d'une nouvelle personne
Person person = Person.builder()
    .id(UUID.randomUUID())
    .name(ClientName.of("John Doe"))
    .email(Email.of("john@example.com"))
    .phone(PhoneNumber.of("+33123456789"))
    .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
    .build();
```

**Caractéristiques** :
- ✅ Tous les champs à `null` au départ
- ✅ On spécifie explicitement chaque champ
- ✅ Utilisé pour créer une nouvelle instance

---

### `toBuilder()` - Copie avec modifications

**Définition** : Méthode d'instance qui crée un builder pré-rempli avec les valeurs actuelles.

**Usage** : Modification d'une instance existante en ne changeant que quelques champs.

```java
// Modification d'une personne existante
Person original = repository.findById(id);  // John Doe, john@example.com, ...

Person updated = original.toBuilder()
    .email(Email.of("john.doe@newcompany.com"))  // Seul champ modifié
    .build();

// Résultat :
// - name: "John Doe" (copié depuis original)
// - email: "john.doe@newcompany.com" (modifié)
// - phone: "+33123456789" (copié depuis original)
// - birthDate: 1990-01-01 (copié depuis original)
```

**Caractéristiques** :
- ✅ Tous les champs pré-remplis avec les valeurs de l'instance actuelle
- ✅ On ne spécifie QUE les champs à modifier
- ✅ Les autres champs sont automatiquement copiés
- ✅ Pattern immutable (nouvelle instance créée)

---

## Comparaison concrète

### Scénario : Changer l'email d'une personne

#### Avec `builder()` ❌ Verbose

```java
Person original = getFromDB();

Person updated = Person.builder()
    .id(original.getId())              // ⚠️ Recopié manuellement
    .name(original.getName())          // ⚠️ Recopié manuellement
    .email(Email.of("new@example.com"))  // ✅ Modifié
    .phone(original.getPhone())        // ⚠️ Recopié manuellement
    .birthDate(original.getBirthDate())  // ⚠️ Recopié manuellement
    .build();
```

**Problèmes** :
- ❌ Beaucoup de code répétitif
- ❌ Risque d'oublier un champ
- ❌ Difficile à maintenir si on ajoute des champs

#### Avec `toBuilder()` ✅ Concis

```java
Person original = getFromDB();

Person updated = original.toBuilder()
    .email(Email.of("new@example.com"))  // ✅ Seul champ modifié
    .build();
```

**Avantages** :
- ✅ Code minimal (seul le changement est visible)
- ✅ Impossible d'oublier un champ (copie automatique)
- ✅ Facile à maintenir (nouveaux champs copiés automatiquement)

---

## Cas d'usage dans le projet

### 1. Création (`Person.of()`)

```java
// Domain Service
public Person createPerson(ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
    return Person.of(name, email, phone, birthDate);
}

// Implémentation de Person.of()
public static Person of(...) {
    return builder()        // ✅ builder() car création from scratch
        .name(name)
        .email(email)
        .phone(phone)
        .birthDate(birthDate)
        .build();
}
```

### 2. Reconstruction depuis DB (`Person.reconstitute()`)

```java
// Infrastructure Assembler
public Person toDomain(PersonJpaEntity entity) {
    return Person.reconstitute(
        entity.getId(),
        ClientName.of(entity.getName()),
        ...
    );
}

// Implémentation de Person.reconstitute()
public static Person reconstitute(UUID id, ...) {
    return builder()        // ✅ builder() car création from scratch
        .id(id)             // Mais avec ID existant
        .name(name)
        .email(email)
        .phone(phone)
        .birthDate(birthDate)
        .build();
}
```

### 3. Modification complète (`withCommonFields()`)

```java
// Application Service - PUT
public Client updateCommonFields(UUID id, ClientName name, Email email, PhoneNumber phone) {
    Client client = getClientById(id);
    Client updated = switch (client) {
        case Person p -> p.withCommonFields(name, email, phone);
        // ...
    };
    return repository.save(updated);
}

// Implémentation de withCommonFields()
public Person withCommonFields(ClientName name, Email email, PhoneNumber phone) {
    return this.toBuilder()  // ✅ toBuilder() car modification d'instance existante
        .name(name)
        .email(email)
        .phone(phone)
        .build();
    // birthDate et id copiés automatiquement
}
```

### 4. Modification partielle (PATCH)

```java
// Application Service - PATCH
public Client patchClient(UUID id, ClientName name, Email email, PhoneNumber phone) {
    Client client = getClientById(id);
    
    Client updated = switch (client) {
        case Person p -> {
            var builder = p.toBuilder();  // ✅ toBuilder() pour copier tous les champs
            if (name != null) builder.name(name);
            if (email != null) builder.email(email);
            if (phone != null) builder.phone(phone);
            yield builder.build();
            // birthDate et id copiés automatiquement
        }
        // ...
    };
    
    return repository.save(updated);
}
```

---

## Configuration Lombok

### Activation de `toBuilder()`

```java
@Builder(toBuilder = true)  // ← Active toBuilder()
private Person(UUID id, ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
    super(id, name, email, phone);
    this.birthDate = birthDate;
    checkInvariants();
}
```

**Sans `toBuilder = true`** :
- ✅ `builder()` disponible
- ❌ `toBuilder()` PAS disponible

**Avec `toBuilder = true`** :
- ✅ `builder()` disponible
- ✅ `toBuilder()` disponible

---

## Performance

### Impact mémoire

```java
// Scénario : Modifier 3 champs sur 5
Person original = getFromDB();

// Option 1 : 3 instances intermédiaires (❌ moins optimal)
Person step1 = original.toBuilder().name(newName).build();      // Instance 1
Person step2 = step1.toBuilder().email(newEmail).build();       // Instance 2
Person step3 = step2.toBuilder().phone(newPhone).build();       // Instance 3

// Option 2 : 1 seule instance finale (✅ optimal)
var builder = original.toBuilder();
builder.name(newName);
builder.email(newEmail);
builder.phone(newPhone);
Person result = builder.build();  // 1 seule instance
```

**Notre choix** : Option 2 dans `patchClient()` pour optimiser les modifications multiples.

---

## Exemples de tests

### Test de création

```java
@Test
void shouldCreatePersonWithAllFields() {
    // Utilise builder() directement (pas toBuilder())
    Person person = Person.builder()
        .name(ClientName.of("Test User"))
        .email(Email.of("test@example.com"))
        .phone(PhoneNumber.of("+33123456789"))
        .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
        .build();

    assertThat(person.getName().value()).isEqualTo("Test User");
}
```

### Test de modification

```java
@Test
void shouldCreateNewInstanceWithUpdatedFields() {
    Person original = Person.builder()
        .id(UUID.randomUUID())
        .name(ClientName.of("Original Name"))
        .email(Email.of("original@example.com"))
        .phone(PhoneNumber.of("+33111111111"))
        .birthDate(PersonBirthDate.of(LocalDate.of(1990, 1, 1)))
        .build();

    // Utilise toBuilder() pour modification
    Person updated = original.toBuilder()
        .name(ClientName.of("Updated Name"))
        .build();

    // Vérifie la modification
    assertThat(updated.getName().value()).isEqualTo("Updated Name");
    
    // Vérifie que l'original n'a pas changé (immutabilité)
    assertThat(original.getName().value()).isEqualTo("Original Name");
    
    // Vérifie que les autres champs sont copiés
    assertThat(updated.getEmail()).isEqualTo(original.getEmail());
    assertThat(updated.getPhone()).isEqualTo(original.getPhone());
    assertThat(updated.getBirthDate()).isEqualTo(original.getBirthDate());
    assertThat(updated.getId()).isEqualTo(original.getId());
}
```

---

## Tableau récapitulatif

| Aspect | `builder()` | `toBuilder()` |
|--------|-------------|---------------|
| **Type** | Méthode statique | Méthode d'instance |
| **État initial** | Tous champs à `null` | Tous champs = valeurs actuelles |
| **Usage** | Création nouvelle instance | Modification instance existante |
| **Code requis** | Spécifier tous les champs | Spécifier seulement les modifications |
| **Avantage** | Contrôle total | Concision, sécurité |
| **Exemple** | `Person.builder().name(...).build()` | `person.toBuilder().name(...).build()` |

---

## Conclusion

**`builder()`** = Création from scratch  
**`toBuilder()`** = Copie + modifications

**Pattern immutable** : Les deux créent de nouvelles instances, garantissant l'immutabilité.

**Recommandation** :
- ✅ Utilisez `builder()` pour créer une nouvelle instance
- ✅ Utilisez `toBuilder()` pour modifier une instance existante
- ✅ Activez toujours `@Builder(toBuilder = true)` sur les entités immutables

**Date** : 2025-01-16

