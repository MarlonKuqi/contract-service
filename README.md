# Contract Service

A RESTful API for managing insurance clients (persons and companies) and their contracts, built with Spring Boot and PostgreSQL.

---

## 🚀 Run Locally

### Option 1: Docker Compose (Recommended)

**Prerequisites:**
- Docker Desktop ([Download](https://www.docker.com/products/docker-desktop/))

**Steps:**

1. Clone and start:
   ```bash
   git clone <repository-url>
   cd contract-service
   docker-compose up --build
   ```

2. Verify: `curl http://localhost:8080/actuator/health`

3. Stop: `docker-compose down`

**What happens automatically:**
- PostgreSQL database created
- Schema `contracts` created via Flyway
- All migrations run automatically
- API available on port 8080 (production profile by default)

**First launch takes 2-3 minutes.**

---

### Option 2: Executable JAR

**Prerequisites:**
- Java 21+
- PostgreSQL 12+ running

**Steps:**

1. **Create database:**
   ```sql
   CREATE DATABASE contract;
   ```

2. **Run the JAR:**
   ```bash
   # Development/testing (using postgres superuser)
   java -jar contract-service-1.0.0-SNAPSHOT.jar

   # Production (recommended: use custom credentials)
   java -jar contract-service-1.0.0-SNAPSHOT.jar \
     --spring.datasource.username=postgres \
     --spring.datasource.password=your_secure_password
   ```

**Flyway will automatically:**
- ✅ Create schema `contracts`
- ✅ Create all tables, indexes, and optimizations

**Important:**
- Default uses `postgres`/`postgres` (superuser)
- For production: use a strong password or create a dedicated user
- The postgres user has all necessary privileges

---

## 📚 API Documentation

All endpoints are under `/v2/` prefix.

### Postman Collections

Import from `api-collections/`:
- `ClientController.postman_collection.json`
- `ContractController.postman_collection.json`

### Main Endpoints

#### Clients
- `POST /v2/clients` - Create a client (Person or Company) with type discriminator
- `GET /v2/clients/{id}` - Get client details
- `PUT /v2/clients/{id}` - Update client (name, email, phone)
- `DELETE /v2/clients/{id}` - Delete client and close all active contracts

#### Contracts
- `POST /v2/contracts?clientId={clientId}` - Create contract for a client
- `GET /v2/contracts?clientId={clientId}` - Get active contracts (paginated, filterable by update date)
- `GET /v2/contracts/{contractId}?clientId={clientId}` - Get a specific contract
- `PATCH /v2/contracts/{contractId}/cost?clientId={clientId}` - Update contract cost
- `GET /v2/contracts/sum?clientId={clientId}` - Sum of active contracts (optimized, < 1 second)

**⚠️ Breaking Change in v2.0.0:**
- All endpoints moved from `/v1/` to `/v2/`
- Removed `POST /v1/clients/persons` and `POST /v1/clients/companies`
- Changed contract endpoints from `/v1/clients/{clientId}/contracts*` to `/v2/contracts?clientId={clientId}`
- `/v1/` is **no longer supported** - all clients must migrate to `/v2/`

---

## 🏗️ Architecture & Design

**Clean Architecture with Hexagonal principles:**
- **Domain Layer**: Core business logic (Client, Contract entities)
- **Application Layer**: Use cases and orchestration (DTOs, Services)
- **Infrastructure Layer**: Technical implementations (JPA repositories, REST controllers)

**Key Design Decisions:**
- **Single Table Inheritance** for Client types (Person/Company) - simpler queries, better performance
- **Flyway migrations** for versioned database schema evolution (including user/schema creation)
- **Indexed queries** on contract dates and client relationships for performance
- **Materialized computation** for sum endpoint to ensure sub-second response times
- **Bean Validation** (JSR-303) for input validation (ISO-8601 dates, emails, phone numbers)
- **Soft delete pattern** - contract end dates updated instead of hard deletion

**Database:** PostgreSQL with dedicated `contracts` schema for logical separation.

---

## 🧪 Testing

Run all tests:
```bash
./mvnw verify
```

Generate coverage report:
```bash
./mvnw test
./mvnw jacoco:report
# Report: target/site/jacoco/index.html
```

---

## 📋 Requirements Checklist

✅ Create, read, update, delete clients (Person & Company)\
✅ Validate phone, email, dates (ISO 8601), identifiers\
✅ Create and update contracts with automatic date management\
✅ Get active contracts with update date filter\
✅ Optimized sum endpoint for active contracts (< 1 second)\
✅ Soft delete - updating contract end dates on client deletion\
✅ RESTful API with JSON format\
✅ Spring Boot 3 + Java 21\
✅ PostgreSQL persistence\
✅ Descriptive code with clear naming\
✅ Flyway handles ALL database setup (no manual SQL required)

---

## 🔧 Configuration

Main configuration in `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/contract
    username: dev
    password: dev
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    default-schema: contracts
```

Override via environment variables:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

---

## 🔐 Security Notes

- Dockerfile runs as non-root user `appuser`
- Default credentials (`dev`/`dev`) are for development only
- In production, use strong passwords and environment variables
- Dependencies are monitored for vulnerabilities using OWASP Dependency-Check
- Regular security updates applied to base images and dependencies

---

## 📝 Important Notes

- **Update date** field is managed internally, not exposed in API
- **Dates** must follow ISO 8601 format (YYYY-MM-DD)
- **Active contracts**: end date is null or in the future
- **All Flyway migrations** embedded in JAR and run automatically
- **Database setup** (user, schema, grants) handled by Flyway migration `V1.0.0.0__init_db.sql`

---

## 🛠️ Build from Source

```bash
# Clone repository
git clone <repository-url>
cd contract-service

# Build JAR
./mvnw clean package

# Run tests with coverage
./mvnw verify

# JAR location: target/contract-service-1.0.0-SNAPSHOT.jar
```

---

## 📖 Additional Resources

- `api-collections/` - Postman collections for all endpoints