# Contract Service - Insurance Client & Contract Management API

A RESTful API for managing insurance clients (persons and companies) and their contracts, built with Spring Boot and PostgreSQL.

---

## 🚀 Quick Start

### Prerequisites
- Docker Desktop ([Download](https://www.docker.com/products/docker-desktop/))
- The provided Docker image builds on `azul-zulu:27-jdk` and runs on a custom Java 27 `jlink` runtime

### Run with Docker Compose (Recommended)

```bash
git clone <repository-url>
cd contract-service
docker-compose up --build
```

✅ **That's it!** API available at `http://localhost:8080`

**First launch takes 2-3 minutes** (PostgreSQL setup + Spring Boot startup)

---

## 🏗️ Architecture & Design

**Vertical Slice + Clean Architecture + DDD.** 
The subject looks like CRUD, but its value is in the rules: what makes a contract active, closing contracts when a client is deleted, immutable fields, an internal update date. 
They live in rich, framework-free aggregates (`Client` sealed `Person | Company`, `Contract`, self-validating Value Objects), unit tested in isolation. 
Each use case is one thin file in `features/`; Spring MVC and JPA/Flyway adapters stay at the edges, and ArchUnit enforces the dependency direction. 
The domain decides, SQL does the heavy lifting: the sum is one aggregate query on a partial index, with no cache since a contract expires without any write, and a client's contracts are closed by a single bulk update. 
The active rule exists in Java and SQL; a test keeps both aligned. 
Deleting a client is a soft delete, so contract history is kept. 
Trade-off: more files than a layered CRUD, accepted for testability. Details in [docs/ADR.md](docs/ADR.md).

---

## ✅ Proof the API Works

### 1. Interactive Documentation (Swagger UI)
```
http://localhost:8080/swagger-ui.html
```
Try all endpoints live with auto-generated examples!

### 2. Quick Test Examples

Every `/v2` endpoint requires a Keycloak access token of a counselor ([ADR-017](docs/ADR.md#adr-017--authentification-des-conseillers-par-keycloak)). The local Keycloak (`http://localhost:8081`, admin console `admin`/`admin`) imports the `contract-service` realm from `keycloak/realm-contract-service.json` on first start, with two development-only users:

| User | Password | Role | Result |
|---|---|---|---|
| `counselor.demo` | `counselor` | `counselor` | full access |
| `visitor.demo` | `visitor` | none | `403 accessDenied` |

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/realms/contract-service/protocol/openid-connect/token \
  -d grant_type=password -d client_id=contract-service-local \
  -d username=counselor.demo -d password=counselor | sed -E 's/.*"access_token":"([^"]+)".*/\1/')
```

Without a token the API answers `401` (`code: unauthenticated`), without the counselor role `403` (`code: accessDenied`). Tokens last 5 minutes.

The realm is imported only when it does not exist yet: Keycloak keeps its data in the `keycloak_pgdata` volume, so changes to the JSON file are ignored once the realm exists. To re-import it, remove only that volume, then restart the application:

```bash
docker compose down
docker volume rm contract-service_keycloak_pgdata
```

Do not use `docker compose down -v`: it also removes `pgdata`, the application database.

**Create a Person Client:**
```bash
curl -H "Authorization: Bearer $TOKEN" -X POST http://localhost:8080/v2/clients \
  -H "Content-Type: application/json" \
  -d '{
    "type": "PERSON",
    "name": "Alice Martin",
    "email": "alice@example.com",
    "phone": "+41791234567",
    "birthDate": "1990-05-15"
  }'
```
✅ **Response:** `201 Created` + `Location: /v2/clients/{id}`

**Create a Company Client:**
```bash
curl -H "Authorization: Bearer $TOKEN" -X POST http://localhost:8080/v2/clients \
  -H "Content-Type: application/json" \
  -d '{
    "type": "COMPANY",
    "name": "TechCorp SA",
    "email": "contact@techcorp.ch",
    "phone": "+41221234567",
    "companyIdentifier": "CHE-123.456.789"
  }'
```
✅ **Response:** `201 Created`

**Create a Contract** (startDate defaults to now if not provided):
```bash
curl -H "Authorization: Bearer $TOKEN" -X POST http://localhost:8080/v2/contracts \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "{client-uuid}",
    "costAmount": 1500.00
  }'
```
✅ **Response:** `201 Created` (startDate = now(), endDate = null)

**Get Active Contracts** (paginated, only active):
```bash
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/v2/contracts?clientId={client-uuid}&page=0&size=20"
```
✅ **Response:** `200 OK` with pagination metadata (isFirst, isLast, totalElements)

**Filter by Update Date:**
```bash
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/v2/contracts?clientId={uuid}&updatedSince=2025-01-01T00:00:00"
```

**Get Sum of Active Contracts:**
```bash
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/v2/contracts/total?clientId={client-uuid}"
```
✅ **Response:** `4500.00` (SQL aggregation, always computed from the database)

**Update Contract Cost** (the internal last modification date is updated, the same amount is a no-op):
```bash
curl -H "Authorization: Bearer $TOKEN" -X PATCH "http://localhost:8080/v2/contracts/{contract-uuid}/cost" \
  -H "Content-Type: application/json" \
  -d '{"amount": 2000.00}'
```
✅ **Response:** `204 No Content`

**Delete Client** (soft delete, closes its active contracts):
```bash
curl -H "Authorization: Bearer $TOKEN" -X DELETE "http://localhost:8080/v2/clients/{client-uuid}"
```
✅ **Response:** `204 No Content` (active contracts get `endDate = deletion date`, the client then answers 404)

### 3. Automated Test Suite

```bash
./mvnw clean verify
```

Every requirement of the subject is covered by an acceptance test that calls the REST API over HTTP (RestAssured) against a real PostgreSQL started by Testcontainers. Time-dependent rules use a controllable clock to make time pass.

| Requirement | Acceptance test |
|---|---|
| Create a person / a company, validation, unique company identifier | `CreatePersonAcceptanceTest`, `CreateCompanyAcceptanceTest` |
| Read a client with all its fields | `ReadClientAcceptanceTest` |
| Update a client, `birthDate` and `companyIdentifier` unchanged | `UpdateClientAcceptanceTest`, `PatchClientAcceptanceTest` |
| Delete a client: active contracts closed at the deletion date | `ClientDeletionAcceptanceTest` |
| Create a contract (start date defaults to now, positive cost) | `CreateContractAcceptanceTest`, `ContractLifecycleAcceptanceTest` |
| Update the cost (updates the internal modification date) | `UpdateContractCostAcceptanceTest` |
| List active contracts, `updatedSince` filter, pagination | `ListContractsAcceptanceTest` |
| Sum of active contracts, including a contract that expires without any write | `ContractTotalUseCaseAcceptanceTest`, `ContractTotalOverTimeAcceptanceTest` |
| Same "active" rule in the domain and in SQL | `ActiveContractRuleAlignmentAcceptanceTest` |
| No lost update when a client is deleted during a cost update | `ContractClosureConcurrencyAcceptanceTest` |

Current result: 269 unit tests and 130 acceptance tests, 0 failure.

Also run by the build:
- **Unit tests** of domain objects and use case handlers (mocked ports, fixed clock);
- **`ArchitectureTest`** (ArchUnit): dependency rules between layers;
- **JaCoCo**: the build fails below 70% line and branch coverage on domain and features, currently 94% (report: `target/site/jacoco/index.html`).

Mutation testing (separate profile and CI job, ADR-016):
- **PIT** mutates the bytecode of domain and features and checks that the unit tests detect each mutant: `./mvnw test -Pmutation` (report: `target/pit-reports/index.html`). Current score: 97% test strength (killed among mutants covered by unit tests, minimum enforced: 95%) and 96% of all mutants killed (minimum enforced: 90%). The remaining mutants are equivalent mutants or plumbing covered by acceptance tests, documented in ADR-016.

### 4. Postman Collections

Import from `api-collections/` for complete API examples:
- `ClientController.postman_collection.json`
- `ContractController.postman_collection.json`

---

## 📚 API Endpoints Summary

### Clients
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v2/clients` | Create client (Person or Company, discriminator: `type`) |
| GET | `/v2/clients/{id}` | Get client details (all fields) |
| PUT | `/v2/clients/{id}` | Update client (name, email, phone - NOT birthDate/companyIdentifier) |
| PATCH | `/v2/clients/{id}` | Partial update (name, email, phone) |
| DELETE | `/v2/clients/{id}` | Soft delete client & close its active contracts |

### Contracts
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v2/contracts` | Create contract (`clientId` in the body, startDate defaults to now) |
| GET | `/v2/contracts?clientId={id}` | Get active contracts (paginated, filter by updatedSince) |
| GET | `/v2/contracts/{id}` | Get specific contract |
| PATCH | `/v2/contracts/{id}/cost` | Update cost (updates the internal last modification date) |
| GET | `/v2/contracts/total?clientId={id}` | Sum of active contracts |

---

## 📋 Requirements Checklist (Subject Compliance)

✅ **Create client** (Person with birthDate, Company with identifier)  
✅ **Read client** (all fields returned)  
✅ **Update client** (except birthDate/companyIdentifier - immutable)  
✅ **Delete client** (soft delete, closes active contracts: endDate = deletion date)  
✅ **Create contract** (startDate defaults to now, endDate defaults to null)  
✅ **Update cost** (lastModified auto-updated, NOT exposed in API)  
✅ **Get active contracts** (current date < endDate, filter by updatedSince)  
✅ **Optimized sum endpoint** (database aggregation, no cache)  
✅ **ISO 8601 dates** (e.g., `2025-01-01T00:00:00`)  
✅ **Validation** (emails, phone numbers, dates, positive amounts)  
✅ **RESTful + JSON** (proper HTTP codes, Content-Type)  
✅ **Java 27 + Spring Boot 4.1** (Java 27 is not an LTS release)  
✅ **PostgreSQL persistence** (survives crashes/restarts)  
✅ **Descriptive code** (DDD, clear naming, minimal comments)  

---

## 🧪 Testing

```bash
# Run all tests
./mvnw verify

# Generate coverage report
./mvnw jacoco:report
# Open: target/site/jacoco/index.html

# Run mutation tests (PIT)
./mvnw test -Pmutation
# Open: target/pit-reports/index.html
```

**Test Statistics:**
- 94% line and branch coverage on domain and features (minimum enforced: 70%)
- Unit and acceptance tests
- Testcontainers for real database

---

## 🔧 Alternative: Run with JAR

**Prerequisites:** Java 27 + PostgreSQL 18 (`uuidv7()` is required by the migrations)

```bash
# 1. Create database
psql -U postgres -c "CREATE DATABASE contract;"

# 2. Run JAR
java -jar contract-service-2.0.0.jar
```

Flyway automatically creates schema + tables.

---

## 🛠️ Build from Source

```bash
./mvnw clean package
# JAR: target/contract-service-2.0.0.jar
```

---

## 🌍 Localization (i18n)

The API resolves the locale from the `Accept-Language` header:
- `fr-CH` (French Swiss - default)
- any other locale sent in `Accept-Language`

The `Content-Language` response header echoes the resolved locale. Error messages are not translated.

---

## 📊 Performance Optimizations

- **Sum Endpoint**: Native SQL aggregation (no object loading)
- **Pagination**: Efficient offset/limit queries
- **No cache on the sum**: the active set changes with time (a contract expires without any write), so the sum is always computed from the database
- **Indexes**: On clientId, end date (partial covering index for the sum), last modified date, companyIdentifier
- **UUID v7 identifiers**: time-ordered, so inserts append to the primary key index

---

## 🔐 Security Notes

- Every `/v2` endpoint requires a JWT issued by Keycloak for the `contract-service` audience, with the realm role `counselor`; health, info and the OpenAPI documentation are public, every other route is denied ([ADR-017](docs/ADR.md#adr-017--authentification-des-conseillers-par-keycloak))
- Keycloak settings: `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_JWK_SET_URI`, `KEYCLOAK_AUDIENCE`; CORS is closed unless `APP_CORS_ALLOWED_ORIGINS` lists the allowed origins
- Default credentials (`postgres`/`postgres`, Keycloak `admin`/`admin`, demo users) are for **development only**
- Docker build uses **Azul Zulu 27** and runtime uses a **custom Java 27 `jlink` image**
- Docker image runs as non-root user `appuser`
- Use environment variables for production credentials

---

## 📝 Key Technical Details

- **Update Date (lastModified)**: Managed internally, **NOT exposed** in API responses
- **Active Contracts**: `endDate == null` OR `now < endDate` (a contract starting in the future is active)
- **Soft Delete**: Client deletion sets `deleted_at`, the client answers 404 everywhere, and its active contracts are closed with `endDate = deleted_at`. Contracts are kept for history.
- **Immutable Fields**: `birthDate` (Person) and `companyIdentifier` (Company)
- **Value Objects**: Self-validating (ClientEmail, ClientPhoneNumber, ContractCost, ContractPeriod)
- **Sealed Hierarchy**: Client → Person | Company (compile-time type safety)

---

## 🎯 Project Structure

```
src/main/java/com/mk/contractservice/
├── domain/          # Aggregates, Value Objects, domain services, repository ports, domain exceptions
├── features/        # One use case per file (vertical slice)
├── controllers/     # REST endpoints, DTOs, DTO mappers, exception handlers
└── infrastructure/  # JPA entities, repositories and mappers, configuration

src/test/java/com/mk/contractservice/
├── domain/          # Unit tests of domain objects
├── features/        # Unit tests of use case handlers
├── acceptance/      # REST API end to end (*AcceptanceTest, Testcontainers)
└── ArchitectureTest # Dependency rules (ArchUnit)
```

Dependencies point inward: `controllers → features → domain ← infrastructure`.

---

## 📚 Documentation

- **[docs/ADR.md](docs/ADR.md)** - Architecture Decision Records (reference)
- **[docs/sujet.txt](docs/sujet.txt)** - Functional requirements
- **[api-collections/](api-collections/)** - Postman collections

---

## 📄 License

This project is a technical exercise for API Factory.

---

## 👨‍💻 Author

Developed as part of a technical assessment demonstrating:
- Domain-Driven Design (DDD)
- Clean Architecture
- Performance optimization
- Comprehensive testing
- Production-ready code quality

