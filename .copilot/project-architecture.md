# Architecture du Projet

Application Java Spring Boot suivant les principes **DDD** (Domain-Driven Design) avec éléments de **Clean Architecture** et **Architecture Hexagonale**.

## Design : DDD

### Aggregates
- **`Client`** (sealed class) : `Person`, `Company`
- **`Contract`**

Entités immuables avec pattern Builder, logique métier pure.

### Value Objects
Records Java immuables : `ClientEmail`, `ClientName`, `ClientPhoneNumber`, `ContractPeriod`, `ContractCost`, etc.

### Domain Events
- `ClientDeletedEvent` : déclenche la fermeture des contrats actifs (listener dans la couche Application)

## Architecture : Clean + Hexagonale

### Structure en Couches (Dependency Rule)

```
Web (Controllers, DTOs)
    ↓
Application (Use Cases, Orchestration)
    ↓
Domain (Aggregates, Value Objects, Ports)
    ↑
Infrastructure (JPA Entities, Assemblers, Repositories)
```

**Règle** : Les dépendances pointent **vers l'intérieur** (vers Domain)

> **Note** : Notre structure suit les principes Clean Architecture :
> - `domain/` + `application/` = équivalent à **Core** (logique métier)
> - `infrastructure/` + `web/` = équivalent à **Adapters** (détails techniques)

### Validation Automatique

Les règles architecturales sont **testées automatiquement** avec **ArchUnit** :
- `HexagonalArchitectureTest.java` : valide que Domain/Application ne dépendent pas de Infrastructure/Web
- Exécuté à chaque build Maven
- Échec du build si violation détectée

### Couches

#### **Domain** (`domain/`)
- Aggregates : `Person`, `Company`, `Contract`
- Value Objects : `ClientEmail`, `ContractPeriod`, etc.
- **Ports** (interfaces) : `ClientRepository`, `ContractRepository`
- Domain Services : `ClientValidationService`, `ContractValidationService`
- Domain Events : `ClientDeletedEvent`

**Aucune dépendance technique** (ni Spring, ni JPA)

**Validation** : Le domaine suit le principe "always-valid" - voir [always-valid-principle.md](always-valid-principle.md) pour les détails.

#### **Application** (`application/`)
- **Use Cases** (CQRS léger) :
  - Commands : `CreatePersonUseCase`, `UpdateClientUseCase`
  - Queries : `GetClientByIdQuery`, `GetActiveContractsQuery`
- Application Services : orchestration transactionnelle (`@Transactional`)
- Event Handlers : `ContractEventHandler`

#### **Infrastructure** (`infrastructure/`)
- **Entités JPA** : `PersonJpaEntity`, `CompanyJpaEntity`, `ContractJpaEntity` (mutables, annotations JPA)
- **Assemblers** : conversion Domain ↔ JPA (`PersonAssembler`, `ContractAssembler`)
- **Repositories JPA** : implémentent les ports du Domain

#### **Web** (`web/`)
- Controllers REST : `ClientController`, `ContractController`
- DTOs : requête/réponse (mapping avec MapStruct)
- Exception Handling centralisé (`@ControllerAdvice`)

## Bounded Context

**1 seul bounded context** : Contract Service

Contient 2 aggregates :
- `Client` (racine : Person ou Company)
- `Contract` (référence Client via `clientId`)

Les Domain Events permettent la cohérence entre aggregates sans couplage fort.

## Technologies

- Spring Boot 4.0, Spring Data JPA, PostgreSQL
- Flyway (migrations), MapStruct (mapping DTO ↔ Domain)
- JUnit 5, Mockito, RestAssured, Testcontainers

