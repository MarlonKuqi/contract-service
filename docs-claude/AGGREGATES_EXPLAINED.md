# Aggregate en DDD : Explication Complète

## 🎯 Définition Simple

> Un **Aggregate** est un **groupe d'objets** (Entity + Value Objects) qui forment une **unité cohérente** et sont **toujours cohérents ensemble**.

**Analogie** : Comme une commande Amazon
- La **Commande** est l'Aggregate Root
- Les **Lignes de commande**, **Adresse de livraison**, **Date de livraison** font partie de l'aggregate
- Si tu supprimes la commande, ces éléments sont supprimés aussi (ils n'existent pas sans la commande)
- Le **Transporteur** (FedEx, UPS) existe INDÉPENDAMMENT → C'est un AUTRE Aggregate
- Le **Produit** existe INDÉPENDAMMENT → C'est un AUTRE Aggregate
- La commande référence le transporteur et le produit, mais ne les contient pas

---

## 🏛️ Les 3 Rôles Clés

### 1. Aggregate Root (Racine)
- **C'est la porte d'entrée** de l'aggregate
- **Seule classe avec un repository**
- **Garantit la cohérence** de tout l'aggregate

### 2. Entities Internes
- Font partie de l'aggregate
- Accédées UNIQUEMENT via la racine
- Pas de repository propre

### 3. Value Objects Internes
- Font partie de l'aggregate
- Immuables, sans identité

---

## 📦 Exemple 1 : Contract (notre code)

### Structure de l'Aggregate

```java
// ┌─────────────────────────────────────┐
// │  AGGREGATE CONTRACT                 │
// │                                     │
// │  ┌─────────────────────────┐       │
// │  │ Contract                │       │ ← Aggregate Root
// │  │ (Aggregate Root)        │       │
// │  ├─────────────────────────┤       │
// │  │ - UUID id               │       │ ← Identité
// │  │ - Client client         │       │ ← Référence à autre Aggregate
// │  │ - ContractPeriod period │       │ ← Value Object interne
// │  │ - ContractCost cost     │       │ ← Value Object interne
// │  │ - LocalDateTime modified│       │
// │  └─────────────────────────┘       │
// │                                     │
// └─────────────────────────────────────┘
```

### Code

```java
public class Contract {  // ← AGGREGATE ROOT
    
    // Identité
    @Setter
    private UUID id;
    
    // Référence à un AUTRE Aggregate (par l'objet complet, mais pourrait être juste l'ID)
    private final Client client;
    
    // Value Objects INTERNES à l'aggregate
    private final ContractPeriod period;
    private ContractCost costAmount;  // Mutable car on peut changer le coût
    
    private LocalDateTime lastModified;
    
    // ✅ Logique métier PROTÉGÉE
    public void changeCost(ContractCost newAmount) {
        if (newAmount == null) {
            throw InvalidContractException.forNullNewCostAmount();
        }
        
        // Changement du coût + mise à jour de lastModified
        // = Cohérence GARANTIE
        this.costAmount = newAmount;
        touch();
    }
    
    private void touch() {
        this.lastModified = LocalDateTime.now();
    }
    
    // ❌ PAS de setCostAmount() public !
    // Le changement se fait UNIQUEMENT via changeCost()
    // qui garantit la cohérence (lastModified mis à jour)
}
```

### Pourquoi Contract est un Aggregate ?

1. ✅ **Cohérence transactionnelle**
   - Si je change le `costAmount`, je DOIS mettre à jour `lastModified`
   - L'aggregate garantit ça via `changeCost()`

2. ✅ **Frontière de persistence**
   - On sauvegarde le Contract COMPLET
   - Pas besoin de sauvegarder `ContractPeriod` séparément

3. ✅ **Invariants garantis**
   - Le Contract ne peut pas avoir un `costAmount` sans `client`
   - Le `lastModified` est toujours cohérent avec les modifications

4. ✅ **Logique métier encapsulée**
   - Pas de setters publics
   - Changements via méthodes métier (`changeCost()`)

---

## 📦 Exemple 2 : Client (notre code)

### Est-ce que Client est un Aggregate ?

**OUI !** Client est aussi un Aggregate (mais plus simple que Contract).

```java
// ┌─────────────────────────────────────┐
// │  AGGREGATE CLIENT                   │
// │                                     │
// │  ┌─────────────────────────┐       │
// │  │ Client                  │       │ ← Aggregate Root
// │  │ (Aggregate Root)        │       │
// │  ├─────────────────────────┤       │
// │  │ - UUID id               │       │ ← Identité
// │  │ - ClientName name       │       │ ← Value Object
// │  │ - Email email           │       │ ← Value Object
// │  │ - PhoneNumber phone     │       │ ← Value Object
// │  └─────────────────────────┘       │
// │           ▲                         │
// │           │                         │
// │  ┌────────┴────────┐               │
// │  │                 │               │
// │  Person          Company           │ ← Sous-types
// │  - birthDate     - companyId      │
// └─────────────────────────────────────┘
```

### Pourquoi Client est un Aggregate ?

1. ✅ **Cohérence des données**
   - Un Client doit TOUJOURS avoir un nom, email et téléphone valides
   - Garantit par le constructeur et l'absence de setters

2. ✅ **Frontière de persistence**
   - On sauvegarde un Client (Person ou Company) en UNE transaction
   - Pas de fragmentation

3. ✅ **Repository dédié**
   - `ClientRepository` pour gérer les Clients
   - Pas de repository pour `Email`, `PhoneNumber`, etc.

---

## 🔗 Relations entre Aggregates

### Règle d'Or : Référence par ID

```java
// ❌ MAUVAIS : Référence directe à un autre Aggregate
public class Contract {
    private Client client;  // ⚠️ Charge tout le client
    private List<Payment> payments;  // ⚠️ Charge tous les paiements !
}

// ✅ BON : Référence par ID
public class Contract {
    private UUID clientId;  // Juste l'ID
}

// Ou acceptable dans notre cas (car on en a souvent besoin)
public class Contract {
    private Client client;  // OK si on lazy-load et contrôle les transactions
}
```

**Pourquoi ?**
- 🚀 **Performance** : Pas de chargement massif
- 🔒 **Isolation** : Modifications d'un aggregate n'affectent pas l'autre
- 💾 **Transactions** : Chaque aggregate = sa propre transaction

---

## 📏 Taille d'un Aggregate

### ⚠️ Trop Petit
```java
// ❌ Chaque Value Object = un Aggregate
// Trop granulaire, trop de repositories !
EmailRepository emailRepo;
PhoneNumberRepository phoneRepo;
```

### ⚠️ Trop Gros
```java
// ❌ Tout dans un seul Aggregate
public class Company {
    private List<Employee> employees;  // 1000 employés !
    private List<Contract> contracts;  // 500 contrats !
    private List<Invoice> invoices;    // 10000 factures !
    // = Trop lourd, transactions trop longues
}
```

### ✅ Juste Bien
```java
// ✅ Aggregate cohérent et de taille raisonnable
public class Contract {
    private UUID clientId;  // Référence légère
    private ContractPeriod period;  // Value Object
    private ContractCost cost;      // Value Object
    // = Léger, cohérent, transactionnel
}
```

---

## 🎯 Les Règles d'un Aggregate

### 1. Une Seule Racine (Aggregate Root)
```java
// ✅ Contract = Root
// ❌ ContractPeriod n'a PAS de repository
// ❌ ContractCost n'a PAS de repository

// Tout passe par Contract
ContractRepository contractRepo;
```

### 2. Cohérence Transactionnelle
```java
// ✅ Sauvegarder Contract = tout est sauvegardé atomiquement
contractRepository.save(contract);

// Si ça échoue, RIEN n'est sauvegardé
// Si ça réussit, TOUT est sauvegardé
```

### 3. Invariants Protégés
```java
public class Contract {
    // Invariant : Si costAmount change, lastModified doit changer
    
    public void changeCost(ContractCost newCost) {
        this.costAmount = newCost;
        this.lastModified = LocalDateTime.now();  // ✅ Cohérence !
    }
    
    // ❌ Pas de setCostAmount() qui ne mettrait pas à jour lastModified
}
```

### 4. Modifications via Méthodes Métier
```java
// ❌ MAUVAIS : Setters publics
contract.setCostAmount(newCost);
contract.setLastModified(now);  // Oubli facile !

// ✅ BON : Méthode métier
contract.changeCost(newCost);  // Cohérence garantie
```

---

## 📊 Tableau Récapitulatif : Entity vs Aggregate

| Aspect | Entity Simple | Aggregate Root |
|--------|---------------|----------------|
| **Identité** | Oui (ID) | Oui (ID) |
| **Repository** | Parfois | Toujours |
| **Cohérence** | Soi-même | Tout l'aggregate |
| **Frontière transactionnelle** | Non | Oui |
| **Logique métier** | Oui | Oui (+ orchestration) |
| **Exemple** | Ligne de commande | Commande |

---

## 🎓 Aggregates dans notre Projet

### Contract Service

```
┌──────────────────────────┐
│ AGGREGATE: Client        │
│ Root: Client             │
│ - Person / Company       │
│ - Email, Phone, Name     │
└──────────────────────────┘

┌──────────────────────────┐
│ AGGREGATE: Contract      │
│ Root: Contract           │
│ - Client (référence)     │
│ - ContractPeriod         │
│ - ContractCost           │
└──────────────────────────┘
```

### Repositories

```java
// ✅ Un repository par Aggregate Root
interface ClientRepository {
    Client save(Client client);
    Optional<Client> findById(UUID id);
}

interface ContractRepository {
    Contract save(Contract contract);
    Optional<Contract> findById(UUID id);
}

// ❌ PAS de repository pour :
// - Email
// - PhoneNumber
// - ContractPeriod
// - ContractCost
```

---

## 💡 Règle Pratique pour Identifier un Aggregate

Pose-toi ces questions :

1. **Cohérence transactionnelle** : "Ces objets doivent-ils être cohérents ensemble ?"
   - Oui → Même aggregate
   - Non → Aggregates séparés

2. **Frontière de modification** : "Quand je modifie A, dois-je modifier B ?"
   - Oui → Même aggregate
   - Non → Aggregates séparés

3. **Frontière de persistence** : "Est-ce que je sauvegarde toujours ces objets ensemble ?"
   - Oui → Même aggregate
   - Non → Aggregates séparés

---

## 🎯 En Résumé

### Aggregate = 
- **Cohérence** : Tout est toujours cohérent
- **Transaction** : Tout ou rien
- **Frontière** : Limite claire
- **Protection** : Invariants garantis

### Dans notre code :
- ✅ `Contract` = Aggregate Root (avec period, cost)
- ✅ `Client` = Aggregate Root (avec name, email, phone)
- ✅ Pas de `EmailRepository`, `PhoneRepository`, etc.
- ✅ Cohérence garantie via méthodes métier (`changeCost()`)

**L'Aggregate est le GARDIEN de la cohérence du domaine !** 🛡️

