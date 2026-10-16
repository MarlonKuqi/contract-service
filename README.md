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

**Vertical Slice + Clean Architecture with DDD Principles**

This project combines **Vertical Slice Architecture** (feature-driven organization) with **Clean Architecture** (onion
layers) following **Domain-Driven Design**. Each feature (e.g., CreateClient, UpdateContract) is isolated end-to-end
across all layers—domain logic → application orchestration → infrastructure persistence → REST API. This decomposition
enables **independent testing of each feature, clear separation of concerns, easy evolution, and long-term
maintainability**.

**Domain Layer** (innermost) enforces invariants via Aggregates (Client sealed hierarchy: Person | Company, Contract)
and immutable self-validating Value Objects (Email, PhoneNumber, ContractCost, ContractPeriod). **Validation at creation ensures "Always Valid" state**: business rules are encoded at instantiation (email format, positive cost, endDate > startDate), and immutability prevents subsequent invalid mutations. Once created, objects are guaranteed to satisfy all invariants—there is no intermediate invalid state. Domain exceptions (InvalidClientEmailException, ExpiredContractException) communicate failures clearly.

**Features** (`features/`) hold one use case per file: a transactional handler that loads aggregates through repository ports, delegates business rules to the domain and saves the result.

**Infrastructure Layer** abstracts technical concerns: JPA entities and Spring Data repositories implementing the domain ports, mappers for bidirectional conversion (JPA ↔ domain), PostgreSQL + Flyway versioned migrations.

**Controllers** (`controllers/`) expose one REST endpoint each with DTO validation (Jakarta Validation), specialized exception handlers returning
`ProblemDetail` errors (400, 404, 409, 422).

This architecture ensures business logic remains independent of frameworks, databases, and delivery mechanisms ;
maximizing code reusability, testability, and resilience to technology changes.

---

## ✅ Proof the API Works

### 1. Interactive Documentation (Swagger UI)
```
http://localhost:8080/swagger-ui.html
```
Try all endpoints live with auto-generated examples!

### 2. Quick Test Examples

**Create a Person Client:**
```bash
curl -X POST http://localhost:8080/v2/clients \
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
curl -X POST http://localhost:8080/v2/clients \
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
curl -X POST http://localhost:8080/v2/contracts \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "{client-uuid}",
    "costAmount": 1500.00
  }'
```
✅ **Response:** `201 Created` (startDate = now(), endDate = null)

**Get Active Contracts** (paginated, only active):
```bash
curl "http://localhost:8080/v2/contracts?clientId={client-uuid}&page=0&size=20"
```
✅ **Response:** `200 OK` with pagination metadata (isFirst, isLast, totalElements)

**Filter by Update Date:**
```bash
curl "http://localhost:8080/v2/contracts?clientId={uuid}&updatedSince=2025-01-01T00:00:00"
```

**Get Sum of Active Contracts:**
```bash
curl "http://localhost:8080/v2/contracts/total?clientId={client-uuid}"
```
✅ **Response:** `4500.00` (database aggregation, < 100 ms for 100 contracts in the acceptance tests)

**Update Contract Cost** (the internal last modification date is updated, the same amount is a no-op):
```bash
curl -X PATCH "http://localhost:8080/v2/contracts/{contract-uuid}/cost" \
  -H "Content-Type: application/json" \
  -d '{"amount": 2000.00}'
```
✅ **Response:** `204 No Content`

**Delete Client** (soft delete, closes its active contracts):
```bash
curl -X DELETE "http://localhost:8080/v2/clients/{client-uuid}"
```
✅ **Response:** `204 No Content` (active contracts get `endDate = deletion date`, the client then answers 404)

### 3. Automated Test Suite (Proof of Quality)

Run all tests:
```bash
./mvnw verify
```

**Coverage:** 80%+ (view report: `target/site/jacoco/index.html`)

**Test Categories:**
- ✅ **Unit Tests** (domain objects and use case handlers, mocked ports, fixed clock)
- ✅ **Acceptance Tests** (REST API end to end with RestAssured, real PostgreSQL via Testcontainers)
- ✅ **Validation Tests** (email, phone, dates ISO-8601, business rules)
- ✅ **Edge Cases** (empty lists, expired contracts, conflicts)
- ✅ **Testcontainers** (real PostgreSQL database)

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
```

**Test Statistics:**
- 80%+ code coverage
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

- Default credentials (`postgres`/`postgres`) are for **development only**
- Docker build uses **Azul Zulu 27** and runtime uses a **custom Java 27 `jlink` image**
- Docker image runs as non-root user `appuser`
- Use environment variables for production credentials
- Dependencies scanned for vulnerabilities (OWASP)

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

