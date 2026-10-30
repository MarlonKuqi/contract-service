# Règles de Conception des Entités Immuables

## Principes d'Immutabilité

Lors de la conception des entités de domaine (aggregates), respectez les principes suivants pour garantir l'immutabilité et l'intégrité des données :

### 1. Immutabilité Stricte

- **Tous les champs** DOIVENT être `private final`, sauf si on utilise `@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)` de Lombok
- L'entité NE DOIT PAS avoir de méthodes setter publiques
- Toute méthode qui modifie conceptuellement l'état de l'entité DOIT retourner une **nouvelle instance** avec l'état mis à jour

### 2. Constructeur Validant avec Builder

- L'entité DOIT avoir un constructeur **privé** unique
- Utiliser `@Builder(toBuilder = true)` de Lombok sur le constructeur pour une classe qui n'hérite pas, ou implementer manuellement le pattern Builder si héritage
- Le constructeur DOIT **valider** toutes les propriétés essentielles pour garantir que seules des instances valides sont créées
- Lancer des exceptions métier spécifiques (ex: `InvalidClientException`) en cas de validation échouée

### 3. Méthodes Factory Statiques

En plus du pattern Builder, fournir des méthodes factory pour clarifier l'intention :

- **`of(...)`** : pour créer une nouvelle entité sans ID (avant persistance)
- **`reconstitute(...)`** : pour recréer une entité avec son ID (depuis la base de données)

### 4. Méthode Helper pour Copie

- Utiliser `toBuilder()` généré par Lombok pour créer un builder pré-rempli avec les valeurs actuelles
- Les méthodes `withXxx()` DOIVENT utiliser `toBuilder()` pour retourner une copie modifiée
- Chaque méthode `withXxx()` modifie un seul champ et retourne une nouvelle instance

## Exemple : Client Aggregate

```java
@Getter
@EqualsAndHashCode
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class Person extends Client {
    
    PersonBirthDate birthDate;
    
    // Constructeur privé avec validation
    @Builder(toBuilder = true)
    private Person(
            @Nullable UUID id,
            @Nullable ClientName name,
            @Nullable ClientEmail email,
            @Nullable ClientPhoneNumber phone,
            @Nullable PersonBirthDate birthDate
    ) {
        super(id, name, email, phone);
        if (birthDate == null) {
            throw InvalidClientException.forNullBirthDate();
        }
        this.birthDate = birthDate;
    }
    
    // Factory method pour nouvelle entité
    public static Person of(
            ClientName name,
            ClientEmail email,
            ClientPhoneNumber phone,
            PersonBirthDate birthDate
    ) {
        return builder()
                .name(name)
                .email(email)
                .phone(phone)
                .birthDate(birthDate)
                .build();
    }
    
    // Factory method pour reconstitution depuis DB
    public static Person reconstitute(
            UUID id,
            ClientName name,
            ClientEmail email,
            ClientPhoneNumber phone,
            PersonBirthDate birthDate
    ) {
        return builder()
                .id(id)
                .name(name)
                .email(email)
                .phone(phone)
                .birthDate(birthDate)
                .build();
    }
    
    // Méthodes withXxx retournant une nouvelle instance
    public Person withName(ClientName name) {
        return toBuilder().name(name).build();
    }
    
    public Person withEmail(ClientEmail email) {
        return toBuilder().email(email).build();
    }
    
    public Person withPhone(ClientPhoneNumber phone) {
        return toBuilder().phone(phone).build();
    }
    
    // Méthode pour mise à jour de plusieurs champs communs
    public Person withCommonFields(
            ClientName name,
            ClientEmail email,
            ClientPhoneNumber phone
    ) {
        return toBuilder()
                .name(name)
                .email(email)
                .phone(phone)
                .build();
    }
}
```

## Exemple : Contract Aggregate

```java
@Getter
@EqualsAndHashCode
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class Contract {
    
    @Nullable UUID id;
    UUID clientId;
    ContractPeriod period;
    ContractCost costAmount;
    
    @Builder(toBuilder = true)
    private Contract(
            @Nullable UUID id,
            @Nullable UUID clientId,
            @Nullable ContractPeriod period,
            @Nullable ContractCost costAmount
    ) {
        // Validation dans le constructeur
        if (clientId == null) {
            throw InvalidContractException.forNullClient();
        }
        if (period == null) {
            throw InvalidContractException.forNullPeriod();
        }
        if (costAmount == null) {
            throw InvalidContractException.forNullCostAmount();
        }
        this.id = id;
        this.clientId = clientId;
        this.period = period;
        this.costAmount = costAmount;
    }
    
    public static Contract of(
            UUID clientId,
            ContractPeriod period,
            ContractCost costAmount
    ) {
        return builder()
                .clientId(clientId)
                .period(period)
                .costAmount(costAmount)
                .build();
    }
    
    public static Contract reconstitute(
            UUID id,
            UUID clientId,
            ContractPeriod period,
            ContractCost costAmount
    ) {
        return builder()
                .id(id)
                .clientId(clientId)
                .period(period)
                .costAmount(costAmount)
                .build();
    }
    
    // Logique métier retournant nouvelle instance
    public Contract changeCost(ContractCost newAmount) {
        if (newAmount == null) {
            throw InvalidContractException.forNullNewCostAmount();
        }
        if (isInactive()) {
            throw new ExpiredContractException(getId());
        }
        return toBuilder()
                .costAmount(newAmount)
                .build();
    }
    
    public boolean isActive() {
        return period.isActive();
    }
}
```

## Justification

Ce pattern a les avantages suivants :

- **Séparation claire** entre état et comportement
- **Réduction des effets de bord** : pas de modification d'état partagé
- **Thread-safety** : les entités immuables sont intrinsèquement thread-safe
- **Testabilité** : plus facile de tester des objets immuables
- **Intégrité des données** : validation au moment de la création
- **Traçabilité** : chaque modification crée un nouvel objet avec un nouvel état

## Notes Spécifiques au Projet

### Pourquoi pas `@Value` ?

Nous n'utilisons pas `@Value` de Lombok car :
1. **Héritage** : Nos aggregates héritent d'une classe parent (`Person` et `Company` héritent de `Client`)
   - `@Value` ne supporte pas bien l'héritage avec appel à `super()`
2. **Contrôle fin sur l'égalité** : Nous avons besoin de `@EqualsAndHashCode(callSuper = true)`
3. **Séparation Domain/Infrastructure** : Nos aggregates de domaine sont **purs** (pas d'annotations JPA)
   - Les entités JPA sont dans `infrastructure.persistence.*` (`PersonJpaEntity`, `CompanyJpaEntity`, etc.)
   - Les aggregates de domaine (`Person`, `Company`) sont mappés vers/depuis les entités JPA par des mappers

### Pattern utilisé à la place

```java
@Getter
@EqualsAndHashCode(callSuper = true) // ou sans callSuper si pas d'héritage
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class MyAggregate {
    // fields...
    
    private MyAggregate(...) {
        // validation
    }
}
```

### Value Objects

Pour les Value Objects (records Java), la règle est encore plus stricte :

```java
public record ClientEmail(String value) {
    public ClientEmail {
        if (value == null || value.isBlank()) {
            throw new InvalidEmailException("Email cannot be null or blank");
        }
        if (!value.matches(EMAIL_PATTERN)) {
            throw new InvalidEmailException("Invalid email format: " + value);
        }
    }
}
```

Les records sont intrinsèquement immuables et fournissent automatiquement :
- Tous les champs `private final`
- Constructeur canonique
- `equals()`, `hashCode()`, `toString()`
- Getters pour tous les champs

## Séparation Domain / Infrastructure

### Architecture Hexagonale

Notre projet suit une **architecture hexagonale stricte** :

```
domain/
  ├── client/
  │   ├── aggregate/           ← Aggregates PURS (Person, Company, Client)
  │   ├── valueobject/         ← Value Objects (ClientEmail, ClientName, etc.)
  │   └── repository/          ← Interfaces de ports
  └── contract/
      ├── aggregate/           ← Aggregates PURS (Contract)
      └── valueobject/         ← Value Objects (ContractPeriod, ContractCost)

infrastructure/
  └── persistence/
      ├── client/
      │   ├── entity/          ← Entités JPA (PersonJpaEntity, CompanyJpaEntity)
      │   └── mapper/          ← Mappers Domain ↔ JPA
      └── contract/
          ├── entity/          ← Entités JPA (ContractJpaEntity)
          └── mapper/          ← Mappers Domain ↔ JPA
```

### Règles de Séparation

1. **Les aggregates de domaine** (`Person`, `Company`, `Contract`) :
   - ❌ **PAS d'annotations JPA** (`@Entity`, `@Table`, `@Column`, etc.)
   - ✅ Logique métier pure
   - ✅ Immuables avec pattern Builder
   - ✅ Validation dans le constructeur

2. **Les entités JPA** (`PersonJpaEntity`, `CompanyJpaEntity`, `ContractJpaEntity`) :
   - ✅ Annotations JPA
   - ✅ Mutables (setters pour JPA/Hibernate)
   - ❌ Pas de logique métier
   - ✅ Constructeur par défaut pour JPA

3. **Les assemblers** convertissent entre les deux :
   ```java
   // Infrastructure → Domain
   Person person = personAssembler.toDomain(personJpaEntity);
   
   // Domain → Infrastructure  
   PersonJpaEntity jpaEntity = personAssembler.toJpaEntity(person);
   ```

### Exemple de Mapping avec Assembler

**Aggregate de Domaine (PUR)** :
```java
// domain/client/aggregate/Person.java
@Getter
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public final class Person extends Client {
    PersonBirthDate birthDate;
    
    // Constructeur package-private avec validation
    Person(
            UUID id,
            ClientName name,
            ClientEmail email,
            ClientPhoneNumber phone,
            PersonBirthDate birthDate
    ) {
        super(id, name, email, phone);
        if (birthDate == null) {
            throw InvalidClientException.forNullBirthDate();
        }
        this.birthDate = birthDate;
    }
    
    // Méthode factory statique
    public static Person of(...) { ... }
    public static Person reconstitute(...) { ... }
}
```

**Entité JPA (Infrastructure)** :
```java
// infrastructure/persistence/client/entity/PersonJpaEntity.java
@Entity
@Table(name = "person")
@DiscriminatorValue("PERSON")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class PersonJpaEntity extends ClientJpaEntity {
    
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;
    
    public PersonJpaEntity(String name, String email, String phone, LocalDate birthDate) {
        super(name, email, phone);
        this.birthDate = birthDate;
    }
}
```

**Assembler (Infrastructure)** :
```java
// infrastructure/persistence/client/assembler/PersonAssembler.java
@Component
public class PersonAssembler {
    
    // JPA → Domain
    public Person toDomain(PersonJpaEntity entity) {
        return Person.reconstitute(
            entity.getId(),
            ClientName.of(entity.getName()),
            ClientEmail.of(entity.getEmail()),
            ClientPhoneNumber.of(entity.getPhone()),
            PersonBirthDate.of(entity.getBirthDate())
        );
    }
    
    // Domain → JPA (nouvelle entité)
    public PersonJpaEntity toJpaEntity(Person person) {
        return new PersonJpaEntity(
            person.getName().value(),
            person.getEmail().value(),
            person.getPhone().value(),
            person.getBirthDate().value()
        );
    }
}
```

**Utilisation dans le Repository** :
```java
@Component
public class JpaClientRepository implements ClientRepository {
    
    private final ClientJpaRepository jpaRepository;
    private final ClientAssembler assembler;
    
    @Override
    public Optional<Client> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(assembler::toDomain);  // JPA → Domain
    }
    
    @Override
    public Client save(Client client) {
        ClientJpaEntity entity = assembler.toJpaEntity(client);  // Domain → JPA
        ClientJpaEntity saved = jpaRepository.save(entity);
        return assembler.toDomain(saved);  // JPA → Domain
    }
}
```

### Avantages de cette Séparation

1. **Indépendance technologique** : Le domaine n'est pas couplé à JPA
2. **Testabilité** : Les aggregates peuvent être testés sans base de données
3. **Flexibilité** : Changement de persistence (JPA → MongoDB) sans toucher au domaine
4. **Clarté** : Séparation nette entre logique métier et persistence
5. **Évolutivité** : Le modèle de domaine peut évoluer indépendamment du modèle de données

