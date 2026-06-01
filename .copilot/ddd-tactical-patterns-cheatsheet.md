# DDD Tactical Patterns - Cheat Sheet

## 🎯 Patterns Tactiques vs Stratégiques

### Patterns Stratégiques (Strategic)
- 🗺️ **Bounded Context** : Limites d'un modèle métier
- 🔗 **Context Map** : Relations entre Bounded Contexts
- 💬 **Ubiquitous Language** : Langage partagé équipe/métier
- ⭐ **Core Domain** : Cœur de valeur métier

### Patterns Tactiques (Tactical) ⭐ **Ce document**
- 📦 Building blocks du domaine
- 🏗️ Structure interne d'un Bounded Context
- 💻 Implémentation concrète

---

## 1. Value Object

### Définition
Un Value Object représente un concept métier défini uniquement par ses attributs, sans identité propre. Deux Value Objects sont considérés égaux si tous leurs attributs ont les mêmes valeurs.

### Règles impératives

**Immutabilité stricte**
- Tous les champs doivent être final
- Aucun setter public
- Utiliser record Java ou classe avec champs final uniquement
- Toute modification retourne une nouvelle instance

**Factory method obligatoire**
- Méthode statique nommée `of()` pour la création
- La validation DOIT être faite dans cette factory ou dans le constructeur
- Impossible de créer une instance invalide
- Lever une exception métier si validation échoue

**Égalité par valeur**
- L'implémentation de equals() compare TOUS les attributs
- Pas de comparaison d'identité ou de référence
- hashCode() doit être cohérent avec equals()

**Encapsulation de la logique métier**
- Les opérations métier sur le concept vivent dans le Value Object
- Exemple : un ContractPeriod sait déterminer s'il est actif
- Exemple : un ContractCost sait calculer une augmentation en pourcentage
- Pas de logique métier dispersée ailleurs dans le code

**Validation dans le constructeur/factory**
- Toute contrainte métier est vérifiée à la création
- Exception métier levée si invalide
- Garantit qu'une instance existante est toujours valide

### Dans notre projet
- ClientName, ClientEmail, ClientPhoneNumber
- PersonBirthDate, CompanyIdentifier
- ContractPeriod, ContractCost

### Quand utiliser
- Concept métier sans identité propre
- Besoin de garantir la validité en permanence
- Concept réutilisé dans plusieurs Entities
- Besoin d'immutabilité pour éviter effets de bord

---

## 2. Entity

### Définition
Une Entity représente un objet métier avec une identité stable dans le temps. Deux Entities sont considérées égales si elles ont le même identifiant, même si leurs autres attributs diffèrent.

### Règles impératives

**Identité unique et stable**
- Champ id obligatoire (UUID recommandé)
- L'id ne change JAMAIS durant le cycle de vie
- L'id est assigné à la création (soit par le domain, soit par l'infrastructure)

**Égalité basée sur l'identité**
- equals() compare UNIQUEMENT l'id
- hashCode() basé UNIQUEMENT sur l'id
- Deux instances avec même id = même Entity (même si attributs différents)

**Mutabilité contrôlée**
- Les attributs peuvent changer
- JAMAIS de setters publics
- Méthodes métier nommées explicitement (changeEmail, updateCost)
- Chaque mutation retourne une nouvelle instance OU modifie l'instance actuelle selon pattern choisi
- Toute mutation valide les invariants

**Protection des invariants**
- L'Entity est responsable de sa cohérence interne
- Validation dans les méthodes de mutation
- Impossible de mettre l'Entity dans un état invalide depuis l'extérieur

**Composition avec Value Objects**
- Les attributs complexes sont des Value Objects
- L'Entity délègue la validation aux Value Objects
- Mutation = création d'un nouveau Value Object

### Dans notre projet
- Client (abstract sealed class, Aggregate Root)
- Person (extends Client)
- Company (extends Client)
- Contract (Aggregate Root)

### Quand utiliser
- Objet avec cycle de vie propre
- Besoin de tracer l'identité dans le temps
- État mutable mais contrôlé
- Point central de règles métier

---

## 3. Aggregate et Aggregate Root

### Définition Aggregate
Un Aggregate est un cluster d'objets du domaine (une Entity racine + ses Entities enfants + ses Value Objects) traités comme une unité cohérente pour les modifications et la persistance. Le boundary de l'Aggregate définit ce qui est considéré comme une unité transactionnelle.

### Définition Aggregate Root
L'Aggregate Root est l'Entity racine qui sert de point d'entrée unique pour accéder et modifier tout objet à l'intérieur du boundary de l'Aggregate.

### Règles impératives Aggregate

**Boundary transactionnel**
- Une transaction modifie UN SEUL Aggregate
- JAMAIS de transaction qui modifie plusieurs Aggregates simultanément
- Si besoin de modifier plusieurs Aggregates : eventual consistency via Domain Events

**Point d'entrée unique**
- Tout accès externe à l'Aggregate passe obligatoirement par la Root
- Les objets internes (Entities enfants, Value Objects) ne sont jamais exposés directement
- Les clients externes ne peuvent pas modifier directement les objets internes

**Cohérence forte à l'intérieur**
- L'Aggregate Root garantit tous les invariants du cluster
- Impossible de violer les règles métier en passant par la Root
- La validation est centralisée dans la Root

**Cohérence éventuelle à l'extérieur**
- Relations entre Aggregates via Domain Events
- Pas de dépendance directe (couplage fort) entre Aggregates

### Règles impératives Aggregate Root

**Repository obligatoire**
- Un Repository par Aggregate Root
- Le Repository charge et sauvegarde l'Aggregate complet (boundary entier)
- Jamais de Repository pour une Entity enfant

**Référence externe par ID uniquement**
- Un Aggregate ne contient JAMAIS la référence objet d'un autre Aggregate
- Utiliser l'ID (UUID) pour référencer un autre Aggregate
- Charger l'autre Aggregate via son Repository si nécessaire

**Taille limitée**
- Un Aggregate doit être petit (règle du pouce : charger en mémoire sans impact performance)
- Si trop grand : découper en plusieurs Aggregates plus petits

**Identité propre**
- L'Aggregate Root est une Entity (donc a un ID)
- Peut exister indépendamment des autres Aggregates

### Identifier un Aggregate Root

Checklist pour déterminer si une Entity doit être un Aggregate Root :
- A-t-elle une identité propre et stable ?
- Peut-elle exister indépendamment des autres objets ?
- A-t-elle son propre Repository ?
- Est-elle modifiée dans une transaction séparée ?
- Protège-t-elle un ensemble d'invariants métier ?
- Est-elle le point d'entrée naturel pour un cluster d'objets ?

Si oui à toutes ces questions : c'est un Aggregate Root.

### Dans notre projet

**Aggregate Client**
- Root : Client (abstract sealed class)
- Entities internes : Person, Company (subclasses de Client)
- Value Objects : ClientName, ClientEmail, ClientPhoneNumber, PersonBirthDate, CompanyIdentifier
- Invariants protégés : Email unique, Phone valide, Company identifier unique

**Aggregate Contract**
- Root : Contract
- Pas d'Entity enfant
- Value Objects : ContractPeriod, ContractCost
- Référence Client via UUID clientId (pas d'objet Client)
- Invariants protégés : Period valide, Cost positif, Contrat expiré non modifiable

### Décision : Pourquoi Contract est un Aggregate Root séparé

Bien que Contract "appartienne" conceptuellement à Client, il est un Aggregate Root indépendant car :
- Identité propre (UUID id)
- Persistance séparée (table contracts)
- Repository dédié (ContractRepository)
- Cycle de vie indépendant (modifiable sans charger Client)
- Invariants propres (isActive, changeCost avec validation)
- Référence Client par ID uniquement

Cette séparation permet scalabilité et découplage (extraction microservice possible).

---

## 4. Repository

### Définition
Un Repository est une abstraction qui représente une collection d'Aggregates. Il encapsule la logique de persistance et de récupération des Aggregates, donnant l'illusion d'une collection en mémoire.

### Règles impératives

**Interface dans le Domain, implémentation dans Infrastructure**
- L'interface Repository est définie dans le domain layer (port)
- L'implémentation concrète est dans l'infrastructure layer (adapter)
- Le domain ne dépend JAMAIS de l'infrastructure
- Inversion de dépendance respectée

**Un Repository par Aggregate Root**
- Jamais de Repository pour une Entity enfant
- Jamais de Repository pour un Value Object
- Si besoin d'un Repository : c'est probablement un Aggregate Root

**Méthodes métier, pas CRUD générique**
- Les méthodes reflètent le langage ubiquitaire
- Exemples : findActiveByClientId, sumActiveByClientId, closeAllActiveContracts
- Éviter les méthodes génériques comme findAll() sans contexte métier
- Chaque méthode a un sens métier précis

**Retourne des objets du Domain**
- Le Repository retourne des Aggregates (objets du domain)
- Jamais de JPA entities ou objets techniques
- Le mapping JPA Entity vers Domain se fait dans l'infrastructure

**Charge l'Aggregate complet**
- Quand on charge un Aggregate, on charge tout son boundary
- Les Value Objects et Entities enfants sont chargés avec la Root
- Pas de lazy loading qui exposerait les détails de persistance au domain

### Dans notre projet

**ClientRepository**
- Méthodes : save, findById, findByEmail, existsByEmail, existsByCompanyIdentifier, deleteById
- Retourne Client (peut être Person OU Company selon le type persisté)
- Gère le polymorphisme (sealed class)

**ContractRepository**
- Méthodes : save, findById, findActiveByClientIdPageable, sumActiveByClientId, closeAllActiveContracts
- Toutes les méthodes ont un sens métier
- Pas de findAll() générique (toujours contextualisé : par client, actifs, etc.)

### Repository vs DAO

Différences fondamentales :
- **DAO** : Data Access Object, opérations CRUD techniques, orienté table/base de données
- **Repository** : Collection d'objets métier, opérations métier, orienté domaine

Le Repository masque la complexité technique de la persistance au domain.

---

## 5. Domain Service

### Définition
Un Domain Service contient de la logique métier pure qui n'appartient naturellement à aucun Aggregate. C'est un objet sans état (stateless) qui coordonne des opérations métier.

### Règles impératives

**Logique métier pure uniquement**
- Contient UNIQUEMENT de la logique métier (business rules)
- Aucune logique technique (transaction, persistance, etc.)
- Vit dans le domain layer
- Utilise l'Ubiquitous Language dans ses méthodes

**Stateless (sans état)**
- Ne conserve aucun état entre les appels
- Toutes les données nécessaires sont passées en paramètres
- Peut avoir des dépendances injectées (Repositories, autres Services)

**Nommage explicite**
- Le nom doit refléter clairement sa responsabilité métier
- Exemples : ContractValidationService, PricingService, RoutingService
- Éviter les noms génériques comme ContractService si trop vague

### Quand utiliser un Domain Service

**Situations légitimes :**
- Logique implique plusieurs Aggregates
- Opération sans "propriétaire" naturel (ne va ni dans Client ni dans Contract)
- Validation cross-aggregate
- Calcul complexe utilisant plusieurs Aggregates
- Transformation ou conversion métier complexe

### Quand NE PAS utiliser un Domain Service

**Anti-patterns :**
- La logique appartient clairement à un Aggregate → mettre dans l'Aggregate
- Simple délégation au Repository → appeler le Repository directement
- Orchestration avec transaction → c'est un Application Service, pas un Domain Service
- Logique technique (envoi email, appel API) → c'est de l'infrastructure

### Domain Service vs Application Service

Distinction critique :
- **Domain Service** : Logique MÉTIER pure, domaine layer, pas de transaction
- **Application Service** : Orchestration + transaction, application layer, coordonne le domain

### Dans notre projet

**ClientService**
- Responsabilité : Point d'entrée unique pour récupérer un Client
- Méthode : findClientById avec gestion d'exception cohérente
- Justification : Centralise la gestion d'erreur (Client not found)

**ContractService**
- Responsabilités : Queries sur Contract avec validation
- Méthodes : getContractForClient, getActiveContractsForClient, sumActiveContractsForClient
- Justification : Queries simples + validation ownership (cross-aggregate)

**ContractValidationService**
- Responsabilité : Validations cross-aggregate Contract ↔ Client
- Méthode : ensureContractBelongsToClient
- Justification : Validation implique 2 Aggregates (Contract et Client)

---

## 6. Factory

### Définition
Une Factory encapsule la logique complexe de création d'objets du domaine. Elle garantit que les objets créés respectent tous les invariants métier dès leur construction.

### Règles impératives

**Validation complète à la création**
- Tous les invariants doivent être vérifiés
- L'objet créé est toujours dans un état valide
- Lever une exception métier si création impossible

**Méthodes statiques sur l'Aggregate**
- Nommer la factory method `of()` pour création métier
- Nommer la factory method `reconstitute()` pour reconstruction infrastructure
- Placer les méthodes directement dans la classe de l'Aggregate

**Distinction création métier vs reconstruction technique**
- `of()` : Création business avec validation métier complète, sans ID
- `reconstitute()` : Reconstruction depuis base de données, avec ID, validation minimale

**Builder pattern en support**
- Utiliser Builder (Lombok @Builder) pour simplifier la construction
- Les factories appellent le builder
- Builder privé, seules les factories sont publiques

### Quand utiliser Factory séparée (classe dédiée)

Situations légitimes pour une classe Factory séparée :
- Logique de création très complexe (10+ lignes)
- Besoin de dépendances externes (Repositories pour validation)
- Plusieurs variantes de création (Factory Method pattern)
- Création implique plusieurs Aggregates

### Dans notre projet

Approche retenue : Méthodes statiques sur les Aggregates
- Contract.of() : création métier
- Contract.reconstitute() : reconstruction infrastructure
- Person, Company : idem
- Value Objects : factory method of() dans le record/classe

---

## 7. Application Service / Use Case

### Définition
Un Application Service (ou Use Case) orchestre les opérations métier complexes en coordonnant Aggregates, Repositories et Domain Services. Il vit dans l'application layer et gère les aspects transactionnels.

### Règles impératives

**Orchestration uniquement, pas de logique métier**
- Coordonne les appels aux Domain Services et Repositories
- AUCUNE logique métier dans le Use Case
- Toute logique métier doit être déléguée au domain layer

**Gestion des transactions**
- Annoter avec @Transactional
- Une transaction par Use Case
- Gérer le scope transactionnel (read-only pour queries)

**Conversion primitives vers Value Objects**
- Les Use Cases reçoivent des primitives (String, BigDecimal, etc.)
- Convertir en Value Objects avant d'appeler le domain
- Valider via les factories des Value Objects

**Pattern Command**
- Utiliser des records Command pour encapsuler les paramètres
- Un Command par Use Case
- Nommage : CreateContractCommand, UpdateContractCostCommand

**Interface + Implémentation séparées**
- Interface dans usecase/ : CreateContractUseCase
- Implémentation à la racine de application/contract/
- Permet testabilité et découplage

### Responsabilités d'un Use Case

**Ce qu'un Use Case DOIT faire :**
- Valider l'existence des Aggregates référencés (Client existe ?)
- Convertir primitives → Value Objects
- Appeler les méthodes métier du domain
- Persister via Repositories
- Gérer la transaction
- Publier des Domain Events si nécessaire

**Ce qu'un Use Case NE DOIT PAS faire :**
- Contenir de la logique métier (calculs, validations business)
- Accéder directement à la base de données (passer par Repository)
- Contenir de la logique technique (email, files, etc.)

### Use Case vs Domain Service

Distinction critique :
- **Use Case** : Orchestration, transaction, coordination, application layer
- **Domain Service** : Logique métier pure, domain layer, pas de transaction

### Quand utiliser un Use Case

**Situations légitimes :**
- Command complexe (création, modification avec validation)
- Orchestration multi-Aggregates
- Transaction multi-étapes
- Besoin de publier des Domain Events
- Coordination Domain Services + Repositories

### Quand NE PAS utiliser un Use Case

**Anti-patterns :**
- Query simple sans logique → appeler Domain Service directement
- Simple délégation au Repository → pas de Use Case
- Pass-through service (aucune valeur ajoutée) → supprimer

### Dans notre projet

**Use Cases Commands (conservés) :**
- CreateContractUseCase : orchestration création avec validation Client
- UpdateContractCostUseCase : orchestration mise à jour avec validation ownership
- DeleteClientUseCase : orchestration suppression + fermeture contrats
- CloseActiveContractsUseCase : opération bulk métier

**Queries simples (SUPPRIMÉS) :**
- GetActiveContractsQuery → remplacé par appel direct ContractService
- SumActiveContractsQuery → remplacé par appel direct ContractService
- GetContractByIdQuery → remplacé par appel direct ContractService

Justification : Queries simples sont des pass-through, Domain Service suffit.

---

## 8. Domain Event

### Définition
Un Domain Event représente un fait métier important qui s'est produit dans le domaine. C'est un objet immuable qui capture l'essence d'un changement d'état ou d'une action métier accomplie.

### Règles impératives

**Nommage au passé**
- Le nom décrit ce qui s'est produit
- Exemples : ContractCreated, CostUpdated, ClientDeleted
- JAMAIS au présent ou futur (pas CreateContract, pas WillCreateContract)

**Immutabilité stricte**
- Utiliser record Java
- Tous les champs final
- Un Event ne change jamais une fois créé

**Données minimales**
- Contenir uniquement les IDs et données essentielles
- Pas de référence aux Aggregates complets
- Horodatage obligatoire (occurredAt)
- ID de l'émetteur (aggregateId)

**Publication après commit transaction**
- Publier APRÈS la transaction committée (doAfterCommit)
- Jamais avant (risque d'inconsistance)
- Si rollback : Event non publié

**Découplage des listeners**
- Les listeners vivent dans d'autres modules
- Pas de dépendance directe entre publisher et listeners
- Communication asynchrone privilégiée

### Dans notre projet

Non implémenté actuellement mais envisagé pour :
- ContractCreatedEvent : notification après création
- ContractExpiredEvent : fermeture automatique
- ClientDeletedEvent : cascade sur contrats

### Quand utiliser Domain Events

**Situations légitimes :**
- Découpler deux Aggregates (Client → Contract)
- Eventual consistency entre Aggregates
- Notification vers d'autres Bounded Contexts
- Audit trail / Event Sourcing
- Déclencher actions asynchrones

### Quand NE PAS utiliser

**Anti-patterns :**
- Synchroniser deux Aggregates dans même transaction (utiliser transaction)
- Remplacer appels de méthodes simples
- Communication technique (pas métier)

---

## 9. Specification Pattern

### Définition
Le pattern Specification encapsule des règles métier réutilisables et combinables. Une Specification détermine si un objet satisfait certains critères métier.

### Règles impératives

**Méthode isSatisfiedBy**
- Retourne boolean
- Prend l'objet candidat en paramètre
- Évalue les critères métier

**Composition via AND, OR, NOT**
- Permettre combinaison de Specifications
- Créer des règles complexes par composition
- Implémenter les opérateurs logiques

**Nom explicite métier**
- ActiveContractSpecification, pas ActiveSpecification
- Refléter l'Ubiquitous Language
- Nom doit être compréhensible par le métier

**Réutilisabilité**
- Une Specification peut être utilisée dans plusieurs contextes
- Tester indépendamment
- Composition encourage la réutilisation

### Dans notre projet

Non implémenté actuellement car nos règles sont simples et directement dans les Aggregates.

Envisageable si complexité augmente :
- ActiveContractSpecification
- ContractBelongsToClientSpecification
- ExpensiveContractSpecification (cost > threshold)

### Quand utiliser Specification

**Situations légitimes :**
- Règles métier complexes et réutilisées
- Besoin de combiner des critères dynamiquement
- Queries avec filtres métier multiples
- Validation complexe multi-critères

### Quand NE PAS utiliser

**Anti-patterns :**
- Règle simple utilisée une seule fois → méthode dans l'Aggregate
- Spécification technique (pas métier)
- Over-engineering pour des règles triviales

---

## 10. Exceptions du Domaine

### Définition
Les exceptions du domaine représentent des violations de règles métier. Elles sont levées par le domain layer quand un invariant est violé ou qu'une opération métier est impossible.

### Règles impératives

**Une exception par Aggregate**
- Package domain/client/exception/ pour exceptions Client
- Package domain/contract/exception/ pour exceptions Contract
- JAMAIS de package domain/exception/ global

**Nom explicite métier**
- Reflète la violation métier : ExpiredContractException, InvalidEmailException
- Pas de nom technique : ValidationException, DataException

**Message clair et métier**
- Message compréhensible par le métier
- Inclure les données contextuelles (IDs)
- Exemple : "Cannot modify expired contract: 123e4567"

**Extends RuntimeException**
- Pas d'exceptions checked
- Permet propagation naturelle
- Gestion centralisée dans infrastructure

**Levée dans le Domain uniquement**
- Les exceptions métier viennent du domain layer
- Jamais levées par infrastructure (sauf mapping)
- Application layer peut les laisser passer

### Dans notre projet

**Exceptions Client :**
- ClientNotFoundException
- InvalidClientException
- DuplicateEmailException
- DuplicateCompanyIdentifierException

**Exceptions Contract :**
- ContractNotFoundException
- InvalidContractException
- ExpiredContractException
- ContractNotOwnedByClientException

### Exceptions métier vs techniques

Distinction :
- **Métier** : Violation de règle business, domaine layer
- **Technique** : Problème infrastructure (DB down, network), infrastructure layer

Gestion :
- Exceptions métier → HTTP 4xx (400, 404, 409, 422)
- Exceptions techniques → HTTP 5xx (500, 503)

---

## Récapitulatif des Patterns

| Pattern | Rôle | Règle d'or |
|---------|------|------------|
| **Value Object** | Concept sans identité | Immutable, factory .of(), validation obligatoire |
| **Entity** | Objet avec identité | Equals sur ID, mutation contrôlée, pas de setters |
| **Aggregate** | Cluster cohérent | Boundary transactionnel, cohérence forte interne |
| **Aggregate Root** | Point d'entrée Aggregate | Repository obligatoire, référence externe par ID |
| **Repository** | Collection d'Aggregates | Interface domain, méthodes métier, charge complet |
| **Domain Service** | Logique sans propriétaire | Stateless, métier pur, pas de transaction |
| **Factory** | Création complexe | of() métier, reconstitute() infra, validation totale |
| **Use Case** | Orchestration | Transaction, coordination, pas de logique métier |
| **Domain Event** | Fait métier | Immutable, passé, publish après commit |
| **Specification** | Règle réutilisable | isSatisfiedBy, composition AND/OR/NOT |
| **Exception** | Violation métier | Par Aggregate, nom explicite, RuntimeException |

---

## Architecture en Couches

**Layered Architecture DDD :**

**Presentation Layer (Web)**
- Controllers REST
- DTOs (Data Transfer Objects)
- Mappers (Domain ↔ DTO)
- Exception Handlers

**Application Layer**
- Use Cases / Application Services
- Commands (records)
- Orchestration
- Transaction management

**Domain Layer** ⭐ (Cœur métier)
- Aggregates & Aggregate Roots
- Entities
- Value Objects
- Domain Services
- Repositories (interfaces)
- Factories
- Domain Events
- Exceptions métier

**Infrastructure Layer**
- JPA Entities
- Repository Implementations
- DB Mappers (Domain ↔ JPA)
- Configuration technique
- Clients externes (REST, SOAP)

**Règle de dépendance :**
- Domain ne dépend de RIEN
- Application dépend de Domain uniquement
- Infrastructure dépend de Domain (implémente les ports)
- Presentation dépend d'Application et Domain

---

## Références

**Livres fondamentaux :**
- Domain-Driven Design - Eric Evans (2003) - Le livre original
- Implementing Domain-Driven Design - Vaughn Vernon (2013) - Guide pratique

**Projets de référence :**
- dddsample-core (Citerus/Eric Evans) - Architecture classique Layered
- cargo-clean (gushakov) - Clean Architecture avec DDD

**Ressources en ligne :**
- DDD Reference - Eric Evans - Condensé gratuit du livre original
- Domain Language website - Glossaire et patterns

