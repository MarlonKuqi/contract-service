# 🗂️ Organisation des Dossiers : DDD vs Technique

**Question** : Dois-je mixer des dossiers DDD (style `valueobject/`) et des dossiers techniques ?

**Réponse courte** : 🎯 **OUI, privilégie les dossiers DDD** pour clarifier l'intention et faciliter la navigation.

---

## 📊 Comparaison des 3 Approches

### Option 1️⃣ : Structure ACTUELLE (Technique/Plate)

```
domain/
├── client/
│   ├── Client.java                    ← Aggregate Root
│   ├── Person.java                    ← Entity
│   ├── Company.java                   ← Entity
│   ├── ClientEmail.java               ← Value Object
│   ├── ClientName.java                ← Value Object
│   ├── ClientPhoneNumber.java         ← Value Object
│   ├── CompanyIdentifier.java         ← Value Object
│   ├── PersonBirthDate.java           ← Value Object
│   ├── ClientRepository.java          ← Repository
│   ├── ClientService.java             ← Domain Service
│   └── exception/                     ← Dossier technique
│       ├── InvalidEmailException.java
│       ├── ClientAlreadyExistsException.java
│       └── ...
│
└── contract/
    ├── Contract.java                  ← Aggregate Root
    ├── ContractCost.java              ← Value Object
    ├── ContractPeriod.java            ← Value Object
    ├── ContractRepository.java        ← Repository
    ├── ContractService.java           ← Domain Service
    └── exception/                     ← Dossier technique
        └── ...
```

#### ✅ Avantages
- ✅ Simple (pas de nesting profond)
- ✅ Rapide à mettre en place
- ✅ Peu de changements si refactoring

#### ❌ Inconvénients
- ❌ **Tout au même niveau** : difficile de distinguer Aggregates / VOs / Services
- ❌ **Pas d'indication du pattern DDD** : un nouveau dev doit deviner
- ❌ **Scalabilité limitée** : si l'aggregate grossit, le dossier devient illisible
- ❌ **Mélange** : `exception/` est un dossier technique au milieu de concepts DDD

#### 🎯 Verdict : **Acceptable pour petit projet**, mais pas optimal

---

### Option 2️⃣ : Structure HYBRIDE (DDD + Pragmatique) 🏆 RECOMMANDÉ

```
domain/
├── shared/                            ← NOUVEAU : Concepts partagés
│   ├── event/                         ← Dossier DDD
│   │   ├── DomainEvent.java
│   │   └── DomainEventPublisher.java
│   ├── Page.java                      ← VO partagé (pagination)
│   └── PageRequest.java               ← VO partagé (pagination)
│
├── client/                            ← Aggregate Client
│   ├── Client.java                    ← Aggregate Root
│   ├── Person.java                    ← Entity
│   ├── Company.java                   ← Entity
│   ├── ClientEmail.java               ← VO (reste au même niveau)
│   ├── ClientName.java                ← VO
│   ├── ClientPhoneNumber.java         ← VO
│   ├── CompanyIdentifier.java         ← VO
│   ├── PersonBirthDate.java           ← VO
│   ├── ClientFactory.java             ← NOUVEAU : Factory
│   ├── ClientRepository.java          ← Repository
│   ├── ClientService.java             ← Domain Service
│   ├── event/                         ← NOUVEAU : Dossier DDD
│   │   ├── ClientCreatedEvent.java
│   │   ├── ClientDeletedEvent.java
│   │   └── ClientUpdatedEvent.java
│   └── exception/                     ← Dossier technique (gardé)
│       └── ...
│
└── contract/                          ← Aggregate Contract
    ├── Contract.java                  ← Aggregate Root
    ├── ContractCost.java              ← VO
    ├── ContractPeriod.java            ← VO
    ├── ContractFactory.java           ← NOUVEAU : Factory
    ├── ContractRepository.java        ← Repository
    ├── ContractService.java           ← Domain Service
    ├── event/                         ← NOUVEAU : Dossier DDD
    │   ├── ContractCreatedEvent.java
    │   ├── ContractCostChangedEvent.java
    │   └── ContractClosedEvent.java
    └── exception/                     ← Dossier technique
        └── ...
```

#### ✅ Avantages
- ✅ **Équilibre** : ajoute des dossiers DDD sans tout réorganiser
- ✅ **Peu de refactoring** : juste ajouter `shared/event/` et `*/event/`
- ✅ **Clarté des events** : tous les events au même endroit
- ✅ **Concepts partagés** : `shared/` pour VOs utilisés partout
- ✅ **Pragmatique** : VOs restent au niveau de leur aggregate (pas de sur-ingénierie)

#### ❌ Inconvénients
- ❌ **VOs pas explicitement marqués** : pas de dossier `valueobject/`
- ❌ **Mix patterns DDD et techniques** : `exception/` reste technique

#### 🎯 Verdict : **Optimal pour ton projet** 🥇
- Ajoute les concepts DDD manquants (events, factories, shared)
- Garde la structure existante (peu de refactoring)
- Scalable pour croissance future

---

### Option 3️⃣ : Structure PURE DDD (Par Pattern)

```
domain/
├── shared/                            ← Concepts partagés
│   ├── valueobject/                   ← Dossier DDD
│   │   ├── Email.java                 ← (ClientEmail renommé)
│   │   ├── PhoneNumber.java           ← (ClientPhoneNumber renommé)
│   │   └── Name.java                  ← (ClientName renommé)
│   ├── event/                         ← Dossier DDD
│   │   ├── DomainEvent.java
│   │   └── DomainEventPublisher.java
│   └── exception/
│       └── DomainException.java
│
├── client/                            ← Aggregate Client
│   ├── aggregate/                     ← NOUVEAU : Dossier DDD
│   │   ├── Client.java                ← Aggregate Root
│   │   ├── Person.java                ← Entity
│   │   └── Company.java               ← Entity
│   ├── valueobject/                   ← NOUVEAU : Dossier DDD
│   │   ├── CompanyIdentifier.java
│   │   └── PersonBirthDate.java
│   ├── factory/                       ← NOUVEAU : Dossier DDD
│   │   └── ClientFactory.java
│   ├── repository/                    ← NOUVEAU : Dossier DDD
│   │   └── ClientRepository.java
│   ├── service/                       ← NOUVEAU : Dossier DDD
│   │   └── ClientService.java
│   ├── event/                         ← NOUVEAU : Dossier DDD
│   │   ├── ClientCreatedEvent.java
│   │   ├── ClientDeletedEvent.java
│   │   └── ClientUpdatedEvent.java
│   └── exception/                     ← Dossier technique
│       └── ...
│
└── contract/                          ← Aggregate Contract
    ├── aggregate/                     ← Dossier DDD
    │   └── Contract.java
    ├── valueobject/                   ← Dossier DDD
    │   ├── ContractCost.java
    │   └── ContractPeriod.java
    ├── factory/                       ← Dossier DDD
    │   └── ContractFactory.java
    ├── repository/                    ← Dossier DDD
    │   └── ContractRepository.java
    ├── service/                       ← Dossier DDD
    │   └── ContractService.java
    ├── event/                         ← Dossier DDD
    │   └── ...
    └── exception/                     ← Dossier technique
        └── ...
```

#### ✅ Avantages
- ✅ **Clarté maximale** : chaque pattern DDD a son dossier dédié
- ✅ **Intention explicite** : chercher un VO → `valueobject/`, chercher une Factory → `factory/`
- ✅ **Pédagogique** : la structure enseigne DDD
- ✅ **Scalable** : facile d'ajouter de nouveaux éléments
- ✅ **Séparation parfaite** : DDD vs technique

#### ❌ Inconvénients
- ❌ **Verbosité** : beaucoup de sous-dossiers
- ❌ **Overkill pour petit projet** : semble sur-ingénéré
- ❌ **Navigation plus profonde** : `client/valueobject/CompanyIdentifier.java` vs `client/CompanyIdentifier.java`
- ❌ **Refactoring massif** : déplacer tous les fichiers

#### 🎯 Verdict : **Idéal pour gros projet d'entreprise**, mais trop pour un technical exercise

---

## 🎯 Comparaison Visuelle

| Critère | Option 1 (Actuel) | Option 2 (Hybride) 🏆 | Option 3 (Pure DDD) |
|---------|-------------------|----------------------|---------------------|
| **Clarté DDD** | 🟡 Moyenne | 🟢 Bonne | 🟢 Excellente |
| **Simplicité** | 🟢 Simple | 🟢 Simple | 🟡 Complexe |
| **Scalabilité** | 🟡 Limitée | 🟢 Bonne | 🟢 Excellente |
| **Temps refactoring** | ✅ 0h | ✅ 2h | ❌ 6h |
| **Pragmatisme** | 🟢 Très pragmatique | 🟢 Pragmatique | 🔴 Académique |
| **Taille projet** | Petit (<5 aggregates) | Moyen (<10) | Grand (10+) |

---

## 📋 Règle Générale : DDD vs Technique

### Dossiers DDD (patterns explicites)

```
✅ UTILISE des dossiers DDD pour :

1. event/              ← Domain Events (concept DDD stratégique)
2. factory/            ← Factories (pattern DDD explicite)
3. valueobject/        ← Value Objects (si beaucoup de VOs, >10)
4. aggregate/          ← Aggregates (si beaucoup d'Entities dans l'aggregate)
5. specification/      ← Specifications (pattern DDD avancé)
6. policy/             ← Domain Policies (règles métier complexes)
```

**Pourquoi ?**
- ✅ Ces patterns sont **des concepts DDD explicites**
- ✅ Ils aident à **naviguer** dans le code
- ✅ Ils **enseignent DDD** aux nouveaux développeurs

---

### Dossiers Techniques (organisation pratique)

```
✅ UTILISE des dossiers techniques pour :

1. exception/          ← Exceptions (pas un pattern DDD, juste de l'organisation)
2. dto/                ← DTOs (couche Application/Web, pas Domain)
3. mapper/             ← Mappers (infrastructure technique)
4. config/             ← Configuration (infrastructure)
5. assembler/          ← Assemblers JPA↔Domain (infrastructure)
```

**Pourquoi ?**
- ✅ Ce sont des **détails d'implémentation**, pas des concepts métier
- ✅ Ils **regroupent des fichiers** similaires (pratique)
- ✅ Ils ne nuisent pas à la **compréhension du domaine**

---

## 🤔 Questions/Réponses

### Q1 : "Les dossiers techniques dans le domaine, c'est grave ?"

**R** : ❌ Non, ce n'est **pas un problème majeur**.

**Exemples acceptables** :
```
domain/client/exception/    ← OK, ce sont des exceptions MÉTIER
domain/contract/exception/  ← OK
```

**Exemples problématiques** :
```
domain/client/dto/          ← ❌ MAUVAIS, les DTOs sont dans Web/Application
domain/client/jpa/          ← ❌ MAUVAIS, JPA est dans Infrastructure
```

**Règle d'or** : Si le dossier contient des **concepts métier** (même organisés techniquement), c'est OK.

---

### Q2 : "Dois-je créer un dossier `valueobject/` ?"

**R** : 🤷 **Ça dépend du nombre de VOs**.

**SI tu as < 5 VOs par aggregate** :
```
client/
├── ClientEmail.java         ← Garder au même niveau
├── ClientName.java          ← Pas de dossier valueobject/
└── ...
```
✅ **Pas besoin**, c'est lisible

**SI tu as > 10 VOs par aggregate** :
```
client/
├── aggregate/
│   └── Client.java
├── valueobject/             ← Créer un dossier
│   ├── ClientEmail.java
│   ├── ClientName.java
│   ├── ClientPhoneNumber.java
│   ├── CompanyIdentifier.java
│   ├── PersonBirthDate.java
│   ├── ClientAddress.java   ← Beaucoup de VOs
│   ├── ClientType.java
│   └── ...
```
✅ **Dossier recommandé** pour éviter le chaos

**Ton cas** : Tu as ~5 VOs par aggregate → **Pas besoin de `valueobject/`**

---

### Q3 : "Les dossiers DDD, c'est pas trop 'Java' comme approche ?"

**R** : 🤷 **C'est un débat culturel**.

**Approche Java/Enterprise** :
```
client/
├── aggregate/
├── valueobject/
├── factory/
├── repository/
└── ...
```
✅ Très structuré, explicite, verbeux

**Approche Kotlin/Moderne** :
```
client/
├── Client.kt              ← Aggregate + VOs dans le même fichier
├── ClientEvents.kt        ← Tous les events ensemble
└── ClientRepository.kt
```
✅ Concis, moins de fichiers, plus moderne

**Ton contexte** : Projet Java + exercice technique → **Approche Java structurée est appropriée**

---

### Q4 : "Quelle approche impressionne le plus un recruteur ?"

**R** : 🎯 **Option 2 (Hybride)**.

**Pourquoi ?**
- ✅ **Équilibre** : montre que tu connais DDD sans sur-ingénierie
- ✅ **Pragmatique** : montre que tu fais des choix réfléchis
- ✅ **Évolutif** : structure peut croître si besoin

**Option 3 (Pure DDD)** pourrait être perçue comme :
- 🟡 "Ce dev aime la structure" (positif)
- 🟡 "Ce dev over-engineer les petits projets" (négatif)

**Option 1 (Actuel)** pourrait être perçue comme :
- 🟡 "Ce dev connaît les bases de DDD" (positif)
- 🟡 "Ce dev n'a pas pensé à l'évolution" (neutre)

---

## ✅ Recommandation Finale

### Pour ton projet `contract-service` :

**Adopte l'Option 2 (Hybride)** en ajoutant juste :

```
domain/
├── shared/               ← NOUVEAU package
│   └── event/
│       ├── DomainEvent.java
│       └── DomainEventPublisher.java
│
├── client/
│   ├── event/            ← NOUVEAU package
│   │   ├── ClientCreatedEvent.java
│   │   └── ClientDeletedEvent.java
│   ├── ClientFactory.java  ← NOUVEAU fichier
│   └── ... (reste inchangé)
│
└── contract/
    ├── event/            ← NOUVEAU package
    │   └── ...
    ├── ContractFactory.java  ← NOUVEAU fichier
    └── ... (reste inchangé)
```

**Changements requis** :
1. ✅ Créer `domain/shared/event/` (2 interfaces)
2. ✅ Créer `domain/client/event/` (2-3 events)
3. ✅ Créer `domain/contract/event/` (2-3 events)
4. ✅ Créer `ClientFactory.java` et `ContractFactory.java`

**Temps estimé** : ⏱️ **2h** (inclus dans les 9h du plan d'action)

**Résultat** : Structure claire, DDD explicite, sans over-engineering.

---

## 📚 Exemples de Grands Projets

### Spring Framework (Projet Java Enterprise)

```
spring-data-jpa/
├── domain/
│   ├── AbstractAuditable.java      ← Entity
│   ├── Auditable.java              ← Interface
│   └── ... (mix patterns)          ← Pas de sous-dossiers DDD
```

👉 **Approche plate** (comme ton Option 1)

### Axon Framework (DDD/CQRS Framework)

```
axonframework/
├── eventsourcing/
│   ├── eventstore/                 ← Dossier technique
│   ├── aggregate/                  ← Dossier DDD !
│   └── ...
├── modelling/
│   ├── command/                    ← Dossier DDD
│   ├── saga/                       ← Dossier DDD
│   └── ...
```

👉 **Mix DDD + Technique** (comme ton Option 2)

### Verdict : Même les grands frameworks mixent les approches !

---

## 🎯 Synthèse

| Question | Réponse |
|----------|---------|
| **Dois-je mixer dossiers DDD et techniques ?** | ✅ OUI, c'est une pratique courante |
| **Quelle option pour mon projet ?** | 🏆 **Option 2 (Hybride)** |
| **Temps de refactoring ?** | ⏱️ **2h** |
| **Impact sur le score DDD ?** | 📈 **+2 points** (72 → 74) |

**Conclusion** : Ajoute des dossiers DDD pour les **concepts stratégiques** (events, factories), mais garde la structure plate pour le reste (VOs, Entities). C'est le meilleur compromis clarté/pragmatisme.

---

*Document créé le 2025-12-17*

