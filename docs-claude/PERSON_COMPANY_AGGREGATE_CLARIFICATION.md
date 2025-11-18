# Clarification : Person et Company dans notre Architecture

## ❌ MAUVAISE Compréhension

> "Person et Company sont des entities internes (aggregate mais pas root) car ils ont chacun un attribut (companyIdentifier et birthDate)"

**Non !** Ce n'est PAS correct.

---

## ✅ BONNE Compréhension

### Person et Company sont des **Aggregate Roots**, PAS des entities internes !

```
┌────────────────────────────────────┐
│  AGGREGATE: Person                 │  ← Aggregate Root indépendant
│  Root: Person                      │
│  ├─ UUID id                        │  ← Identité
│  ├─ ClientName name                │  ← Value Object
│  ├─ Email email                    │  ← Value Object
│  ├─ PhoneNumber phone              │  ← Value Object
│  └─ PersonBirthDate birthDate      │  ← Value Object (spécifique à Person)
└────────────────────────────────────┘

┌────────────────────────────────────┐
│  AGGREGATE: Company                │  ← Aggregate Root indépendant
│  Root: Company                     │
│  ├─ UUID id                        │  ← Identité
│  ├─ ClientName name                │  ← Value Object
│  ├─ Email email                    │  ← Value Object
│  ├─ PhoneNumber phone              │  ← Value Object
│  └─ CompanyIdentifier identifier   │  ← Value Object (spécifique à Company)
└────────────────────────────────────┘
```

---

## 🎯 Pourquoi Person et Company sont des Aggregate Roots ?

### 1. Ils ont leur propre Repository
```java
// ✅ Un repository pour gérer Person ET Company
public interface ClientRepository {
    Client save(Client client);  // Sauvegarde Person OU Company
    Optional<Client> findById(UUID id);
}

// On sauvegarde :
Person person = new Person(...);
clientRepo.save(person);  // ← Person est sauvegardé SEUL

Company company = new Company(...);
clientRepo.save(company);  // ← Company est sauvegardé SEUL
```

### 2. Ils ont une identité propre (UUID)
```java
public final class Person extends Client {
    // Hérite de Client qui a un UUID id
}

Person person1 = new Person(...);
person1.setId(UUID.randomUUID());  // ← Identité propre !

Person person2 = new Person(...);
person2.setId(UUID.randomUUID());  // ← Autre identité !
```

### 3. Ils peuvent exister INDÉPENDAMMENT
```java
// ✅ Person existe SANS Contract
Person person = new Person(...);
clientRepo.save(person);

// ✅ Plus tard, on crée un Contract qui RÉFÉRENCE cette Person
Contract contract = new Contract(person, period, cost);
contractRepo.save(contract);
```

### 4. Ils sont sauvegardés dans leur propre transaction
```java
@Transactional
public Person createPerson(...) {
    Person person = new Person(...);
    return clientRepo.save(person);  // ← Transaction pour Person seul
}

@Transactional
public Contract createContract(UUID personId, ...) {
    Person person = clientRepo.findById(personId).orElseThrow();
    Contract contract = new Contract(person, ...);
    return contractRepo.save(contract);  // ← Transaction pour Contract seul
}
```

---

## 🔍 Entities Internes vs Aggregate Roots

### Entity Interne (exemple théorique)

Imagine si on avait ça (on ne l'a PAS dans notre code) :

```java
// ❌ Exemple théorique (pas dans notre code)
public class Person {
    private UUID id;
    private ClientName name;
    
    // Entity INTERNE (pas dans notre code actuel)
    private Address address;  // ← Entity interne à Person
}

public class Address {  // Entity INTERNE
    private String street;
    private String city;
    private String zipCode;
    
    // ❌ PAS de UUID id (pas d'identité globale)
    // ❌ PAS de AddressRepository
    // ✅ Sauvegardé AVEC Person
    // ✅ N'existe PAS sans Person
}
```

Dans ce cas :
- `Person` = Aggregate Root
- `Address` = Entity interne
- Pas de `AddressRepository`
- `Address` sauvegardé automatiquement avec `Person`

**Mais dans NOTRE code, on n'a PAS ça !**

---

## 🎯 Dans NOTRE Code

### Aggregates Séparés

```
┌─────────────────────────────┐     ┌─────────────────────────────┐
│  AGGREGATE: Person          │     │  AGGREGATE: Company         │
│  Root: Person               │     │  Root: Company              │
│  - Value Objects internes   │     │  - Value Objects internes   │
└─────────────────────────────┘     └─────────────────────────────┘
         ▲                                     ▲
         │                                     │
         └─────────────┬───────────────────────┘
                       │ RÉFÉRENCÉ PAR
         ┌─────────────▼─────────────┐
         │  AGGREGATE: Contract      │
         │  Root: Contract           │
         │  - Client client          │ ← Référence à Person ou Company
         │  - ContractPeriod period  │ ← Value Object
         │  - ContractCost cost      │ ← Value Object
         └───────────────────────────┘
```

**Relations** :
- `Contract` RÉFÉRENCE `Client` (Person ou Company)
- Mais `Person` et `Company` sont des Aggregates **INDÉPENDANTS**
- Pas des entities internes de `Contract`

---

## 📊 Comparaison : Entity Interne vs Aggregate Root

| Critère | Entity Interne | Person/Company (Aggregate Root) |
|---------|----------------|----------------------------------|
| **Identité globale** | Non | Oui (UUID) |
| **Repository** | Non | Oui (ClientRepository) |
| **Existe seul** | Non | Oui |
| **Sauvegarde** | Avec son aggregate | Indépendamment |
| **Référencé par ID** | Non | Oui |
| **Exemple** | Ligne de commande | Client, Contract |

---

## 🎓 Règle Simple

### Comment savoir si c'est un Aggregate Root ?

**Pose-toi ces questions** :

1. **A-t-il un UUID id ?**
   - Person : ✅ Oui → Aggregate Root
   - Email : ❌ Non → Value Object

2. **A-t-il un Repository ?**
   - Person : ✅ Oui (ClientRepository) → Aggregate Root
   - PersonBirthDate : ❌ Non → Value Object

3. **Peut-il exister SEUL, sans autre objet ?**
   - Person : ✅ Oui → Aggregate Root
   - ContractPeriod : ❌ Non (n'existe que dans Contract) → Value Object

4. **Est-il référencé par ID par d'autres objets ?**
   - Person : ✅ Oui (Contract référence Person) → Aggregate Root
   - Email : ❌ Non → Value Object

---

## ✅ Résumé pour NOTRE Code

### Aggregate Roots (3)
1. **Person** (avec name, email, phone, birthDate)
2. **Company** (avec name, email, phone, companyIdentifier)
3. **Contract** (avec client, period, cost)

### Entities Internes
- **Aucune** dans notre code actuel !

### Value Objects (7)
1. Email
2. PhoneNumber
3. ClientName
4. PersonBirthDate
5. CompanyIdentifier
6. ContractPeriod
7. ContractCost

---

## 🎯 Pourquoi c'est Important ?

```java
// ❌ Si Person était une entity interne de Contract :
public class Contract {
    private Person person;  // Entity interne
}

// Conséquences :
// - Person n'aurait PAS d'UUID id
// - Person n'existerait PAS sans Contract
// - Pas de ClientRepository
// - On ne pourrait PAS faire : personRepo.save(person)

// ✅ Dans notre code actuel (Person = Aggregate Root) :
Person person = new Person(...);
clientRepo.save(person);  // ← Sauvegarde indépendante

Contract contract = new Contract(person, ...);
contractRepo.save(contract);  // ← Référence Person par son ID
```

---

**Conclusion : Person et Company sont des Aggregate Roots INDÉPENDANTS, pas des entities internes !** 🎯

