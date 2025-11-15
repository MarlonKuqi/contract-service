# ✅ Factory Methods Implémentées !

## 🎯 Ce qui a été fait

### 1. **Person** avec Factory Methods
```java
// Création (nouvelle entité)
Person person = Person.create(name, email, phone, birthDate);  // ✅ Pas de null !

// Reconstruction (depuis BDD)
Person person = Person.reconstitute(id, name, email, phone, birthDate);  // ✅ Intent clair !
```

### 2. **Company** avec Factory Methods
```java
// Création
Company company = Company.create(name, email, phone, companyId);

// Reconstruction
Company company = Company.reconstitute(id, name, email, phone, companyId);
```

### 3. **Contract** avec Factory Methods
```java
// Création
Contract contract = Contract.create(client, period, cost);

// Reconstruction
Contract contract = Contract.reconstitute(id, client, period, cost);
```

### 4. **Services** utilisent `create()`
```java
@Transactional
public Person createPerson(...) {
    Person person = Person.create(  // ✅ Expressif !
        ClientName.of(name),
        Email.of(email),
        PhoneNumber.of(phone),
        PersonBirthDate.of(birthDate)
    );
    return clientRepo.save(person);
}
```

### 5. **Mappers** utilisent `reconstitute()`
```java
public Client toDomain(ClientJpaEntity entity) {
    return switch (entity) {
        case PersonJpaEntity p -> Person.reconstitute(  // ✅ Intent clair !
            p.getId(),
            ClientName.of(p.getName()),
            ...
        );
    };
}
```

### 6. **Tests** utilisent `create()`
```java
// Tests domaine
Person person = Person.create(name, email, phone, birthDate);  // ✅ Propre !
```

---

## 💎 Avantages Obtenus

1. ✅ **Code métier expressif** : `Person.create()` est clair
2. ✅ **Pas de `null` dans le code métier** : Le service n'a jamais à passer `null`
3. ✅ **Reconstruction explicite** : `reconstitute()` dit exactement ce qu'il fait
4. ✅ **Constructeur privé** : Contrôle total sur la création
5. ✅ **DDD best practice** : Pattern utilisé partout dans la littérature DDD

---

## ⚠️ Tests d'Intégration Restants

Il reste ~30 tests d'intégration à mettre à jour manuellement ou via Find & Replace :

```
new Person(...) → Person.create(...)
new Company(...) → Company.create(...)
new Contract(...) → Contract.create(...)
```

---

# 📚 Réponse : Nombreux Champs (Cas Assurance)

## 🤔 Question
> "Si on a énormément de champs un jour, que faudra-t-il mettre en place ? Prends le cas où on rattache des documents à une person ou toute autre donnée importante pour une assurance"

## 💡 Solutions selon le Cas

### Cas 1 : Beaucoup de Value Objects Simples (ex: 10-15 champs)

**Problème** :
```java
Person.create(
    name, email, phone, birthDate,
    address, nationality, profession,
    maritalStatus, numberOfChildren,
    annualIncome, healthCondition, etc...  // 😱 15 paramètres !
)
```

**Solution : Builder Pattern** ✨

```java
public final class Person extends Client {
    private final PersonBirthDate birthDate;
    private final Address address;
    private final Nationality nationality;
    private final Profession profession;
    // ... autres champs
    
    private Person(PersonBuilder builder) {
        super(builder.id, builder.name, builder.email, builder.phone);
        this.birthDate = builder.birthDate;
        this.address = builder.address;
        this.nationality = builder.nationality;
        // ... validations et affectations
    }
    
    // Factory methods
    public static PersonBuilder builder() {
        return new PersonBuilder();
    }
    
    public static Person create(PersonBuilder builder) {
        builder.validate();  // Validation des champs obligatoires
        return new Person(builder);
    }
    
    public static Person reconstitute(UUID id, PersonBuilder builder) {
        if (id == null) throw new IllegalArgumentException("ID required");
        builder.id = id;
        return new Person(builder);
    }
    
    // Builder interne
    public static class PersonBuilder {
        private UUID id;
        private ClientName name;
        private Email email;
        private PhoneNumber phone;
        private PersonBirthDate birthDate;
        private Address address;
        private Nationality nationality;
        
        public PersonBuilder name(ClientName name) {
            this.name = name;
            return this;
        }
        
        public PersonBuilder email(Email email) {
            this.email = email;
            return this;
        }
        
        // ... autres setters fluent
        
        void validate() {
            if (name == null) throw new IllegalArgumentException("Name required");
            if (email == null) throw new IllegalArgumentException("Email required");
            // ... autres validations
        }
    }
}
```

**Usage** :
```java
// Service
Person person = Person.create(
    Person.builder()
        .name(ClientName.of("John"))
        .email(Email.of("john@example.com"))
        .phone(PhoneNumber.of("+33..."))
        .birthDate(PersonBirthDate.of(...))
        .address(Address.of(...))
        .nationality(Nationality.FRENCH)
        .profession(Profession.of("Engineer"))
);

// Mapper
Person person = Person.reconstitute(
    entity.getId(),
    Person.builder()
        .name(ClientName.of(entity.getName()))
        .email(Email.of(entity.getEmail()))
        // ...
);
```

---

### Cas 2 : Documents Attachés (Collections)

**Problème** : Les documents ne font PAS partie de l'aggregate Person !

```java
// ❌ MAUVAIS : Documents dans Person
public final class Person {
    private List<InsuranceDocument> documents;  // ❌ Trop couplé !
}
```

**Solution : Aggregate Séparé** ✨

```java
// Person Aggregate (simple)
public final class Person {
    private UUID id;
    private ClientName name;
    private Email email;
    // ... champs de base
}

// Document Aggregate (séparé)
public final class InsuranceDocument {
    private UUID id;
    private UUID personId;  // ← Référence à Person (pas l'objet complet !)
    private DocumentType type;
    private DocumentContent content;
    private DocumentMetadata metadata;
    
    public static InsuranceDocument create(UUID personId, DocumentType type, DocumentContent content) {
        return new InsuranceDocument(null, personId, type, content);
    }
}

// Repository séparé
public interface InsuranceDocumentRepository {
    List<InsuranceDocument> findByPersonId(UUID personId);
    InsuranceDocument save(InsuranceDocument document);
}
```

**Pourquoi ?**
- ✅ **Scalabilité** : Une personne peut avoir des centaines de documents
- ✅ **Performance** : On ne charge pas tous les documents quand on veut juste le nom
- ✅ **Transactions** : Modifier un document ne modifie pas Person
- ✅ **Bounded Context** : Documents peut être un sous-domaine séparé

---

### Cas 3 : Données Complexes Imbriquées

**Problème** : Hiérarchie complexe (ex: assurance santé)

```java
// ❌ MAUVAIS : Tout dans un seul aggregate
public final class Person {
    private HealthInsurance healthInsurance;  // Gros objet
    private CarInsurance carInsurance;        // Gros objet
    private HomeInsurance homeInsurance;      // Gros objet
    // ... = Trop lourd !
}
```

**Solution : Aggregates Séparés + Event-Driven** ✨

```java
// Person Aggregate (identité de base)
public final class Person {
    private UUID id;
    private ClientName name;
    private Email email;
}

// HealthInsurance Aggregate (séparé)
public final class HealthInsurance {
    private UUID id;
    private UUID personId;  // Référence
    private HealthCoverage coverage;
    private List<Beneficiary> beneficiaries;
    
    public static HealthInsurance create(UUID personId, HealthCoverage coverage) {
        // Validation
        return new HealthInsurance(null, personId, coverage);
    }
}

// Event pour synchronisation
public record PersonCreatedEvent(UUID personId, String name, String email) {}

// Listener pour créer l'assurance par défaut
@EventListener
public void onPersonCreated(PersonCreatedEvent event) {
    HealthInsurance insurance = HealthInsurance.createDefault(event.personId());
    healthInsuranceRepo.save(insurance);
}
```

---

### Cas 4 : Value Object Composite

**Problème** : Plusieurs champs liés

```java
// ❌ MAUVAIS : Trop de paramètres
Person.create(
    name, email, phone,
    street, city, zipCode, country,  // Adresse = 4 champs !
    birthDate, nationality
)
```

**Solution : Value Object Composite** ✨

```java
// Value Object Address
public final class Address {
    private final String street;
    private final String city;
    private final String zipCode;
    private final Country country;
    
    public static Address of(String street, String city, String zipCode, Country country) {
        validate(street, city, zipCode, country);
        return new Address(street, city, zipCode, country);
    }
}

// Person simplifié
Person.create(
    name, email, phone,
    Address.of(street, city, zipCode, country),  // ✅ Regroupé !
    birthDate, nationality
)
```

---

## 📊 Tableau Récapitulatif

| Cas | Solution | Quand l'utiliser |
|-----|----------|------------------|
| **10-15 champs simples** | Builder Pattern | Beaucoup de paramètres optionnels |
| **Collections (documents)** | Aggregate Séparé | Données volumineuses, indépendantes |
| **Hiérarchie complexe** | Aggregates + Events | Sous-domaines distincts |
| **Champs liés** | Value Object Composite | Groupe cohérent de champs |

---

## 🎯 Recommandations

### Pour une Assurance (ex: Person avec documents)

```java
// Person Aggregate (léger)
public final class Person {
    private UUID id;
    private ClientName name;
    private Email email;
    private PhoneNumber phone;
    private PersonBirthDate birthDate;
    private Address address;  // Value Object composite
    
    public static Person create(
        ClientName name,
        Email email,
        PhoneNumber phone,
        PersonBirthDate birthDate,
        Address address
    ) {
        return new Person(null, name, email, phone, birthDate, address);
    }
}

// InsuranceDocument Aggregate (séparé)
public final class InsuranceDocument {
    private UUID id;
    private UUID personId;  // Référence
    private DocumentType type;
    private byte[] content;
    
    public static InsuranceDocument attach(UUID personId, DocumentType type, byte[] content) {
        return new InsuranceDocument(null, personId, type, content);
    }
}

// InsurancePolicy Aggregate (séparé)
public final class InsurancePolicy {
    private UUID id;
    private UUID personId;  // Référence
    private PolicyType type;
    private Coverage coverage;
    private Money premium;
    
    public static InsurancePolicy create(UUID personId, PolicyType type, Coverage coverage) {
        return new InsurancePolicy(null, personId, type, coverage);
    }
}
```

### Service Orchestrateur

```java
@Service
public class InsuranceApplicationService {
    
    @Transactional
    public PersonWithPolicies createInsuredPerson(
        String name, 
        String email, 
        // ... autres champs
        List<PolicyCreationRequest> policies
    ) {
        // 1. Créer Person
        Person person = Person.create(
            ClientName.of(name),
            Email.of(email),
            // ...
        );
        person = personRepo.save(person);
        
        // 2. Créer Policies
        List<InsurancePolicy> createdPolicies = policies.stream()
            .map(req -> InsurancePolicy.create(person.getId(), req.type(), req.coverage()))
            .map(policyRepo::save)
            .toList();
        
        return new PersonWithPolicies(person, createdPolicies);
    }
}
```

---

**En résumé** : Pour beaucoup de champs, utilise le Builder Pattern. Pour les documents/collections, sépare en Aggregates distincts ! 🎯

