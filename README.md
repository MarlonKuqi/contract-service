# Contract Service - Insurance Client & Contract Management API

A RESTful API for managing insurance clients (persons and companies) and their contracts, built with Spring Boot and PostgreSQL.

---

## 🚀 Quick Start

### Prerequisites
- Docker Desktop ([Download](https://www.docker.com/products/docker-desktop/))

### Run with Docker Compose (Recommended)

```bash
git clone <repository-url>
cd contract-service
docker-compose up --build
```

✅ **That's it!** API available at `http://localhost:8080`

**First launch takes 2-3 minutes** (PostgreSQL setup + Spring Boot startup)

---

## 🏗️ Architecture & Design (1000 chars max - as required)

This project follows **Domain-Driven Design (DDD)** with clean separation:

**Domain Layer**: Aggregates (Client with Person/Company sealed hierarchy, Contract) enforce business invariants. Value Objects (Email, PhoneNumber, ContractCost, ContractPeriod) are immutable and self-validating, ensuring rules at creation time (clientEmail format, positive cost, endDate > startDate). Domain exceptions provide clear business errors (InvalidEmailException, ExpiredContractException).

**Application Layer**: Services orchestrate domain logic within transaction boundaries (@Transactional). Caching strategy (@Cacheable) optimizes frequent queries.

**Infrastructure**: JPA repositories with optimized fetch strategies (LAZY default, EAGER with join fetch to avoid N+1 queries). Hibernate unproxy handles lazy proxies. Assemblers convert JPA entities ↔ domain objects. PostgreSQL + Flyway for versioned migrations.

**Web Layer**: REST controllers expose clean endpoints. DTOs with Jakarta Validation (@NotBlank, @Email, @Positive). Specialized ControllerAdvice returns proper HTTP codes (422 validation, 404 not found, 409 conflict, 403 forbidden). OpenAPI documentation.

**Performance**: Sum endpoint uses native SQL aggregation (<100ms for 1000+ contracts).  
**Quality**: 80%+ test coverage with Testcontainers integration tests.

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
    "clientEmail": "alice@example.com",
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
    "clientEmail": "contact@techcorp.ch",
    "phone": "+41221234567",
    "companyIdentifier": "CHE-123.456.789"
  }'
```
✅ **Response:** `201 Created`

**Create a Contract** (startDate defaults to now if not provided):
```bash
curl -X POST "http://localhost:8080/v2/contracts?clientId={client-uuid}" \
  -H "Content-Type: application/json" \
  -d '{
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

**Get Sum of Active Contracts** (Optimized - <100ms):
```bash
curl "http://localhost:8080/v1/contracts/sum?clientId={client-uuid}"
```
✅ **Response:** `{"sum": 4500.00}` (< 100ms even with 1000+ contracts)

**Update Contract Cost** (lastModified auto-updated):
```bash
curl -X PATCH "http://localhost:8080/v1/contracts/{contract-uuid}/cost?clientId={client-uuid}" \
  -H "Content-Type: application/json" \
  -d '{"amount": 2000.00}'
```
✅ **Response:** `204 No Content`

**Delete Client** (auto-closes all active contracts):
```bash
curl -X DELETE "http://localhost:8080/v1/clients/{client-uuid}"
```
✅ **Response:** `204 No Content` (all contracts endDate set to now())

### 3. Automated Test Suite (Proof of Quality)

Run all tests:
```bash
./mvnw verify
```

**Coverage:** 80%+ (view report: `target/site/jacoco/index.html`)

**Test Categories:**
- ✅ **Integration Tests** (9 pagination tests, lifecycle tests, CRUD)
- ✅ **Performance Tests** (sum < 100ms for 1000 contracts)
- ✅ **Validation Tests** (clientEmail, phone, dates ISO-8601, business rules)
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
| POST | `/v1/clients` | Create client (Person or Company, discriminator: `type`) |
| GET | `/v1/clients/{id}` | Get client details (all fields) |
| PUT | `/v1/clients/{id}` | Update client (name, clientEmail, phone - NOT birthDate/companyIdentifier) |
| DELETE | `/v1/clients/{id}` | Delete client & auto-close all contracts |

### Contract Endpoints (v2)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v2/contracts?clientId={id}` | Create contract (startDate defaults to now) |
| GET | `/v2/contracts?clientId={id}` | Get active contracts (paginated, filter by updatedSince) |
| GET | `/v2/contracts/{id}?clientId={id}` | Get specific contract |
| PATCH | `/v2/contracts/{id}/cost?clientId={id}` | Update cost (auto-updates lastModified) |
| GET | `/v2/contracts/sum?clientId={id}` | Sum of active contracts (optimized) |

---

## 📋 Requirements Checklist (Subject Compliance)

✅ **Create client** (Person with birthDate, Company with identifier)  
✅ **Read client** (all fields returned)  
✅ **Update client** (except birthDate/companyIdentifier - immutable)  
✅ **Delete client** (auto-closes contracts: endDate = now())  
✅ **Create contract** (startDate defaults to now, endDate defaults to null)  
✅ **Update cost** (lastModified auto-updated, NOT exposed in API)  
✅ **Get active contracts** (current date < endDate, filter by updatedSince)  
✅ **Optimized sum endpoint** (<100ms for 1000+ contracts)  
✅ **ISO 8601 dates** (e.g., `2025-01-01T00:00:00`)  
✅ **Validation** (emails, phone numbers, dates, positive amounts)  
✅ **RESTful + JSON** (proper HTTP codes, Content-Type)  
✅ **Java 21 + Spring Boot 3**  
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
- 100+ integration tests
- Testcontainers for real database
- Performance benchmarks

---

## 🔧 Alternative: Run with JAR

**Prerequisites:** Java 21 + PostgreSQL 12+

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

API supports multiple languages via `Accept-Language` header:
- `fr-CH` (French Swiss - default)
- `de-CH` (German Swiss)
- `it-CH` (Italian Swiss)
- `en` (English)

All error messages and `Content-Language` headers adapt automatically.

---

## 📊 Performance Optimizations

- **Sum Endpoint**: Native SQL aggregation (no object loading)
- **Pagination**: Efficient offset/limit queries
- **Lazy Loading**: Default for associations (EAGER only where needed)
- **Caching**: Caffeine cache on sum queries (`@Cacheable`)
  - ⚠️ **Single-instance only** (suitable for demo/evaluation)
  - For multi-instance production → Migrate to Redis (see [docs-claude/CAFFEINE_CACHE_MULTI_INSTANCE_PROBLEM.md](docs-claude/CAFFEINE_CACHE_MULTI_INSTANCE_PROBLEM.md))
- **Indexes**: On clientId, dates, clientEmail, companyIdentifier

---

## 🔐 Security Notes

- Default credentials (`postgres`/`postgres`) are for **development only**
- Docker image runs as non-root user `appuser`
- Use environment variables for production credentials
- Dependencies scanned for vulnerabilities (OWASP)

---

## 📝 Key Technical Details

- **Update Date (lastModified)**: Managed internally, **NOT exposed** in API responses
- **Active Contracts**: `endDate == null` OR `endDate > now()`
- **Soft Delete**: Client deletion sets contract `endDate = now()` (no hard delete)
- **Immutable Fields**: `birthDate` (Person) and `companyIdentifier` (Company)
- **Value Objects**: Self-validating (Email, PhoneNumber, ContractCost, ContractPeriod)
- **Sealed Hierarchy**: Client → Person | Company (compile-time type safety)

---

## 🎯 Project Structure

```
src/main/java/com/mk/contractservice/
├── domain/              # Business logic (DDD)
│   ├── client/         # Client aggregate (Person, Company)
│   ├── contract/       # Contract aggregate
│   ├── valueobject/    # Email, Phone, Cost, Period
│   └── exception/      # Business exceptions
├── application/         # Use cases (Services)
├── infrastructure/      # Technical (JPA, persistence)
│   └── persistence/    # Repositories, Entities, Assemblers
└── web/                # API layer
    ├── controller/     # REST endpoints
    ├── dto/            # Request/Response DTOs
    └── advice/         # Exception handlers

src/test/
├── java/.../integration/  # Integration tests (Testcontainers)
└── java/.../domain/       # Unit tests
```

---

## 📚 Documentation

### Architecture & Design
- **[docs/DDD_SERVICES_ARCHITECTURE.md](docs/DDD_SERVICES_ARCHITECTURE.md)** - Architecture DDD détaillée
- **[docs/DOCKER_OPTIMIZATION.md](docs/DOCKER_OPTIMIZATION.md)** - Optimisation de l'image Docker
- **[docs/MIGRATION_GUIDE_V2.md](docs/MIGRATION_GUIDE_V2.md)** - Guide de migration vers v2

### Documentation Technique (sessions Claude)
- **[docs-claude/](docs-claude/)** - Documentation générée lors des sessions de développement
  - [Pagination Architecture](docs-claude/PAGINATION_ARCHITECTURE.md)
  - [Docker Build Guide](docs-claude/DOCKER_BUILD_GUIDE.md)
  - [Et plus...](docs/DDD/README.md)

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

