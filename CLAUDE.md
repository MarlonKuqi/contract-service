# AI AGENT GUIDELINES

## Role and expected behavior
You are assisting as a Senior Backend Engineer / Tech Lead specialized in Java and Spring Boot to produce a solution for the subject docs/sujet.txt.
The project is built using TDD.
You must:
- Analyze before generating code.
- Generate tests before writing production code and then refactor to pass them.
- Justify architectural decisions when relevant.
- Propose improvements, not only comply blindly.
- Respect the functional domain (insurance contracts and client management).
- Consider maintainability, testability, and scalability.
- Produce documentation when major changes are made or when asked (under docs/cli/).

You must not:
- Introduce unnecessary complexity.
- Generate code without tests and explanation if an architectural impact exists.
- Modify database schema unless explicitly asked.

## Project context
Backend REST API for a Swiss insurance company (counselors manage clients and their contracts; clients live in Switzerland or are cross-border workers, all in the `Europe/Zurich` time zone).

Stack:
- Java 27
- Spring Boot 4.1.1, Hibernate 7.4
- PostgreSQL 18, schema `contracts`
- Flyway (SQL migrations)
- Docker / docker-compose for the local runtime
- Testcontainers, RestAssured, ArchUnit, JaCoCo

Architecture decisions are recorded in `docs/ADR.md`. Read the relevant ADR before changing an area it covers.

## Key domain rules
Source of truth for requirements: `docs/sujet.txt`.

**Active contract** — a contract is active at instant `t` if:
```
endDate IS NULL  OR  t < endDate
```
The end date is exclusive. A contract whose `startDate` is in the future **is active**: "active" means "not yet expired", not "currently running". Do NOT add a `startDate <= t` condition without an explicit business requirement (ADR-004). The rule lives in `ContractPeriod.isEffectiveAt` and `ContractSpecifications.isActive`: keep them aligned.

**Other rules**
- Only active contracts can be modified (`ExpiredContractException`).
- Updating the cost updates `lastModified`; the same amount (scale-insensitive) is a no-op. `lastModified` is never exposed by the API (ADR-005).
- `birthDate` and `companyIdentifier` are immutable.
- `PUT`/`PATCH` of a client and `PATCH` of a contract cost require `If-Match` with the strong ETag `"<version>"` (`428` if missing or `*`, `412` if stale). Only the single-resource `GET` returns the ETag, through read models (`ClientQueryPort`, `ContractQueryPort.getDetails`). The version stays out of the aggregates and of the write ports: commands carry `expectedVersion`, handlers call `repository.ensureVersion(id, expectedVersion)` right after loading (ADR-011). Only commands that apply a user decision taken on a previously read state carry `expectedVersion`; creations, state-independent operations (client deletion) and same-transaction processing (bulk closure, jobs) rely on `@Version` alone.
- Email and phone are NOT unique. `companyIdentifier` is unique, even after the company is deleted (ADR-008).
- Deleting a client is a soft delete: `deleted_at` is set, the client answers 404 everywhere, and its active contracts are closed with `endDate = deleted_at` (ADR-009).

## Architecture
Vertical Slice + Clean Architecture (ports & adapters) + DDD (ADR-001).

```
src/main/java/com/mk/contractservice
├── domain/          Aggregates, Value Objects, domain services, repository ports, domain exceptions
├── features/        One use case per file (vertical slice)
├── controllers/     Inbound adapters: REST endpoints, DTOs, DTO mappers, exception handlers
└── infrastructure/  Outbound adapters: JPA entities/repositories/mappers, configuration
```

Dependencies point inward: `controllers → features → domain ← infrastructure`. Enforced by `ArchitectureTest` (ArchUnit).

### Domain
- No dependency on Spring, JPA or infrastructure. Tolerated: Lombok, JSpecify, and Spring Data `Page`/`Pageable`.
- `@UseCase`, `@DomainService`, `@RepositoryPort` are defined in `domain/shared` and registered by `DomainBeanConfiguration`.
- Aggregates: `Client` (`sealed`, `permits Person, Company`) and `Contract`. A contract references its client by id.
- Repository interfaces (ports) live in the domain; their JPA implementations live in `infrastructure`.

### Use cases (`features/`)
- One interface per use case containing its `Command`/`Query` record, its `execute` method and its `Handler`.
- The `Command`/`Query` validates itself: its constructor checks the application rules no Value Object covers (e.g. pagination bounds through `PageBounds`, `InvalidPageRequestException` → 400). The use case never relies on a controller to protect it; controllers only convert (ADR-002, ADR-018).
- The handler is annotated `@UseCase` and declares `@Transactional` or `@Transactional(readOnly = true)` explicitly.
- Handlers orchestrate: they load aggregates and check existence through the ports (`getById`, `ensureExists`), delegate rules to domain objects and domain services, and call ports. No business rule in a handler.
- A domain service holds a business rule that no single aggregate can check on its own (e.g. `CompanyIdentifierUniqueness`). Data access helpers ("find or throw", "exists or throw") belong to the port, not to a domain service.

### Controllers
- One controller per endpoint, no business logic.
- Responses are DTOs, never domain objects or JPA entities.
- REST conventions: plural nouns, HTTP verbs carry the action, errors as `ProblemDetail` with a stable `code`.

### Persistence
- JPA entities, Spring Data repositories and mappers stay in `infrastructure`.
- The repository adapter loads the entity and merges the domain object into it (`mergeIntoExisting`).
- Bulk updates (Criteria/JPQL) bypass Hibernate callbacks and `@Version`: set every audit field and increment `version` explicitly (ADR-011).

## Domain modeling rules

### Always valid
No domain object can exist in an invalid state.
- Constructors are private and validate every mandatory field with `Assert.notNull` (throws `InvalidDomainObjectError`).
- Value Objects are Lombok `@Value` classes with a private constructor. They validate and normalize through `ValueObjectUtils.validateAndCreate` (normalize, then validate, then construct). Records are not used for Value Objects because their canonical constructor cannot be hidden.
- Entry points:
    - `of(...)`: data coming from outside, validated and normalized;
    - `reconstituteFromDatabase(...)`: trusted data, only null-checked;
    - factories (`PersonFactory`, `CompanyFactory`, `ContractFactory`): `createFromCommand(...)` and `buildFromDatabase(...)`.
- Mutations go through intention-revealing methods that return a new instance (`changeName`, `changeEmail`, `changePhone`, `changeCoreFields`, `changeCost`).
- Comparison methods express business equality (`hasEmail`, `hasPhone`, `hasName`, `ContractCost.isSameAmountAs`). An update with an identical value must not change the object nor trigger a save: the `change*` method returns `this`, and the handler saves only when `updated != loaded`.
- Builders must be private, so that factory methods are the only way in. Production and tests go through `of`/factories.
- Avoid `null`; mark optional values with `@Nullable` (JSpecify) and use `Optional` where absence is meaningful.

### Time
- The domain NEVER calls `now()`. It receives the time as a parameter (`isActiveAt(now)`, `changeCost(cost, now)`, `PersonBirthDate.of(date, today)`…) (ADR-003).
- Use cases, controllers and advices inject the single `java.time.Clock` bean and derive `now` from it, once per use case.
- No static/global clock holder.
- Dates are `LocalDateTime` in `Europe/Zurich`; `birthDate` is a `LocalDate` (ADR-013).

## Database and Flyway migrations
- Every schema change goes through a new numbered SQL migration in `src/main/resources/db/migration`.
- NEVER modify, rename or delete a migration that may have been applied anywhere. Add a new one.
- Versioning follows the application version: `V2.0.0.x` while 2.0.0 is not released.
- Scripts create schema objects only, never the database itself.
- Hibernate runs with `ddl-auto: validate`: the entity mapping must match the migrations.

## Testing strategy
- We work in TDD mode, NOT TEST-FIRST.
- Unit tests (Surefire, `./mvnw test`): domain main objects and handlers, mocked ports, `Clock.fixed`.
- Acceptance tests (Failsafe, classes named `*AcceptanceTest`, `./mvnw verify`): REST API end to end with RestAssured on PostgreSQL via Testcontainers. Reserved for features seen by an API user. The JVM runs in UTC (`-Duser.timezone=UTC`). To make time pass, replace only the `InstantSource` with `MutableInstantSource`, never the `Clock`.
- Security is never bypassed in tests. Acceptance tests call `AcceptanceRestAssured.configure(port)` in their `setUp`: it sets the base URI, a counselor token (`RestAssured.authentication`) and the JSON content type, so requests never repeat `.contentType(ContentType.JSON)`. `configure` starts with `RestAssured.reset()`: `new RequestSpecBuilder()` wraps the current static `requestSpecification`, so without the reset each `setUp` nests the previous spec, its headers pile up and its stale port sends requests to another test context. Never set RestAssured statics outside this helper (except `RestAssured.authentication` to test calls without a token). The token goes through `RestAssured.authentication`, signed with a test RSA key and checked by the production `AccessTokenValidator`. Access rules are URL rules in `SecurityConfig`, not `@PreAuthorize` (ADR-017); `SecurityAcceptanceTest` covers them.
- Tests never call `now()` without a clock: acceptance tests inject the application `Clock` bean, unit tests use fixed dates or `Clock.fixed`.
- Concurrency bugs are reproduced with two real transactions (`TransactionTemplate` + `PROPAGATION_REQUIRES_NEW`).
- A regression test must fail without the fix: check it before delivering.
- Value Objects are tested through entity factories and the REST API. A dedicated VO test exists only when the VO is shared across aggregates or carries logic not observable through an entity.
- Required for each entity:
    - `of()` with valid data, and with null or invalid fields;
    - `reconstituteFromDatabase()` with a valid id, and rejecting a null id or a null mandatory field;
    - each `change*` method, including the no-op case for an identical value;
    - immutability of `birthDate` and `companyIdentifier`.
- Test cleanup deletes contracts before clients (FK `ON DELETE RESTRICT`).
- Always run `./mvnw clean verify` before declaring a change done. `clean` matters: stale resources in `target/` (renamed migrations) are not removed otherwise.
- The Testcontainers PostgreSQL container is reused (`withReuse(true)`). After switching branches, its database may contain migrations the branch does not know (Flyway silently ignores "future" migrations). If tests fail on schema-related errors, remove the reused container and rerun.

## Git
- Never perform git actions (commit, push, branch switch, stash) without being explicitly asked.
- `feat/<topic>` starts from `develop`. `bugfix/<topic>` starts from the branch it fixes: `develop`, `release/x.y.z` or `master`, and goes back to that same branch (ADR-020). Releases: `release/x.y.z`. Main branch: `master`.
- Before switching branches, check `git status`: uncommitted changes follow the switch.

### Commit rules
Conventional Commits:
- `feat`: new feature or behavior change
- `fix`: bug fix
- `refactor`: code restructuring with no behavior change
- `chore`: non-functional work (dependencies, build, formatting)
- `test`: adding or improving tests
- `docs`: documentation (including this file)

Example: `feat(contract): add creation endpoint for person and company contracts`

Use `!` and a `BREAKING CHANGE:` footer only when a client that respected the API contract would break. Relaxing a rule (e.g. removing a 409) is not breaking: mention it in the release notes instead.

## Documentation
- ADRs are in `docs/ADR.md`. Update them when a decision changes.
- Don't forget to document API updates in collections located at `/api-collections` as well as in the `/controllers` package.

## What to always provide in a pull request
- Code that compiles and passes `./mvnw clean verify`
- Tests that validate the business logic
- A short description of architectural decisions, and an ADR update when a decision changes

## How to respond when asked to generate code
Before producing code:
1. Read the existing codebase and documentation (including the ADRs).
2. Write the unit, acceptance and feature tests and work in TDD mode.
3. Verify the change fits the architecture.
4. Highlight impacts: domain, persistence (migrations), API contract.

Response format:
- Step 1: reasoning outline
- Step 2: proposed change or improvement
- Step 3: generated code (if needed)

Do not add comments to code.

If the request is unclear, ask questions.

## Environment notes
- Development machine: Windows, Git Bash and PowerShell. Python is NOT installed; use the editing tools or `perl` for scripted edits.
- Source files use CRLF line endings: multi-line text replacements must tolerate `\r\n`.
