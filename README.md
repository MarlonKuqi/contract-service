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
- User `dev` and schema `contracts` created via Flyway
- All migrations run automatically
- API available on port 8080

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

### Quick Test

See **[API_TESTING.md](API_TESTING.md)** for step-by-step curl commands.

### Postman Collections

Import from `api-collections/`:
- `ClientController.postman_collection.json`
- `ContractController.postman_collection.json`
- `PersonController.postman_collection.json`
- `CompanyController.postman_collection.json`

### Main Endpoints

#### Clients
- `POST /api/clients/person` - Create a person client
- `POST /api/clients/company` - Create a company client
- `GET /api/clients/{id}` - Get client details
- `PUT /api/clients/{id}` - Update client
- `DELETE /api/clients/{id}` - Soft delete (ends all contracts)

#### Contracts
- `POST /api/clients/{clientId}/contracts` - Create contract
- `GET /api/clients/{clientId}/contracts` - Get active contracts (with optional update date filter)
- `PUT /api/contracts/{id}/cost` - Update contract cost
- `GET /api/clients/{clientId}/contracts/sum` - Get sum of active contracts (optimized)

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
./mvnw test
```

Generate coverage report:
```bash
./mvnw verify
# Report: target/site/jacoco/index.html
```

---

## 📋 Requirements Checklist

✅ Create, read, update, delete clients (Person & Company)
✅ Validate phone, email, dates (ISO 8601), identifiers
✅ Create and update contracts with automatic date management
✅ Get active contracts with update date filter
✅ Optimized sum endpoint for active contracts (< 1 second)
✅ Soft delete - updating contract end dates on client deletion
✅ RESTful API with JSON format
✅ Spring Boot 3 + Java 21
✅ PostgreSQL persistence
✅ Descriptive code with clear naming
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

## 📦 Project Structure

```
contract-service/
├── src/main/java/com/mk/contractservice/
│   ├── application/          # DTOs, Application Services
│   ├── domain/              # Entities, Value Objects
│   ├── infrastructure/      # Controllers, Repositories
│   └── ContractServiceApplication.java
├── src/main/resources/
│   ├── db/migration/        # Flyway SQL scripts (V1.0.0.0 = DB setup)
│   └── application.yml
├── src/test/                # Unit and Integration tests
├── api-collections/         # Postman collections
├── docker-compose.yml       # Docker orchestration
└── Dockerfile              # Multi-stage build (non-root user)
```

---

## 🔐 Security Notes

- Dockerfile runs as non-root user `appuser`
- Default credentials (`dev`/`dev`) are for development only
- In production, use strong passwords and environment variables

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

- [API Testing Guide](API_TESTING.md) - Step-by-step curl examples
- `api-collections/` - Postman collections for all endpoints
- `target/site/jacoco/` - Test coverage reports (after `mvn verify`)
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
- User `dev` and schema `contracts` created via Flyway
- All migrations run automatically
- API available on port 8080

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

2. **Run the JAR (first time with superuser to create dev user):**
   ```bash
   # First run: Use postgres to let Flyway create the 'dev' user
   java -jar contract-service-1.0.0-SNAPSHOT.jar \
     --spring.datasource.username=postgres \
     --spring.datasource.password=your_postgres_password
   ```

3. **Subsequent runs can use dev user:**
   ```bash
   # After first run, you can use the dev user
   java -jar contract-service-1.0.0-SNAPSHOT.jar \
     --spring.datasource.username=dev \
     --spring.datasource.password=dev
   ```

**Flyway will automatically (first run):**
- ✅ Create user `dev` with password `dev`
- ✅ Create schema `contracts` 
- ✅ Grant all privileges to `dev` user
- ✅ Create all tables, indexes, and optimizations

**For production:**
```bash
# Option 1: Let Flyway create 'dev' user (simple but uses default password)
java -jar contract-service-1.0.0-SNAPSHOT.jar \
  --spring.datasource.username=postgres \
  --spring.datasource.password=secure_postgres_password

# Option 2: Create a custom user manually before running
psql -U postgres -d contract -c "CREATE USER myapp WITH PASSWORD 'secure_password';"
# Then modify V1.0.0.0__init_db.sql to use 'myapp' instead of 'dev'
```

**Important:** 
- First run requires a superuser (postgres) to create the `dev` user
- Flyway migration creates `dev` user with fixed name (stable checksum)
- Default password is `dev` - change in production!

---

## 📚 API Documentation

### Quick Test

See **[API_TESTING.md](API_TESTING.md)** for step-by-step curl commands.

### Postman Collections

Import from `api-collections/`:
- `ClientController.postman_collection.json`
- `ContractController.postman_collection.json`
- `PersonController.postman_collection.json`
- `CompanyController.postman_collection.json`

### Main Endpoints

#### Clients
- `POST /api/clients/person` - Create a person client
- `POST /api/clients/company` - Create a company client
- `GET /api/clients/{id}` - Get client details
- `PUT /api/clients/{id}` - Update client
- `DELETE /api/clients/{id}` - Soft delete (ends all contracts)

#### Contracts
- `POST /api/clients/{clientId}/contracts` - Create contract
- `GET /api/clients/{clientId}/contracts` - Get active contracts (with optional update date filter)
- `PUT /api/contracts/{id}/cost` - Update contract cost
- `GET /api/clients/{clientId}/contracts/sum` - Get sum of active contracts (optimized)

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
./mvnw test
```

Generate coverage report:
```bash
./mvnw verify
# Report: target/site/jacoco/index.html
```

---

## 📋 Requirements Checklist

✅ Create, read, update, delete clients (Person & Company)
✅ Validate phone, email, dates (ISO 8601), identifiers
✅ Create and update contracts with automatic date management
✅ Get active contracts with update date filter
✅ Optimized sum endpoint for active contracts (< 1 second)
✅ Soft delete - updating contract end dates on client deletion
✅ RESTful API with JSON format
✅ Spring Boot 3 + Java 21
✅ PostgreSQL persistence
✅ Descriptive code with clear naming
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

## 📦 Project Structure

```
contract-service/
├── src/main/java/com/mk/contractservice/
│   ├── application/          # DTOs, Application Services
│   ├── domain/              # Entities, Value Objects
│   ├── infrastructure/      # Controllers, Repositories
│   └── ContractServiceApplication.java
├── src/main/resources/
│   ├── db/migration/        # Flyway SQL scripts (V1.0.0.0 = DB setup)
│   └── application.yml
├── src/test/                # Unit and Integration tests
├── api-collections/         # Postman collections
├── docker-compose.yml       # Docker orchestration
└── Dockerfile              # Multi-stage build (non-root user)
```

---

## 🔐 Security Notes

- Dockerfile runs as non-root user `appuser`
- Default credentials (`dev`/`dev`) are for development only
- In production, use strong passwords and environment variables

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

- [API Testing Guide](API_TESTING.md) - Step-by-step curl examples
- `api-collections/` - Postman collections for all endpoints
- `target/site/jacoco/` - Test coverage reports (after `mvn verify`)
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
- User `dev` and schema `contracts` created via Flyway
- All migrations run automatically
- API available on port 8080

**First launch takes 2-3 minutes.**

---

### Option 2: Executable JAR

**Prerequisites:**
- Java 21+
- PostgreSQL 12+ running

**Steps:**

1. **Create database and user with proper privileges:**
   ```sql
   -- Create database
   CREATE DATABASE contract;

   -- Option A: Use postgres superuser (dev/testing only)
   -- No additional setup needed

   -- Option B: Create dedicated user (production recommended)
   CREATE USER contract_app WITH PASSWORD 'secure_password';
   ALTER USER contract_app CREATEDB; -- Needed for schema creation
   GRANT ALL PRIVILEGES ON DATABASE contract TO contract_app;
   ```

2. **Run the JAR:**
   ```bash
   # Using postgres superuser (development/testing)
   java -jar contract-service-1.0.0-SNAPSHOT.jar

   # Or with dedicated user (production)
   java -jar contract-service-1.0.0-SNAPSHOT.jar \
     --spring.datasource.username=contract_app \
     --spring.datasource.password=secure_password
   ```

**Flyway will automatically:**
- ✅ Create schema `contracts`
- ✅ Create all tables, indexes, and optimizations
- ✅ Work with any user that has schema creation privileges

**Important:**
- The database user MUST have schema creation privileges
- Default config uses `postgres`/`postgres` (superuser, has all privileges)
- For production: create a dedicated user with `CREATEDB` or make it owner of the database

---

## 📚 API Documentation

### Quick Test

See **[API_TESTING.md](API_TESTING.md)** for step-by-step curl commands.

### Postman Collections

Import from `api-collections/`:
- `ClientController.postman_collection.json`
- `ContractController.postman_collection.json`
- `PersonController.postman_collection.json`
- `CompanyController.postman_collection.json`

### Main Endpoints

#### Clients
- `POST /api/clients/person` - Create a person client
- `POST /api/clients/company` - Create a company client
- `GET /api/clients/{id}` - Get client details
- `PUT /api/clients/{id}` - Update client
- `DELETE /api/clients/{id}` - Soft delete (ends all contracts)

#### Contracts
- `POST /api/clients/{clientId}/contracts` - Create contract
- `GET /api/clients/{clientId}/contracts` - Get active contracts (with optional update date filter)
- `PUT /api/contracts/{id}/cost` - Update contract cost
- `GET /api/clients/{clientId}/contracts/sum` - Get sum of active contracts (optimized)

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
./mvnw test
```

Generate coverage report:
```bash
./mvnw verify
# Report: target/site/jacoco/index.html
```

---

## 📋 Requirements Checklist

✅ Create, read, update, delete clients (Person & Company)
✅ Validate phone, email, dates (ISO 8601), identifiers
✅ Create and update contracts with automatic date management
✅ Get active contracts with update date filter
✅ Optimized sum endpoint for active contracts (< 1 second)
✅ Soft delete - updating contract end dates on client deletion
✅ RESTful API with JSON format
✅ Spring Boot 3 + Java 21
✅ PostgreSQL persistence
✅ Descriptive code with clear naming
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

## 📦 Project Structure

```
contract-service/
├── src/main/java/com/mk/contractservice/
│   ├── application/          # DTOs, Application Services
│   ├── domain/              # Entities, Value Objects
│   ├── infrastructure/      # Controllers, Repositories
│   └── ContractServiceApplication.java
├── src/main/resources/
│   ├── db/migration/        # Flyway SQL scripts (V1.0.0.0 = DB setup)
│   └── application.yml
├── src/test/                # Unit and Integration tests
├── api-collections/         # Postman collections
├── docker-compose.yml       # Docker orchestration
└── Dockerfile              # Multi-stage build (non-root user)
```

---

## 🔐 Security Notes

- Dockerfile runs as non-root user `appuser`
- Default credentials (`dev`/`dev`) are for development only
- In production, use strong passwords and environment variables

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

- [API Testing Guide](API_TESTING.md) - Step-by-step curl examples
- `api-collections/` - Postman collections for all endpoints
- `target/site/jacoco/` - Test coverage reports (after `mvn verify`)
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
- User `dev` and schema `contracts` created via Flyway
- All migrations run automatically
- API available on port 8080

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
   -- For production, create a dedicated user:
   -- CREATE USER contract_app WITH PASSWORD 'secure_password';
   -- GRANT ALL PRIVILEGES ON DATABASE contract TO contract_app;
   ```

2. **Run the JAR:**
   ```bash
   # Using default postgres user (development)
   java -jar contract-service-1.0.0-SNAPSHOT.jar

   # Or with custom credentials (production)
   java -jar contract-service-1.0.0-SNAPSHOT.jar \
     --spring.datasource.username=contract_app \
     --spring.datasource.password=secure_password
   ```

**Flyway will automatically:**
- ✅ Create schema `contracts`
- ✅ Create all tables, indexes, and optimizations
- ✅ Grant necessary privileges to current user

**Note:** Default config uses `postgres`/`postgres`. Always override in production!

---

## 📚 API Documentation

### Quick Test

See **[API_TESTING.md](API_TESTING.md)** for step-by-step curl commands.

### Postman Collections

Import from `api-collections/`:
- `ClientController.postman_collection.json`
- `ContractController.postman_collection.json`
- `PersonController.postman_collection.json`
- `CompanyController.postman_collection.json`

### Main Endpoints

#### Clients
- `POST /api/clients/person` - Create a person client
- `POST /api/clients/company` - Create a company client
- `GET /api/clients/{id}` - Get client details
- `PUT /api/clients/{id}` - Update client
- `DELETE /api/clients/{id}` - Soft delete (ends all contracts)

#### Contracts
- `POST /api/clients/{clientId}/contracts` - Create contract
- `GET /api/clients/{clientId}/contracts` - Get active contracts (with optional update date filter)
- `PUT /api/contracts/{id}/cost` - Update contract cost
- `GET /api/clients/{clientId}/contracts/sum` - Get sum of active contracts (optimized)

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
./mvnw test
```

Generate coverage report:
```bash
./mvnw verify
# Report: target/site/jacoco/index.html
```

---

## 📋 Requirements Checklist

✅ Create, read, update, delete clients (Person & Company)
✅ Validate phone, email, dates (ISO 8601), identifiers
✅ Create and update contracts with automatic date management
✅ Get active contracts with update date filter
✅ Optimized sum endpoint for active contracts (< 1 second)
✅ Soft delete - updating contract end dates on client deletion
✅ RESTful API with JSON format
✅ Spring Boot 3 + Java 21
✅ PostgreSQL persistence
✅ Descriptive code with clear naming
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

## 📦 Project Structure

```
contract-service/
├── src/main/java/com/mk/contractservice/
│   ├── application/          # DTOs, Application Services
│   ├── domain/              # Entities, Value Objects
│   ├── infrastructure/      # Controllers, Repositories
│   └── ContractServiceApplication.java
├── src/main/resources/
│   ├── db/migration/        # Flyway SQL scripts (V1.0.0.0 = DB setup)
│   └── application.yml
├── src/test/                # Unit and Integration tests
├── api-collections/         # Postman collections
├── docker-compose.yml       # Docker orchestration
└── Dockerfile              # Multi-stage build (non-root user)
```

---

## 🔐 Security Notes

- Dockerfile runs as non-root user `appuser`
- Default credentials (`dev`/`dev`) are for development only
- In production, use strong passwords and environment variables

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

- [API Testing Guide](API_TESTING.md) - Step-by-step curl examples
- `api-collections/` - Postman collections for all endpoints
- `target/site/jacoco/` - Test coverage reports (after `mvn verify`)
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
- User `dev` and schema `contracts` created via Flyway
- All migrations run automatically
- API available on port 8080

**First launch takes 2-3 minutes.**

---

### Option 2: Executable JAR

**Prerequisites:**
- Java 21+
- PostgreSQL 12+ running

**Steps:**

1. **Create database and user:**
   ```sql
   CREATE DATABASE contract;
   CREATE USER dev WITH PASSWORD 'dev';
   GRANT ALL PRIVILEGES ON DATABASE contract TO dev;
   ```

2. **Run the JAR:**
   ```bash
   java -jar contract-service-1.0.0-SNAPSHOT.jar
   ```

**Flyway will automatically:**
- ✅ Create schema `contracts`
- ✅ Run all migrations (tables, indexes, optimizations)
- ✅ Initialize the database completely

**Note:** The JAR expects a user `dev` with password `dev` by default. Override with:
```bash
java -jar contract-service-1.0.0-SNAPSHOT.jar \
  --spring.datasource.username=your_user \
  --spring.datasource.password=your_password
```

---

## 📚 API Documentation

### Quick Test

See **[API_TESTING.md](API_TESTING.md)** for step-by-step curl commands.

### Postman Collections

Import from `api-collections/`:
- `ClientController.postman_collection.json`
- `ContractController.postman_collection.json`
- `PersonController.postman_collection.json`
- `CompanyController.postman_collection.json`

### Main Endpoints

#### Clients
- `POST /api/clients/person` - Create a person client
- `POST /api/clients/company` - Create a company client
- `GET /api/clients/{id}` - Get client details
- `PUT /api/clients/{id}` - Update client
- `DELETE /api/clients/{id}` - Soft delete (ends all contracts)

#### Contracts
- `POST /api/clients/{clientId}/contracts` - Create contract
- `GET /api/clients/{clientId}/contracts` - Get active contracts (with optional update date filter)
- `PUT /api/contracts/{id}/cost` - Update contract cost
- `GET /api/clients/{clientId}/contracts/sum` - Get sum of active contracts (optimized)

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
./mvnw test
```

Generate coverage report:
```bash
./mvnw verify
# Report: target/site/jacoco/index.html
```

---

## 📋 Requirements Checklist

✅ Create, read, update, delete clients (Person & Company)
✅ Validate phone, email, dates (ISO 8601), identifiers
✅ Create and update contracts with automatic date management
✅ Get active contracts with update date filter
✅ Optimized sum endpoint for active contracts (< 1 second)
✅ Soft delete - updating contract end dates on client deletion
✅ RESTful API with JSON format
✅ Spring Boot 3 + Java 21
✅ PostgreSQL persistence
✅ Descriptive code with clear naming
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

## 📦 Project Structure

```
contract-service/
├── src/main/java/com/mk/contractservice/
│   ├── application/          # DTOs, Application Services
│   ├── domain/              # Entities, Value Objects
│   ├── infrastructure/      # Controllers, Repositories
│   └── ContractServiceApplication.java
├── src/main/resources/
│   ├── db/migration/        # Flyway SQL scripts (V1.0.0.0 = DB setup)
│   └── application.yml
├── src/test/                # Unit and Integration tests
├── api-collections/         # Postman collections
├── docker-compose.yml       # Docker orchestration
└── Dockerfile              # Multi-stage build (non-root user)
```

---

## 🔐 Security Notes

- Dockerfile runs as non-root user `appuser`
- Default credentials (`dev`/`dev`) are for development only
- In production, use strong passwords and environment variables

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

- [API Testing Guide](API_TESTING.md) - Step-by-step curl examples
- `api-collections/` - Postman collections for all endpoints
- `target/site/jacoco/` - Test coverage reports (after `mvn verify`)
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
- Spring Boot starts with `postgres` superuser
- Flyway creates schema `contracts` (V1.0.0.0)
- Flyway creates all tables and indexes (V1.0.0.1, V1.0.0.2, V1.0.0.3)
- API available on port 8080

**Note:** Uses `postgres` superuser everywhere for simplicity.

**Troubleshooting:**
- **Error: "port is already allocated"?**
  - Your local PostgreSQL is using port 5432
  - The `docker-compose.yml` is configured to use port 5433 on the host instead
  - PostgreSQL accessible at `localhost:5433` from your machine
  - No need to stop your local PostgreSQL!

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
   java -jar contract-service-1.0.0-SNAPSHOT.jar \
     --spring.datasource.url=jdbc:postgresql://localhost:5432/contract \
     --spring.datasource.username=postgres \
     --spring.datasource.password=your_password
   ```

**Flyway will automatically:**
- ✅ Create user `dev` and schema `contracts`
- ✅ Run all migrations (tables, indexes, optimizations)
- ✅ Initialize the database completely

**Note:** All database setup scripts are embedded in the JAR via Flyway migrations.

---

## 📚 API Documentation

### Quick Test

See **[API_TESTING.md](API_TESTING.md)** for step-by-step curl commands.

### Postman Collections

Import from `api-collections/`:
- `ClientController.postman_collection.json`
- `ContractController.postman_collection.json`
- `PersonController.postman_collection.json`
- `CompanyController.postman_collection.json`

### Main Endpoints

#### Clients
- `POST /api/clients/person` - Create a person client
- `POST /api/clients/company` - Create a company client
- `GET /api/clients/{id}` - Get client details
- `PUT /api/clients/{id}` - Update client
- `DELETE /api/clients/{id}` - Soft delete (ends all contracts)

#### Contracts
- `POST /api/clients/{clientId}/contracts` - Create contract
- `GET /api/clients/{clientId}/contracts` - Get active contracts (with optional update date filter)
- `PUT /api/contracts/{id}/cost` - Update contract cost
- `GET /api/clients/{clientId}/contracts/sum` - Get sum of active contracts (optimized)

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
./mvnw test
```

Generate coverage report:
```bash
./mvnw verify
# Report: target/site/jacoco/index.html
```

---

## 📋 Requirements Checklist

✅ Create, read, update, delete clients (Person & Company)
✅ Validate phone, email, dates (ISO 8601), identifiers
✅ Create and update contracts with automatic date management
✅ Get active contracts with update date filter
✅ Optimized sum endpoint for active contracts (< 1 second)
✅ Soft delete - updating contract end dates on client deletion
✅ RESTful API with JSON format
✅ Spring Boot 3 + Java 21
✅ PostgreSQL persistence
✅ Descriptive code with clear naming
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

## 📦 Project Structure

```
contract-service/
├── src/main/java/com/mk/contractservice/
│   ├── application/          # DTOs, Application Services
│   ├── domain/              # Entities, Value Objects
│   ├── infrastructure/      # Controllers, Repositories
│   └── ContractServiceApplication.java
├── src/main/resources/
│   ├── db/migration/        # Flyway SQL scripts (V1.0.0.0 = DB setup)
│   └── application.yml
├── src/test/                # Unit and Integration tests
├── api-collections/         # Postman collections
├── docker-compose.yml       # Docker orchestration
└── Dockerfile              # Multi-stage build (non-root user)
```

---

## 🔐 Security Notes

- Dockerfile runs as non-root user `appuser`
- Default credentials (`dev`/`dev`) are for development only
- In production, use strong passwords and environment variables

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

- [API Testing Guide](API_TESTING.md) - Step-by-step curl examples
- `api-collections/` - Postman collections for all endpoints
- `target/site/jacoco/` - Test coverage reports (after `mvn verify`)

