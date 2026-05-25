# DDD Strategic Patterns

## Définition

Les **Patterns Stratégiques** (Strategic Patterns) concernent l'organisation à haut niveau du système et la gestion de la complexité métier. Ils définissent comment découper le domaine métier en morceaux cohérents et gérables, et comment ces morceaux communiquent entre eux.

**À distinguer des Patterns Tactiques** qui concernent l'implémentation concrète à l'intérieur d'un Bounded Context.

---

## 1. Ubiquitous Language

### Définition
Le Ubiquitous Language (Langage Omniprésent) est un langage commun rigoureux partagé par l'équipe de développement et les experts métier. Ce langage est utilisé partout : dans le code, les conversations, la documentation, les tests.

### Règles impératives

**Un seul langage pour tous**
- Les développeurs utilisent les termes métier dans le code
- Les experts métier comprennent les noms de classes et méthodes
- Aucune traduction entre "langage technique" et "langage métier"
- Le code devient la documentation vivante du métier

**Découverte collaborative**
- Le langage émerge des discussions entre dev et métier
- Sessions de modélisation collaborative (Event Storming, Example Mapping)
- Affinage continu lors des refinements et reviews
- Glossaire partagé et maintenu

**Refléter le langage dans le code**
- Noms de classes = concepts métier exacts
- Méthodes = verbes métier (bookCargo, not saveCargoToDatabase)
- Éviter termes techniques génériques (Manager, Handler, Processor)
- Tests écrits dans le langage métier (Given/When/Then)

**Cohérence stricte dans le Bounded Context**
- Un terme = une seule signification dans un contexte
- Si ambiguïté : c'est probablement deux contextes différents
- Rejet des synonymes (choisir UN terme et s'y tenir)

**Évolution du langage**
- Le langage évolue avec la compréhension métier
- Refactoring du code si le langage change
- Documenter les changements de terminologie
- Mise à jour du glossaire en continu

### Dans notre projet

**Termes Ubiquitaires identifiés :**
- Client (pas Customer, pas User) - Personne ou entreprise ayant des contrats
- Person vs Company - Deux types de Client distincts
- Contract (pas Policy, pas Agreement) - Contrat d'assurance
- Active Contract - Contrat où la date courante < date de fin
- Cost Amount - Montant du coût du contrat
- Contract Period - Période de validité (start/end date)
- Client Identifier - Identifiant unique du client (email ou company identifier)

**Termes évités (trop techniques) :**
- Entity, Repository, Service (dans les conversations métier)
- CRUD, Persistence, Database
- DTO, Mapper, Controller

**Glossaire maintenu dans la documentation**
- Chaque terme métier défini précisément
- Exemples concrets pour chaque concept
- Relations entre concepts explicitées

### Quand le langage diverge

**Signaux d'alerte :**
- Un même terme a deux significations différentes
- Besoin de "traduire" entre dev et métier
- Confusion récurrente sur un concept
- Code difficile à expliquer au métier

**Actions correctives :**
- Identifier le malentendu
- Affiner la définition avec le métier
- Refactorer le code pour refléter la nouvelle compréhension
- Documenter la décision dans l'ADR (Architecture Decision Record)

---

## 2. Bounded Context

### Définition
Un Bounded Context est une frontière explicite à l'intérieur de laquelle un modèle de domaine particulier est défini et applicable. C'est une limite linguistique et conceptuelle : à l'intérieur du contexte, les termes ont une signification précise et cohérente.

### Règles impératives

**Un modèle par contexte**
- Chaque Bounded Context a son propre modèle de domaine
- Un même concept peut exister dans plusieurs contextes avec des significations différentes
- Pas de partage de code domain entre contextes (duplication acceptable)
- Chaque contexte est autonome et cohérent

**Frontières explicites**
- Les limites du contexte sont clairement définies et documentées
- On sait précisément ce qui est à l'intérieur et à l'extérieur
- Les dépendances externes sont explicites
- Pas de fuite du modèle interne vers l'extérieur

**Autonomie du contexte**
- Un Bounded Context peut fonctionner indépendamment
- Possède sa propre base de données (ou schéma dédié)
- Peut être développé et déployé séparément
- Équipe dédiée responsable du contexte

**Communication via interfaces publiques**
- API REST, Events, Messages pour communiquer avec l'extérieur
- Pas d'accès direct à la base de données d'un autre contexte
- Contrats d'interface versionnés et stables
- Anti-Corruption Layer pour protéger le modèle interne

**Ubiquitous Language propre**
- Chaque contexte a son Ubiquitous Language
- Un terme peut avoir des significations différentes dans deux contextes
- Traduction explicite à la frontière si nécessaire

### Identifier un Bounded Context

**Critères de découpage :**
- Cohérence linguistique : mêmes termes, même signification
- Cohérence métier : même sous-domaine, mêmes règles
- Autonomie d'équipe : peut être développé par une équipe séparée
- Autonomie technique : peut avoir sa propre stack technique
- Frontière transactionnelle : rarement besoin de transaction distribuée

**Questions à se poser :**
- Ce concept a-t-il plusieurs significations selon le contexte ?
- Cette frontière facilite-t-elle ou complique-t-elle le développement ?
- Ces deux parties du système évoluent-elles à des rythmes différents ?
- Des équipes différentes pourraient-elles gérer ces contextes ?

### Dans notre projet

**Bounded Context actuel : Contract Management**

**Périmètre :**
- Gestion des clients (Person, Company)
- Gestion des contrats d'assurance
- Validation des relations Client-Contract
- Calculs liés aux contrats actifs

**Frontières :**
- Ne gère PAS les sinistres (claims) - ce serait un autre contexte
- Ne gère PAS les paiements - ce serait un autre contexte
- Ne gère PAS la souscription (underwriting) - ce serait un autre contexte

**Modèle du contexte :**
- Client (Aggregate Root) avec Person et Company
- Contract (Aggregate Root)
- Value Objects spécifiques au contexte

**Si évolution vers microservices :**
Potentiels Bounded Contexts futurs :
- Client Management Context (gestion clients uniquement)
- Contract Management Context (gestion contrats uniquement)
- Claim Management Context (gestion sinistres)
- Payment Context (gestion paiements)

### Taille idéale d'un Bounded Context

**Règles empiriques :**
- Petit assez pour qu'une équipe le comprenne entièrement
- Grand assez pour avoir de la cohésion métier
- Éviter les micro-contextes (trop de complexité de communication)
- Éviter les macro-contextes (perte de cohérence, trop complexe)

**Signaux qu'un contexte est trop grand :**
- Équipe ne comprend plus tout le contexte
- Termes ambigus ou conflictuels
- Parties évoluant à des rythmes très différents
- Transactions distribuées fréquentes nécessaires

**Signaux qu'un contexte est trop petit :**
- Communication excessive entre contextes
- Logique métier éparpillée sur plusieurs contextes
- Duplication importante sans valeur
- Complexité de coordination élevée

---

## 3. Context Map

### Définition
Une Context Map est une représentation visuelle de tous les Bounded Contexts d'un système et de leurs relations. Elle documente comment les contextes communiquent, qui dépend de qui, et quel type de relation existe entre eux.

### Types de relations entre contextes

**Partnership (Partenariat)**
- Deux contextes coordonnent leur évolution
- Dépendance mutuelle forte
- Planification commune des releases
- Équipes collaborent étroitement

**Shared Kernel (Noyau Partagé)**
- Sous-ensemble du modèle partagé entre contextes
- Modifications coordonnées obligatoires
- À utiliser avec parcimonie (couplage fort)
- Code commun minimal et stable

**Customer-Supplier (Client-Fournisseur)**
- Un contexte (Supplier) fournit des services à l'autre (Customer)
- Customer dépend du Supplier
- Supplier doit satisfaire les besoins du Customer
- Négociation des contrats d'interface

**Conformist (Conformiste)**
- Un contexte se conforme totalement au modèle d'un autre
- Aucune influence sur le modèle upstream
- Acceptation du modèle tel quel
- Utilisé quand aucun pouvoir de négociation

**Anti-Corruption Layer (ACL)**
- Couche de protection entre deux contextes
- Traduit les modèles externes vers le modèle interne
- Protège l'intégrité du Ubiquitous Language interne
- Isole des changements externes
- Recommandé pour intégrations legacy ou externes

**Open Host Service (OHS)**
- API publique bien définie pour accéder au contexte
- Documentée et versionnée
- Stable et orientée besoins clients
- Souvent REST API ou GraphQL

**Published Language (PL)**
- Langage commun de communication entre contextes
- Format standardisé (JSON, XML, Protocol Buffers)
- Documentation claire du schéma
- Versioning géré

**Separate Ways**
- Deux contextes sans relation directe
- Fonctionnent de manière totalement indépendante
- Duplication acceptée
- Pas de communication

### Dans notre projet

**Context Map actuel (monolithe modulaire) :**

Notre système est actuellement un seul Bounded Context (Contract Management) donc pas de Context Map complexe.

**Si évolution vers microservices, Context Map potentielle :**

```
Client Management Context
    │
    │ (Customer-Supplier + Open Host Service)
    │ Fournit données clients via REST API
    │
    ▼
Contract Management Context ← Notre contexte actuel
    │
    │ (Customer-Supplier + ACL)
    │ Consomme API externe de pricing
    │
    ▼
External Pricing Service (système externe)
```

Relations possibles futures :
- Contract Management → Client Management : Customer-Supplier (besoin des données clients)
- Contract Management → Claim Management : Customer-Supplier (contrat lié aux sinistres)
- Contract Management → Payment Context : Partnership (coordination paiements/contrats)

**Anti-Corruption Layer pour intégration externe :**
Si intégration avec système legacy ou partenaire externe :
- Mapper les concepts externes vers notre Ubiquitous Language
- Protéger notre modèle des changements externes
- Valider et nettoyer les données entrantes
- Transformer les données sortantes selon format externe

---

## 4. Core Domain, Supporting Subdomain, Generic Subdomain

### Définition
Cette classification stratégique aide à prioriser les investissements en distinguant ce qui apporte un avantage compétitif (Core Domain) de ce qui est nécessaire mais standard (Supporting/Generic).

### Core Domain (Domaine Cœur)

**Définition :**
Le Core Domain est la partie du système qui apporte un avantage compétitif unique. C'est la raison d'être du système, ce qui différencie l'entreprise de ses concurrents.

**Caractéristiques :**
- Valeur métier maximale
- Différenciation compétitive
- Logique métier complexe et propriétaire
- Évolution fréquente
- Investissement maximal (meilleurs développeurs, DDD complet)

**Dans notre contexte d'assurance :**
Exemples de Core Domain potentiels :
- Algorithme de pricing propriétaire
- Règles de souscription innovantes
- Modèle de risque unique

**Dans notre projet actuel :**
Contract Management est probablement un Supporting Subdomain (nécessaire mais pas différenciant).

### Supporting Subdomain (Sous-domaine Support)

**Définition :**
Un Supporting Subdomain est nécessaire au fonctionnement du système mais n'apporte pas de différenciation compétitive. C'est du métier spécifique à l'entreprise mais pas stratégique.

**Caractéristiques :**
- Nécessaire au business
- Métier spécifique mais pas différenciant
- Peut être modélisé en DDD (mais moins d'investissement que Core)
- Peut évoluer vers Core si stratégie change

**Dans notre projet :**
- Gestion des clients (Person/Company) - nécessaire mais standard
- Gestion basique des contrats - nécessaire mais pas innovant

**Approche :**
- Appliquer DDD tactical patterns
- Qualité de code élevée mais moins de sophistication que Core
- Équipe compétente mais pas forcément les meilleurs

### Generic Subdomain (Sous-domaine Générique)

**Définition :**
Un Generic Subdomain résout un problème commun à de nombreux systèmes. Ce n'est pas spécifique au métier de l'entreprise.

**Caractéristiques :**
- Solution standardisée existe
- Aucune valeur compétitive
- Peut être acheté ou utilisé open-source
- Investissement minimal

**Exemples typiques :**
- Authentification/Autorisation (utiliser Keycloak, Auth0)
- Envoi d'emails (utiliser SendGrid, AWS SES)
- Stockage de fichiers (utiliser S3, Azure Blob)
- Gestion de workflows (utiliser Camunda, Temporal)
- Reporting (utiliser Jasper, Crystal Reports)

**Approche :**
- Acheter une solution du marché (build vs buy → buy)
- Utiliser un framework ou bibliothèque open-source
- Investissement minimal en développement custom
- Pas de DDD (juste intégration technique)

### Stratégie d'investissement

**Allocation des ressources :**
- Core Domain : 60-70% du budget dev, meilleurs talents, DDD complet
- Supporting Subdomains : 20-30% du budget, équipe compétente, DDD tactique
- Generic Subdomains : 5-10% du budget, solutions du marché, intégration minimale

**Évolution dans le temps :**
- Un Supporting peut devenir Core si stratégie change
- Un Core peut devenir Supporting si concurrence rattrape
- Generic reste Generic (sauf innovation disruptive improbable)

**Décisions architecturales :**
- Core Domain : Architecture flexible, découplage maximal, tests exhaustifs
- Supporting : Architecture solide mais pragmatique
- Generic : Intégration via ACL, abstraction minimale

---

## 5. Distillation du Domain

### Définition
La Distillation du Domain est le processus continu d'identification, d'extraction et de mise en valeur du Core Domain, en le séparant clairement des autres parties du système.

### Objectifs

**Clarté du Core :**
- Rendre le Core Domain explicite et visible
- Séparer physiquement Core et Supporting
- Documenter ce qui est Core vs Supporting vs Generic

**Focus sur la valeur :**
- Concentrer l'attention sur ce qui compte vraiment
- Investir là où c'est important
- Simplifier ou déléguer le reste

**Faciliter l'évolution :**
- Le Core peut évoluer indépendamment
- Les changements dans Supporting n'impactent pas Core
- Generic est isolé et remplaçable

### Techniques de distillation

**Segregated Core :**
- Code du Core dans des packages/modules séparés
- Tests du Core séparés et exhaustifs
- Documentation du Core prioritaire
- Review du code Core plus strictes

**Abstract Core :**
- Interfaces et abstractions du Core clairement définies
- Implémentations techniques dans Supporting/Infrastructure
- Core Domain indépendant des détails techniques

**Cohesive Mechanisms :**
- Mécanismes techniques génériques extraits du Core
- Réutilisables par Core et Supporting
- Bien testés et documentés séparément

### Dans notre projet

**Identification actuelle :**
Notre projet Contract Management est probablement un Supporting Subdomain complet (pas de Core Domain identifié).

**Si Core Domain émergeait (exemple) :**
Imaginons que l'algorithme de calcul de prime devienne stratégique et propriétaire :

Structure suggérée :
```
domain/
  core/                          ← Core Domain séparé
    pricing/
      PricingEngine
      RiskCalculator
      PremiumFormula
  supporting/                    ← Supporting Subdomains
    client/
      Client, Person, Company
    contract/
      Contract
```

**Critères de validation :**
- Le Core est-il clairement identifiable ?
- L'investissement est-il proportionnel à la valeur ?
- Le Core peut-il évoluer indépendamment ?

---

## Récapitulatif Strategic Patterns

| Pattern | Rôle | Règle d'or |
|---------|------|------------|
| **Ubiquitous Language** | Langage commun dev/métier | Un terme = une signification, utilisé partout |
| **Bounded Context** | Frontière de cohérence | Un modèle par contexte, autonomie complète |
| **Context Map** | Relations entre contextes | Documenter toutes les intégrations |
| **Core Domain** | Avantage compétitif | Investissement maximal, meilleurs talents |
| **Supporting Subdomain** | Nécessaire mais pas différenciant | DDD tactique, équipe compétente |
| **Generic Subdomain** | Standard du marché | Acheter ou open-source, investissement minimal |

---

## Application dans notre projet

**Bounded Context actuel :**
- Contract Management (monolithe modulaire)
- Ubiquitous Language défini et appliqué
- Modèle cohérent avec 2 Aggregates (Client, Contract)

**Classification des domaines :**
- Core Domain : Non identifié (projet educational/technique)
- Supporting : Gestion Client + Contract (tout le projet actuel)
- Generic : Authentification (Spring Security), Validation (Bean Validation)

**Évolution potentielle :**
Si contexte réel d'assurance :
- Séparer en plusieurs Bounded Contexts (Client, Contract, Claim, Payment)
- Identifier le Core Domain (probablement Underwriting/Pricing)
- Context Map avec relations Customer-Supplier et ACL

**Principes appliqués :**
- Ubiquitous Language strict et documenté
- Bounded Context bien défini (même si monolithe)
- Séparation Domain/Infrastructure (inversion de dépendance)
- Modèle cohérent sans fuites d'abstraction

---

## Références

**Livres :**
- Domain-Driven Design - Eric Evans - Chapitres sur Strategic Design
- Implementing Domain-Driven Design - Vaughn Vernon - Partie II Strategic Design

**Ressources :**
- DDD Reference - Section Strategic Design
- Context Mapping par Martin Fowler
- Bounded Context pattern sur Domain Language

**Outils :**
- Context Mapper (outil de modélisation Context Maps)
- Event Storming (découverte Bounded Contexts)
- Domain Storytelling (découverte Ubiquitous Language)

