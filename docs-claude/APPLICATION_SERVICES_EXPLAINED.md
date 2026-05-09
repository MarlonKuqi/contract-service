# Application Services : Orchestrateurs de Use Cases

## 🎯 Définition Simple

> Un **Application Service** est un **orchestrateur** qui coordonne les **use cases** (cas d'utilisation) de l'application.

**Analogie** : C'est le **chef d'orchestre**
- Il ne joue pas d'instrument (pas de logique métier)
- Il coordonne les musiciens (domaine, repositories, etc.)
- Il suit une partition (use case)

---

## 🏗️ Architecture en Couches

```
┌─────────────────────────────────────────┐
│  WEB (Controllers)                       │  ← Reçoit HTTP, retourne JSON
│  - PersonController                      │
│  - ContractController                    │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│  APPLICATION (Services) ⭐               │  ← USE CASES, ORCHESTRATION
│  - ClientApplicationService             │
│  - ContractApplicationService           │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│  DOMAIN (Entities, VOs)                  │  ← LOGIQUE MÉTIER PURE
│  - Client, Person, Company              │
│  - Contract, Email, etc.                │
│  - Repository Interfaces                │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│  INFRASTRUCTURE (Persistence)            │  ← TECHNIQUE
│  - JpaClientRepository                   │
│  - Mappers, JPA Entities                │
└──────────────────────────────────────────┘
```

---

## 📝 Rôle des Application Services

### 1. Orchestration
```java
@Service
public class ClientApplicationService {
    
    @Transactional
    public Person createPerson(String name, String email, String phone, LocalDate birthDate) {
        // 1. Vérification (règle applicative, pas métier)
        if (clientRepo.existsByEmail(email)) {
            throw new ClientAlreadyExistsException("Email already exists", email);
        }
        
        // 2. Création de l'objet domaine (VALIDATION MÉTIER)
        Person person = new Person(
            ClientName.of(name),
            Email.of(email),
            PhoneNumber.of(phone),
            PersonBirthDate.of(birthDate)
        );
        
        // 3. Persistence
        return (Person) clientRepo.save(person);
    }
}
```

**Responsabilités** :
- ✅ Orchestrer les appels (domaine + repositories)
- ✅ Gérer les transactions (`@Transactional`)
- ✅ Vérifications applicatives (email déjà utilisé)
- ✅ Coordonner plusieurs repositories si nécessaire

**PAS ses responsabilités** :
- ❌ Validation métier (c'est le domaine)
- ❌ Logique métier (c'est le domaine)
- ❌ Gestion HTTP (c'est le controller)
- ❌ Persistence technique (c'est l'infrastructure)

---

### 2. Use Cases (Cas d'Utilisation)

Un **Use Case** = Une fonctionnalité de l'application du point de vue utilisateur

```java
@Service
public class ContractApplicationService {
    
    // USE CASE: "Créer un contrat"
    @Transactional
    public Contract createContract(UUID clientId, LocalDateTime start, LocalDateTime end, BigDecimal amount) {
        // 1. Récupérer le client
        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));
        
        // 2. Créer le contrat (validation métier dans le domaine)
        Contract contract = new Contract(
            client,
            ContractPeriod.of(start, end),
            ContractCost.of(amount)
        );
        
        // 3. Sauvegarder
        return contractRepo.save(contract);
    }
    
    // USE CASE: "Changer le coût d'un contrat"
    @Transactional
    public Contract changeContractCost(UUID contractId, BigDecimal newAmount) {
        // 1. Récupérer le contrat
        Contract contract = contractRepo.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException(contractId));
        
        // 2. Appliquer la logique métier (dans le domaine !)
        contract.changeCost(ContractCost.of(newAmount));
        
        // 3. Sauvegarder
        return contractRepo.save(contract);
    }
    
    // USE CASE: "Fermer tous les contrats d'un client"
    @Transactional
    public void closeAllClientContracts(UUID clientId) {
        // Orchestration de plusieurs opérations
        contractRepo.closeAllActiveByClientId(clientId, LocalDateTime.now());
    }
}
```

---

## 🎭 Application Service vs Domain Service

### Domain Service
```java
// Logique métier qui ne rentre pas dans une Entity
public class ContractPricingService {  // Domain Service
    
    public ContractCost calculateDiscountedPrice(Contract contract, Client client) {
        // Logique métier complexe impliquant plusieurs aggregates
        if (client instanceof Company) {
            // 10% de réduction pour les entreprises
            return ContractCost.of(contract.getCostAmount().value().multiply(new BigDecimal("0.9")));
        }
        return contract.getCostAmount();
    }
}
```

**Caractéristiques** :
- ✅ Logique métier PURE
- ✅ Sans état (stateless)
- ✅ Peut être utilisé par les Application Services
- ✅ Dans la couche DOMAINE

### Application Service
```java
@Service
public class ContractApplicationService {  // Application Service
    
    private final ContractRepository contractRepo;
    private final ClientRepository clientRepo;
    private final ContractPricingService pricingService;  // ← Domain Service
    
    @Transactional
    public Contract createDiscountedContract(UUID clientId, ...) {
        // 1. Récupération
        Client client = clientRepo.findById(clientId).orElseThrow();
        
        // 2. Création du contrat
        Contract contract = new Contract(client, period, cost);
        
        // 3. Application du domaine service
        ContractCost discountedCost = pricingService.calculateDiscountedPrice(contract, client);
        contract.changeCost(discountedCost);
        
        // 4. Persistence
        return contractRepo.save(contract);
    }
}
```

**Caractéristiques** :
- ✅ Orchestration
- ✅ Gestion des transactions
- ✅ Utilise les Domain Services
- ✅ Dans la couche APPLICATION

---

## 📊 Comparaison

| Aspect | Application Service | Domain Service | Entity/Aggregate |
|--------|---------------------|----------------|------------------|
| **Couche** | Application | Domain | Domain |
| **Rôle** | Orchestration | Logique métier transverse | Logique métier de l'objet |
| **État** | Stateless | Stateless | Stateful |
| **Transaction** | Oui (@Transactional) | Non | Non |
| **Repositories** | Utilise | N'utilise PAS | N'utilise PAS |
| **Exemple** | createPerson() | calculateDiscount() | changeCost() |

---

## 🎯 Exemple Complet : Flow d'un Use Case

### Use Case : "Créer une Person"

```
┌──────────────────────────────────────────────────┐
│ 1. PersonController.createPerson()               │
│    - Reçoit HTTP POST                            │
│    - Extrait les paramètres du JSON              │
└──────────────┬───────────────────────────────────┘
               │
┌──────────────▼───────────────────────────────────┐
│ 2. ClientApplicationService.createPerson()       │ ← APPLICATION SERVICE
│    ┌──────────────────────────────────────┐     │
│    │ @Transactional                        │     │
│    │ // ORCHESTRATION :                    │     │
│    │                                        │     │
│    │ // a) Vérification applicative        │     │
│    │ if (emailExists) throw ...            │     │
│    │                                        │     │
│    │ // b) Création domaine (VALIDATION)   │     │
│    │ Person person = new Person(...)       │ ────┼─┐
│    │                                        │     │ │
│    │ // c) Persistence                     │     │ │
│    │ return clientRepo.save(person);       │ ────┼─┤
│    └──────────────────────────────────────┘     │ │
└─────────────────────────────────────────────────┘ │
                                                    │ │
┌───────────────────────────────────────────────────┘ │
│ 3. Person (Domaine)                                 │
│    - Validation des Value Objects                   │
│    - Email.of(email) → validation RFC 5321          │
│    - PhoneNumber.of(phone) → validation format      │
│    - PersonBirthDate.of(date) → validation passé    │
└─────────────────────────────────────────────────────┘
                                                      │
┌─────────────────────────────────────────────────────┘
│ 4. ClientRepository (Infrastructure)
│    - Mapper : Person → PersonJpaEntity
│    - JPA : PersonJpaEntity → BDD
│    - Mapper : PersonJpaEntity → Person
└─────────────────────────────────────────────────────┘
```

---

## 💡 Pourquoi Séparer Application Service du Domaine ?

### ❌ MAUVAIS : Tout dans le Domaine
```java
public class Person {
    
    // ❌ L'entité ne doit PAS connaître le repository !
    public void save() {
        PersonRepository repo = ...;  // ❌ Couplage !
        repo.save(this);
    }
    
    // ❌ L'entité ne doit PAS gérer les transactions !
    @Transactional  // ❌ Annotation technique !
    public void createAndSave() {
        // ...
    }
}
```

### ✅ BON : Séparation
```java
// Domaine : PURE logique métier
public class Person {
    public Person(ClientName name, Email email, ...) {
        // Validation métier
    }
}

// Application : Orchestration
@Service
public class ClientApplicationService {
    
    @Transactional  // ✅ Gestion technique ici
    public Person createPerson(...) {
        Person person = new Person(...);  // ✅ Domaine pur
        return clientRepo.save(person);   // ✅ Orchestration
    }
}
```

---

## 🎓 Pattern : Application Service

C'est un pattern DDD classique :

### Caractéristiques
1. **Stateless** : Pas d'état entre les appels
2. **Façade** : Point d'entrée pour les use cases
3. **Coordonne** : Domaine + Infrastructure
4. **Transactionnel** : Gère les transactions
5. **Léger** : Pas de logique métier (délègue au domaine)

### Structure Type
```java
@Service
@Transactional
public class XxxApplicationService {
    
    // Dependencies
    private final XxxRepository xxxRepo;
    private final YyyRepository yyyRepo;
    private final ZzzDomainService zzzService;
    
    // Use Cases
    public Xxx createXxx(...) { }
    public Xxx updateXxx(...) { }
    public void deleteXxx(...) { }
    public Xxx findXxx(...) { }
}
```

---

## 🎯 Dans notre Projet

### ClientApplicationService
```java
@Service
public class ClientApplicationService {
    
    // USE CASES pour les Clients
    - createPerson(...)        // Créer une personne
    - createCompany(...)       // Créer une entreprise
    - updateClient(...)        // Mettre à jour un client
    - deleteClient(...)        // Supprimer un client
    - getClient(...)           // Récupérer un client
}
```

### ContractApplicationService
```java
@Service
public class ContractApplicationService {
    
    // USE CASES pour les Contrats
    - createContract(...)           // Créer un contrat
    - changeContractCost(...)       // Changer le coût
    - closeContract(...)            // Fermer un contrat
    - closeAllClientContracts(...)  // Fermer tous les contrats d'un client
    - getActiveContracts(...)       // Récupérer les contrats actifs
    - calculateTotalCost(...)       // Calculer le coût total
}
```

---

## 📝 Résumé en 3 Points

1. **Application Service** = Orchestrateur de use cases
   - Coordonne domaine + repositories
   - Gère les transactions
   - Pas de logique métier

2. **Domain Service** = Logique métier transverse
   - Logique métier qui ne rentre pas dans une Entity
   - Pur, sans état
   - Utilisé par les Application Services

3. **Entity/Aggregate** = Logique métier de l'objet
   - Logique métier propre à l'objet
   - Garantit les invariants
   - Méthodes métier (changeCost(), etc.)

---

## 🎯 Règle Pratique

**Où mettre la logique ?**

```
┌────────────────────────────────────────────────┐
│ "Changer le coût d'un contrat met à jour       │
│  lastModified"                                  │
│  → ENTITY (Contract.changeCost())               │
└────────────────────────────────────────────────┘

┌────────────────────────────────────────────────┐
│ "Calculer une réduction basée sur le type      │
│  de client et le montant"                       │
│  → DOMAIN SERVICE (PricingService)              │
└────────────────────────────────────────────────┘

┌────────────────────────────────────────────────┐
│ "Créer un contrat : vérifier que le client     │
│  existe, créer le contrat, le sauvegarder"      │
│  → APPLICATION SERVICE                          │
│     (ContractApplicationService.createContract) │
└────────────────────────────────────────────────┘
```

**Les Application Services sont la COLLE entre les couches !** 🎯

