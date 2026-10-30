# Refactoring : Séparation Domaine / Infrastructure

## 🎯 Objectif
Éliminer le couplage entre le domaine métier et l'infrastructure JPA en suivant les principes de l'architecture hexagonale/clean architecture.

## ✅ Ce qui a été fait

### 1. **Nettoyage du Domaine** (Couche Pure)
Suppression de toutes les annotations JPA des classes du domaine :

#### Entités Domaine
- ✅ `Client.java` - Plus d'annotations `@Entity`, `@Table`, `@Inheritance`, etc.
- ✅ `Person.java` - Plus d'annotations `@Entity`, `@DiscriminatorValue`, etc.
- ✅ `Company.java` - Plus d'annotations `@Entity`, `@DiscriminatorValue`, etc.
- ✅ `Contract.java` - Plus d'annotations `@Entity`, `@ManyToOne`, `@PrePersist`, etc.

#### Value Objects
- ✅ `Email.java` - Plus d'`@Embeddable`, `@Column`
- ✅ `ClientName.java` - Plus d'`@Embeddable`, `@Column`
- ✅ `PhoneNumber.java` - Plus d'`@Embeddable`, `@Column`
- ✅ `CompanyIdentifier.java` - Plus d'`@Embeddable`, `@Column`
- ✅ `PersonBirthDate.java` - Plus d'`@Embeddable`, `@Column`
- ✅ `ContractPeriod.java` - Plus d'`@Embeddable`, `@Column`
- ✅ `ContractCost.java` - Plus d'`@Embeddable`, `@Column`

**Résultat** : Le domaine est maintenant pur, sans aucune dépendance technique !

### 2. **Création des Entités JPA** (Infrastructure)
Nouvelles classes dans `infrastructure/persistence/entity/` :

- ✅ `ClientJpaEntity.java` - Entité JPA pour les clients
- ✅ `PersonJpaEntity.java` - Entité JPA pour les personnes
- ✅ `CompanyJpaEntity.java` - Entité JPA pour les entreprises
- ✅ `ContractJpaEntity.java` - Entité JPA pour les contrats

Ces entités contiennent toutes les annotations JPA et représentent la structure de la base de données.

### 3. **Création des Mappers** (Traduction Domaine ↔ Infrastructure)
Nouvelles classes dans `infrastructure/persistence/mapper/` :

- ✅ `ClientMapper.java` - Conversion Client ↔ ClientJpaEntity
- ✅ `ContractMapper.java` - Conversion Contract ↔ ContractJpaEntity

Les mappers assurent la traduction bidirectionnelle entre :
- **Domaine** : Objets métier purs avec Value Objects
- **Infrastructure** : Entités JPA avec types primitifs

### 4. **Mise à jour des Repositories**
Modification des repositories pour utiliser les mappers :

- ✅ `ClientJpaRepository.java` - Travaille maintenant avec `ClientJpaEntity`
- ✅ `JpaClientRepository.java` - Utilise `ClientMapper` pour la conversion
- ✅ `ContractJpaRepository.java` - Travaille maintenant avec `ContractJpaEntity`
- ✅ `JpaContractRepository.java` - Utilise `ContractMapper` pour la conversion

## 🏗️ Architecture Résultante

```
┌─────────────────────────────────────────┐
│          COUCHE WEB (API)               │
│   - Controllers                         │
│   - DTOs Request/Response               │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│      COUCHE APPLICATION                 │
│   - Services Application                │
│   - Orchestration                       │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│      COUCHE DOMAINE (PURE) ✨           │
│   - Client, Person, Company             │
│   - Contract                            │
│   - Value Objects (Email, Phone, etc.)  │
│   - Repository Interfaces               │
│   - Logique Métier                      │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│   COUCHE INFRASTRUCTURE                 │
│                                         │
│   Persistence:                          │
│   - ClientJpaEntity, ContractJpaEntity  │
│   - ClientMapper, ContractMapper        │
│   - JpaClientRepository                 │
│   - ClientJpaRepository (Spring Data)   │
└─────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│         BASE DE DONNÉES                 │
└─────────────────────────────────────────┘
```

## 💡 Avantages de cette Architecture

### 1. **Domaine Pur et Indépendant**
- ✅ Aucune dépendance technique (JPA, Spring, etc.)
- ✅ Testable sans framework
- ✅ Portable vers un autre framework de persistence

### 2. **Séparation des Responsabilités**
- ✅ Le domaine exprime le métier
- ✅ L'infrastructure gère la technique
- ✅ Les mappers font la traduction

### 3. **Flexibilité**
- ✅ Changement de BDD plus facile
- ✅ Changement de framework de persistence possible
- ✅ Tests unitaires plus simples

### 4. **Maintenabilité**
- ✅ Code plus clair et lisible
- ✅ Responsabilités bien définies
- ✅ Moins de couplage

## 📝 Pattern Utilisé

Cette architecture suit le pattern **Repository avec Mapper** (variante du pattern DAO) :

```
Domaine (Client) 
    ↕ 
Mapper (ClientMapper)
    ↕
Infrastructure (ClientJpaEntity)
    ↕
Base de Données
```

C'est une approche classique et propre, similaire à ce que vous avez vu dans des projets legacy avec DTO/DAO, mais modernisée avec :
- Value Objects immuables
- Architecture hexagonale
- Domain-Driven Design (DDD)

## 🔄 Prochaines Étapes (Optionnel)

1. **Tests** : Vérifier que tout fonctionne avec les tests existants
2. **Documentation** : Documenter les choix d'architecture
3. **Migration de données** : Si nécessaire, adapter les scripts de migration Flyway/Liquibase

## 📚 Références

- **Architecture Hexagonale** (Ports & Adapters) - Alistair Cockburn
- **Clean Architecture** - Robert C. Martin
- **Domain-Driven Design** - Eric Evans

