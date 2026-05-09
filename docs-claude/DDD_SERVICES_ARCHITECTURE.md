# DDD Services Architecture - Complete Guide

## 📋 Overview

This document explains the DDD (Domain-Driven Design) architecture implemented in the contract-service project, focusing on the separation between **Application Services** and **Domain Services**.

---

## 🏗️ Architecture Layers

```
┌─────────────────────────────────────────────────┐
│  Web Layer (Controllers + Advice)               │
│  - ClientController, ContractController         │
│  - ClientControllerAdvice                       │
│  - DTOs, Mappers                                │
└─────────────────┬───────────────────────────────┘
                  │ calls
┌─────────────────▼───────────────────────────────┐
│  Application Layer (Use Case Orchestration)     │
│  - ClientApplicationService                     │
│  - ContractApplicationService                   │
│  - @Transactional, caching, logging             │
└─────────────────┬───────────────────────────────┘
                  │ uses
┌─────────────────▼───────────────────────────────┐
│  Domain Layer (Business Logic)                  │
│  ┌───────────────────────────────────────────┐  │
│  │ Domain Services                           │  │
│  │  - ClientDomainService (record)           │  │
│  │    • createPerson()                       │  │
│  │    • createCompany()                      │  │
│  │    • ensureEmailIsUnique()                │  │
│  │    • ensureCompanyIdentifierIsUnique()    │  │
│  └───────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────┐  │
│  │ Aggregates                                │  │
│  │  - Client (root)                          │  │
│  │  - Person, Company                        │  │
│  │  - Contract (root)                        │  │
│  └───────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────┐  │
│  │ Value Objects                             │  │
│  │  - Email, PhoneNumber, ClientName, etc.   │  │
│  └───────────────────────────────────────────┘  │
└─────────────────┬───────────────────────────────┘
                  │ persisted by
┌─────────────────▼───────────────────────────────┐
│  Infrastructure Layer                           │
│  - ClientRepository (interface in domain)       │
│  - ClientJpaRepository (impl in infra)          │
│  - DomainConfig (Spring bean registration)      │
└─────────────────────────────────────────────────┘
```

---

## 🔍 Service Types Explained

### 1. Application Services

**Location**: `src/main/java/.../application/`  
**Stereotype**: `@Service`  
**Purpose**: Orchestrate use cases

**Characteristics**:
- ✅ Thin, no business logic
- ✅ Transaction boundaries (`@Transactional`)
- ✅ Coordinate domain services and aggregates
- ✅ Convert DTOs ↔ Domain objects
- ✅ Infrastructure concerns (caching, logging)

**Examples in our code**:
```java
// ✅ GOOD: Pure orchestration
@Transactional
public void patchClient(UUID id, ClientName name, Email email, PhoneNumber phone) {
    Client client = getClientById(id);
    
    boolean hasChanges = false;
    if (name != null) {
        client.changeName(name);  // ← Delegates to aggregate
        hasChanges = true;
    }
    // ...
    if (hasChanges) {
        clientRepo.save(client);  // ← Infrastructure
    }
}

// ✅ GOOD: Cross-aggregate orchestration
@Transactional
public void deleteClientAndCloseContracts(UUID id) {
    if (!clientRepo.existsById(id)) {
        throw new ClientNotFoundException(...);
    }
    contractService.closeActiveContractsByClientId(id); // ← Orchestration
    clientRepo.deleteById(id);
}
```

---

### 2. Domain Services

**Location**: `src/main/java/.../domain/`  
**Stereotype**: No Spring annotations (registered via `@Configuration`)  
**Purpose**: Business logic that doesn't fit in a single aggregate

**Characteristics**:
- ✅ Pure domain logic (no `@Transactional`, no caching)
- ✅ Stateless (implemented as `record` in Java 17+)
- ✅ Framework-agnostic (no Spring coupling)
- ✅ Reusable across use cases
- ✅ Named after business concepts

**When to use**:
- ✅ Business logic involving multiple aggregates
- ✅ Domain invariants requiring repository access
- ✅ Complex calculations/validations
- ✅ **Entity creation with business rules**

**Examples in our code**:
```java
public record ClientDomainService(ClientRepository clientRepository) {
    
    // Business rule: Create Person with validation
    public Person createPerson(ClientName name, Email email, 
                               PhoneNumber phone, PersonBirthDate birthDate) {
        ensureEmailIsUnique(email);
        return Person.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .birthDate(birthDate)
                .build();
    }
    
    // Business rule: Email uniqueness
    public void ensureEmailIsUnique(Email email) {
        if (clientRepository.existsByEmail(email.value())) {
            throw new ClientAlreadyExistsException("Client already exists", email.value());
        }
    }
}
```

---

### 3. Aggregates

**Location**: `src/main/java/.../domain/`  
**Purpose**: Encapsulate business logic about entity's own state

**Characteristics**:
- ✅ Enforce invariants within aggregate boundary
- ✅ Methods modify own state
- ✅ No repository access

**Examples in our code**:
```java
public abstract class Client {
    // Business logic about Client's own state
    public void changeName(ClientName newName) {
        this.name = newName;
    }
    
    public void changeEmail(Email newEmail) {
        this.email = newEmail;
    }
}
```

---

## 🎯 Decision Tree: Where Does Logic Go?

```
Is it business logic?
├─ NO → Application Service (orchestration, transactions, DTO mapping)
│
└─ YES → Does it modify only ONE aggregate's state?
         ├─ YES → Put in Aggregate (changeName, changeEmail)
         │
         └─ NO → Does it need repository access or create entities?
                 ├─ YES → Domain Service (createPerson, ensureEmailIsUnique)
                 │
                 └─ NO → Could be Value Object or utility
```

---

## 📦 Current Implementation

### Domain Service (Record Pattern)

**File**: `ClientDomainService.java`

```java
/**
 * Domain Service implemented as a record to emphasize immutability and statelessness.
 * Contains business logic for Client creation and validation.
 */
public record ClientDomainService(ClientRepository clientRepository) {
    
    /**
     * Creates a Person with business validation.
     * This is domain logic because it involves business rules (email uniqueness).
     */
    public Person createPerson(ClientName name, Email email, 
                               PhoneNumber phone, PersonBirthDate birthDate) {
        ensureEmailIsUnique(email);
        return Person.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .birthDate(birthDate)
                .build();
    }
    
    /**
     * Creates a Company with business validation.
     * This is domain logic because it involves business rules (email + identifier uniqueness).
     */
    public Company createCompany(ClientName name, Email email, 
                                 PhoneNumber phone, CompanyIdentifier companyIdentifier) {
        ensureEmailIsUnique(email);
        ensureCompanyIdentifierIsUnique(companyIdentifier);
        return Company.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .companyIdentifier(companyIdentifier)
                .build();
    }
    
    /**
     * Business rule: Email must be unique across all clients.
     */
    public void ensureEmailIsUnique(Email email) {
        if (clientRepository.existsByEmail(email.value())) {
            throw new ClientAlreadyExistsException("Client already exists", email.value());
        }
    }
    
    /**
     * Business rule: Company identifier must be unique.
     */
    public void ensureCompanyIdentifierIsUnique(CompanyIdentifier identifier) {
        if (clientRepository.existsByCompanyIdentifier(identifier.value())) {
            throw new CompanyIdentifierAlreadyExistsException(
                    "A company with identifier '" + identifier.value() + "' already exists",
                    identifier.value()
            );
        }
    }
}
```

**Why record?**
- ✅ Immutable by default
- ✅ Less boilerplate (no constructor, getters)
- ✅ Emphasizes statelessness
- ✅ Modern Java best practice (Java 17+)

---

### Application Service (Orchestration)

**File**: `ClientApplicationService.java`

```java
@Service
public class ClientApplicationService {
    
    private final ClientRepository clientRepo;
    private final ClientDomainService clientDomainService;
    
    /**
     * Use case: Create Person
     * Application Service orchestrates: Domain Service creates, Repository saves.
     */
    @Transactional
    public Person createPerson(String name, String email, String phone, LocalDate birthDate) {
        // Domain Service handles creation + validation
        Person person = clientDomainService.createPerson(
                ClientName.of(name),
                Email.of(email),
                PhoneNumber.of(phone),
                PersonBirthDate.of(birthDate)
        );
        
        // Application Service handles persistence
        return (Person) clientRepo.save(person);
    }
    
    /**
     * Use case: Create Company
     * Same pattern: delegate creation to Domain Service, handle persistence here.
     */
    @Transactional
    public Company createCompany(String name, String email, String phone, String companyId) {
        Company company = clientDomainService.createCompany(
                ClientName.of(name),
                Email.of(email),
                PhoneNumber.of(phone),
                CompanyIdentifier.of(companyId)
        );
        return (Company) clientRepo.save(company);
    }
}
```

---

### Domain Configuration

**File**: `DomainConfig.java`

```java
@Configuration
public class DomainConfig {
    
    @Bean
    public ClientDomainService clientDomainService(ClientRepository clientRepository) {
        return new ClientDomainService(clientRepository);
    }
}
```

**Purpose:**
- ✅ Register Domain Services as Spring beans
- ✅ Keep domain layer clean (no `@Service` annotation in domain)
- ✅ Separation: infrastructure config vs domain logic
- ✅ Framework-agnostic domain layer

---

## 💡 Method Naming Conventions

### Pattern 1: `create*()` - Entity Creation
**Returns**: Entity instance  
**Behavior**: Creates entity with validation

```java
public Person createPerson(ClientName name, Email email, ...) {
    ensureEmailIsUnique(email);  // Validation
    return Person.builder()...build();  // Creation
}
```

**When to use:**
- ✅ Entity creation involves business rules
- ✅ Need validation before construction
- ✅ Complex initialization logic

**Where**: Domain Service

---

### Pattern 2: `ensure*()` - Invariant Enforcement
**Returns**: `void`  
**Behavior**: Throws exception if condition not met

```java
public void ensureEmailIsUnique(Email email) {
    if (clientRepository.existsByEmail(email.value())) {
        throw new ClientAlreadyExistsException(...);
    }
    // Silence = success
}
```

**When to use:**
- ✅ Business invariants that MUST be respected
- ✅ Breaking case = exception (not conditional)
- ✅ Guard clause / defensive programming

**Where**: Domain Service

**Why not return boolean?**
- ✅ Cleaner caller code (no if/else)
- ✅ Domain Service owns validation + violation handling
- ✅ `ensure*` naming convention = assertion pattern

---

### Pattern 3: `is*/has*()` - Query Methods
**Returns**: `boolean`  
**Behavior**: Never throws, returns true/false

```java
public boolean isEmailAlreadyRegistered(Email email) {
    return clientRepository.existsByEmail(email.value());
}
```

**When to use:**
- ✅ Business questions (not enforcements)
- ✅ Conditional logic needed
- ✅ Non-violation is valid outcome

**Where**: Domain Service or Aggregate

---

### Pattern 4: `change*/update*()` - State Modification
**Returns**: `void`  
**Behavior**: Modifies aggregate state

```java
public void changeName(ClientName newName) {
    this.name = newName;
}
```

**When to use:**
- ✅ Modifying aggregate's own state
- ✅ Business logic about entity itself

**Where**: Aggregate

---

## 🧪 Testing Strategy

### Domain Service Tests (No Spring Context)

```java
class ClientDomainServiceTest {
    
    @Mock
    private ClientRepository repository;
    
    private ClientDomainService service;
    
    @BeforeEach
    void setUp() {
        service = new ClientDomainService(repository);
    }
    
    @Test
    void shouldCreatePersonWhenEmailIsUnique() {
        when(repository.existsByEmail(any())).thenReturn(false);
        
        Person person = service.createPerson(
                ClientName.of("John"),
                Email.of("john@test.com"),
                PhoneNumber.of("+33123456789"),
                PersonBirthDate.of(LocalDate.of(1990, 1, 1))
        );
        
        assertThat(person.getName().value()).isEqualTo("John");
    }
    
    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        when(repository.existsByEmail("john@test.com")).thenReturn(true);
        
        assertThatThrownBy(() -> service.createPerson(...))
                .isInstanceOf(ClientAlreadyExistsException.class);
    }
}
```

**Benefits:**
- ✅ Fast (no Spring context)
- ✅ Focused on business logic
- ✅ Easy to test edge cases

---

### Application Service Tests (With Mocks)

```java
@ExtendWith(MockitoExtension.class)
class ClientApplicationServiceTest {
    
    @Mock
    private ClientRepository clientRepo;
    
    @Mock
    private ClientDomainService clientDomainService;
    
    @InjectMocks
    private ClientApplicationService service;
    
    @Test
    void shouldCreateAndSavePerson() {
        Person person = Person.builder()...build();
        
        when(clientDomainService.createPerson(any(), any(), any(), any()))
                .thenReturn(person);
        when(clientRepo.save(any())).thenReturn(person);
        
        Person result = service.createPerson("John", "john@test.com", "+33123", LocalDate.now());
        
        verify(clientDomainService).createPerson(any(), any(), any(), any());
        verify(clientRepo).save(person);
    }
}
```

**Benefits:**
- ✅ Tests orchestration logic
- ✅ Verifies collaboration between services
- ✅ Transaction boundaries tested via integration tests

---

## 🎯 Key Takeaways

### Responsibilities Summary

| Layer | Responsibility | Examples |
|-------|---------------|----------|
| **Application Service** | Orchestration, transactions, infrastructure | Save, transactions, DTO mapping |
| **Domain Service** | Business logic not fitting in aggregate | Entity creation, cross-aggregate validation |
| **Aggregate** | Business logic about own state | changeName(), validate own invariants |
| **Value Object** | Immutable values with validation | Email, PhoneNumber |

---

### When to Create Domain Service

✅ **Create** when:
- Entity creation has complex business rules
- Validation requires repository access
- Logic involves multiple aggregates
- Business rule reused across use cases

❌ **Don't create** when:
- Simple CRUD operations
- Logic fits in aggregate
- No business rules to enforce

---

### Current Architecture Status

**IMPLEMENTED** ✅:
- ✅ Domain Service: `ClientDomainService` (record)
- ✅ Application Services delegate to Domain Service
- ✅ Clear separation: business logic in domain, orchestration in application
- ✅ **Entity creation moved to Domain Service**
- ✅ Framework-agnostic domain layer

**Benefits**:
- ✅ Business logic centralized and reusable
- ✅ Easier to test (domain logic independent of Spring)
- ✅ Clearer responsibilities
- ✅ Better maintainability

---

## 📚 Controller Advice Best Practices

**Your Implementation**: `ClientControllerAdvice.java` is **excellent** ✅

1. **Scoped to specific controller**: `@RestControllerAdvice(assignableTypes = ClientController.class)`
2. **RFC 7807 ProblemDetail**: Standard REST error format
3. **Proper HTTP status codes**: 409 Conflict, 404 Not Found, 422 Unprocessable
4. **Structured errors**: businessKey, timestamp, traceId
5. **Appropriate logging**: DEBUG for expected business errors

**Code is expressive**: Clear method names, self-documenting, well-structured

---

## 📝 Commit Message Template

```
refactor: move entity creation to Domain Service

BREAKING CHANGE: ClientApplicationService now delegates entity creation

- Move createPerson() logic to ClientDomainService
- Move createCompany() logic to ClientDomainService
- Application Service focuses on orchestration (save, transactions)
- Domain Service handles business logic (validation, creation)
- Implemented ClientDomainService as record for immutability
- Update tests to mock ClientDomainService methods

Benefits:
- Business logic properly layered in domain
- Entity creation reusable and testable in isolation
- Clearer separation of concerns (orchestration vs business rules)
- Framework-agnostic domain layer
```

