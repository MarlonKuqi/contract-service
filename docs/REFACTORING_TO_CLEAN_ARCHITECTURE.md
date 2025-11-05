# Guide de Refactoring vers Clean Architecture

## 🎯 Objectif
Transformer le projet d'une "architecture en couches avec annotations JPA dans le domain" vers une vraie Clean Architecture.

---

## 📦 Avant/Après

### ❌ AVANT (situation actuelle)

```java
// domain/client/Person.java
package com.mk.contractservice.domain.client;

import jakarta.persistence.*;  // ← PROBLÈME : dépendance infrastructure dans domain
import jakarta.validation.*;

@Entity  // ← JPA dans le domain
@Table(name = "person")
@DiscriminatorValue("PERSON")
public class Person extends Client {
    
    @Embedded  // ← JPA
    @NotNull
    @Valid
    private final PersonBirthDate birthDate;
    
    // ...
}
```

### ✅ APRÈS (Clean Architecture)

```java
// domain/client/Person.java
package com.mk.contractservice.domain.client;

// Aucun import de framework !
public final class Person extends Client {
    
    private final PersonBirthDate birthDate;
    
    public Person(ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
        super(name, email, phone);
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date must not be null");
        }
        this.birthDate = birthDate;
    }
    
    public PersonBirthDate getBirthDate() {
        return birthDate;
    }
}
```

```java
// infrastructure/persistence/jpa/PersonJpaEntity.java
package com.mk.contractservice.infrastructure.persistence.jpa;

import jakarta.persistence.*;  // ← JPA reste dans l'infrastructure

@Entity
@Table(name = "person", schema = "contracts")
@DiscriminatorValue("PERSON")
@PrimaryKeyJoinColumn(name = "id")
class PersonJpaEntity extends ClientJpaEntity {
    
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;
    
    // Getters/Setters (package-private)
    
    LocalDate getBirthDate() {
        return birthDate;
    }
    
    void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
}
```

```java
// infrastructure/persistence/jpa/PersonMapper.java
package com.mk.contractservice.infrastructure.persistence.jpa;

import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.valueobject.*;

class PersonMapper {
    
    public Person toDomain(PersonJpaEntity entity) {
        return new Person(
            ClientName.of(entity.getName()),
            Email.of(entity.getEmail()),
            PhoneNumber.of(entity.getPhone()),
            PersonBirthDate.of(entity.getBirthDate())
        );
    }
    
    public PersonJpaEntity toEntity(Person domain, UUID id) {
        PersonJpaEntity entity = new PersonJpaEntity();
        entity.setId(id);
        entity.setName(domain.getName().value());
        entity.setEmail(domain.getEmail().value());
        entity.setPhone(domain.getPhone().value());
        entity.setBirthDate(domain.getBirthDate().value());
        return entity;
    }
    
    public void updateEntity(PersonJpaEntity entity, Person domain) {
        entity.setName(domain.getName().value());
        entity.setEmail(domain.getEmail().value());
        entity.setPhone(domain.getPhone().value());
        // Note: birthDate est immuable
    }
}
```

---

## 🔄 Plan de Refactoring

### Phase 1 : Value Objects (1-2 jours)

#### Étape 1.1 : Email

**AVANT :**
```java
@Embeddable
public final class Email {
    @Column(name = "email", nullable = false, length = 254)
    private final String value;
    // ...
}
```

**APRÈS :**
```java
// domain/valueobject/Email.java - AUCUNE annotation
public final class Email {
    private final String value;
    
    private Email(String value) {
        this.value = value;
    }
    
    public static Email of(String rawValue) {
        String normalized = normalize(rawValue);
        validate(normalized, rawValue);
        return new Email(normalized);
    }
    
    private static String normalize(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or blank");
        }
        return rawValue.trim().toLowerCase(Locale.ROOT);
    }
    
    private static void validate(String normalizedValue, String rawValue) {
        if (normalizedValue.length() > 254) {
            throw new IllegalArgumentException("Email too long (max 254 characters per RFC 5321)");
        }
        if (!normalizedValue.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Invalid email format: " + rawValue);
        }
    }
    
    public String value() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Email other && Objects.equals(value, other.value));
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
```

Répéter pour : `PhoneNumber`, `ClientName`, `ContractCost`, `ContractPeriod`, etc.

---

### Phase 2 : Entités Domain (2-3 jours)

#### Étape 2.1 : Client (abstract)

**APRÈS :**
```java
// domain/client/Client.java - PURE
package com.mk.contractservice.domain.client;

import com.mk.contractservice.domain.valueobject.*;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Client {
    
    private ClientName name;
    private Email email;
    private PhoneNumber phone;
    
    protected Client(ClientName name, Email email, PhoneNumber phone) {
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null");
        }
        if (email == null) {
            throw new IllegalArgumentException("Email must not be null");
        }
        if (phone == null) {
            throw new IllegalArgumentException("Phone must not be null");
        }
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
    
    public ClientName getName() {
        return name;
    }
    
    public Email getEmail() {
        return email;
    }
    
    public PhoneNumber getPhone() {
        return phone;
    }
    
    public void updateCommonFields(ClientName name, Email email, PhoneNumber phone) {
        if (name == null || email == null || phone == null) {
            String nullFields = Stream.of(
                name == null ? "name" : null,
                email == null ? "email" : null,
                phone == null ? "phone" : null
            ).filter(Objects::nonNull).collect(Collectors.joining(", "));
            
            throw new IllegalArgumentException(
                "Cannot update client: the following required fields are null: " + nullFields
            );
        }
        
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
}
```

#### Étape 2.2 : Contract

**APRÈS :**
```java
// domain/contract/Contract.java - PURE
package com.mk.contractservice.domain.contract;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.exception.InvalidContractException;
import com.mk.contractservice.domain.valueobject.*;
import java.time.OffsetDateTime;

public class Contract {
    
    private final Client client;
    private ContractPeriod period;
    private ContractCost costAmount;
    private OffsetDateTime lastModified;
    
    public Contract(Client client, ContractPeriod period, ContractCost costAmount) {
        if (client == null) {
            throw InvalidContractException.forNullClient();
        }
        if (period == null) {
            throw InvalidContractException.forNullPeriod();
        }
        if (costAmount == null) {
            throw InvalidContractException.forNullCostAmount();
        }
        this.client = client;
        this.period = period;
        this.costAmount = costAmount;
        this.lastModified = OffsetDateTime.now();
    }
    
    public void changeCost(ContractCost newAmount) {
        if (newAmount == null) {
            throw InvalidContractException.forNullNewCostAmount();
        }
        this.costAmount = newAmount;
        this.lastModified = OffsetDateTime.now();
    }
    
    // Getters uniquement (pas de setters)
    public Client getClient() { return client; }
    public ContractPeriod getPeriod() { return period; }
    public ContractCost getCostAmount() { return costAmount; }
    public OffsetDateTime getLastModified() { return lastModified; }
}
```

---

### Phase 3 : Entités JPA dans Infrastructure (3-4 jours)

#### Étape 3.1 : Créer ClientJpaEntity

```java
// infrastructure/persistence/jpa/entity/ClientJpaEntity.java
package com.mk.contractservice.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "client", schema = "contracts")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
abstract class ClientJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;
    
    @Column(name = "phone", nullable = false)
    private String phone;
    
    // Package-private getters/setters
    UUID getId() { return id; }
    void setId(UUID id) { this.id = id; }
    
    String getName() { return name; }
    void setName(String name) { this.name = name; }
    
    String getEmail() { return email; }
    void setEmail(String email) { this.email = email; }
    
    String getPhone() { return phone; }
    void setPhone(String phone) { this.phone = phone; }
}
```

#### Étape 3.2 : ContractJpaEntity

```java
// infrastructure/persistence/jpa/entity/ContractJpaEntity.java
package com.mk.contractservice.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contract", schema = "contracts")
class ContractJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientJpaEntity client;
    
    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;
    
    @Column(name = "end_date", nullable = false)
    private OffsetDateTime endDate;
    
    @Column(name = "cost_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal costAmount;
    
    @Column(name = "last_modified", nullable = false)
    private OffsetDateTime lastModified;
    
    @PrePersist
    @PreUpdate
    void touch() {
        this.lastModified = OffsetDateTime.now();
    }
    
    // Package-private getters/setters
    UUID getId() { return id; }
    void setId(UUID id) { this.id = id; }
    
    ClientJpaEntity getClient() { return client; }
    void setClient(ClientJpaEntity client) { this.client = client; }
    
    OffsetDateTime getStartDate() { return startDate; }
    void setStartDate(OffsetDateTime startDate) { this.startDate = startDate; }
    
    OffsetDateTime getEndDate() { return endDate; }
    void setEndDate(OffsetDateTime endDate) { this.endDate = endDate; }
    
    BigDecimal getCostAmount() { return costAmount; }
    void setCostAmount(BigDecimal costAmount) { this.costAmount = costAmount; }
    
    OffsetDateTime getLastModified() { return lastModified; }
}
```

---

### Phase 4 : Mappers (2 jours)

#### Étape 4.1 : ContractMapper

```java
// infrastructure/persistence/jpa/mapper/ContractMapper.java
package com.mk.contractservice.infrastructure.persistence.jpa.mapper;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.valueobject.*;
import com.mk.contractservice.infrastructure.persistence.jpa.entity.*;
import org.springframework.stereotype.Component;

@Component
class ContractMapper {
    
    private final ClientMapper clientMapper;
    
    ContractMapper(ClientMapper clientMapper) {
        this.clientMapper = clientMapper;
    }
    
    public Contract toDomain(ContractJpaEntity entity, UUID entityId) {
        // Créer le domain Contract
        Contract contract = new Contract(
            clientMapper.toDomain(entity.getClient(), entity.getClient().getId()),
            ContractPeriod.of(entity.getStartDate(), entity.getEndDate()),
            ContractCost.of(entity.getCostAmount())
        );
        
        // Utiliser la réflexion pour set l'ID (ou créer un constructeur package-private)
        // Alternative : ajouter une méthode withId() dans Contract
        return contract;
    }
    
    public ContractJpaEntity toEntity(Contract domain, UUID id, ClientJpaEntity clientEntity) {
        ContractJpaEntity entity = new ContractJpaEntity();
        entity.setId(id);
        entity.setClient(clientEntity);
        updateEntity(entity, domain);
        return entity;
    }
    
    public void updateEntity(ContractJpaEntity entity, Contract domain) {
        entity.setStartDate(domain.getPeriod().startDate());
        entity.setEndDate(domain.getPeriod().endDate());
        entity.setCostAmount(domain.getCostAmount().value());
    }
}
```

---

### Phase 5 : Repository Implementation (2 jours)

#### Étape 5.1 : JpaContractRepository refactorisé

```java
// infrastructure/persistence/jpa/JpaContractRepository.java
package com.mk.contractservice.infrastructure.persistence.jpa;

import com.mk.contractservice.domain.contract.Contract;
import com.mk.contractservice.domain.contract.ContractRepository;
import com.mk.contractservice.infrastructure.persistence.jpa.entity.*;
import com.mk.contractservice.infrastructure.persistence.jpa.mapper.*;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaContractRepository implements ContractRepository {
    
    private final ContractJpaRepository jpaRepo;
    private final ClientJpaRepository clientJpaRepo;
    private final ContractMapper mapper;
    private final ClientMapper clientMapper;
    
    JpaContractRepository(
        ContractJpaRepository jpaRepo,
        ClientJpaRepository clientJpaRepo,
        ContractMapper mapper,
        ClientMapper clientMapper
    ) {
        this.jpaRepo = jpaRepo;
        this.clientJpaRepo = clientJpaRepo;
        this.mapper = mapper;
        this.clientMapper = clientMapper;
    }
    
    @Override
    public Optional<Contract> findById(UUID id) {
        return jpaRepo.findById(id)
            .map(entity -> mapper.toDomain(entity, entity.getId()));
    }
    
    @Override
    public Contract save(Contract contract) {
        // 1. Sauvegarder le client d'abord
        UUID clientId = getClientId(contract.getClient());
        ClientJpaEntity clientEntity = clientJpaRepo.findById(clientId)
            .orElseThrow(() -> new IllegalStateException("Client must be saved first"));
        
        // 2. Gérer create vs update
        UUID contractId = getContractId(contract);
        
        ContractJpaEntity entity;
        if (contractId == null) {
            // Nouveau contrat
            entity = mapper.toEntity(contract, UUID.randomUUID(), clientEntity);
        } else {
            // Update
            entity = jpaRepo.findById(contractId)
                .orElseThrow(() -> new IllegalStateException("Contract not found"));
            mapper.updateEntity(entity, contract);
        }
        
        ContractJpaEntity saved = jpaRepo.save(entity);
        return mapper.toDomain(saved, saved.getId());
    }
    
    @Override
    public void deleteById(UUID id) {
        jpaRepo.deleteById(id);
    }
    
    // Helpers pour extraire les IDs (via réflexion ou méthode exposée)
    private UUID getClientId(Client client) {
        // Option 1 : Réflexion
        // Option 2 : Client expose getId() (mais avec visibilité package)
        // Option 3 : Utiliser un IdentifiedClient wrapper
        throw new UnsupportedOperationException("À implémenter selon votre choix");
    }
    
    private UUID getContractId(Contract contract) {
        // Idem
        throw new UnsupportedOperationException("À implémenter selon votre choix");
    }
}
```

---

## 🔧 Problème : Gestion des IDs

### Challenge
Le domain pur ne devrait pas exposer d'ID technique (UUID généré par la BDD).

### Solutions

#### Option 1 : ID dans le domain (pragmatique)
```java
// domain/contract/Contract.java
public class Contract {
    private UUID id;  // Nullable au départ
    
    // Méthode package-private pour l'infra
    void setId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("ID already set");
        }
        this.id = id;
    }
    
    public UUID getId() {
        return id;
    }
}
```

#### Option 2 : Wrapper dans l'infrastructure (puriste)
```java
// infrastructure/persistence/ContractWithId.java
record ContractWithId(UUID id, Contract contract) {}

// Le repository retourne ContractWithId
public interface ContractRepository {
    Optional<ContractWithId> findById(UUID id);
    ContractWithId save(Contract contract);
}
```

#### Option 3 : Identity Map dans le repository
```java
// infrastructure/persistence/jpa/JpaContractRepository.java
private final Map<Contract, UUID> identityMap = new WeakHashMap<>();

@Override
public Contract save(Contract contract) {
    UUID id = identityMap.get(contract);
    // ...
    identityMap.put(contract, savedId);
    return contract;
}
```

---

## ✅ Checklist de Migration

### Phase 1 : Value Objects
- [ ] Email sans annotations
- [ ] PhoneNumber sans annotations
- [ ] ClientName sans annotations
- [ ] ContractCost sans annotations
- [ ] ContractPeriod sans annotations
- [ ] PersonBirthDate sans annotations
- [ ] CompanyIdentifier sans annotations

### Phase 2 : Entités Domain
- [ ] Client sans annotations
- [ ] Person sans annotations
- [ ] Company sans annotations
- [ ] Contract sans annotations

### Phase 3 : Entités JPA
- [ ] ClientJpaEntity créée
- [ ] PersonJpaEntity créée
- [ ] CompanyJpaEntity créée
- [ ] ContractJpaEntity créée

### Phase 4 : Mappers
- [ ] ClientMapper
- [ ] PersonMapper
- [ ] CompanyMapper
- [ ] ContractMapper

### Phase 5 : Repositories
- [ ] JpaClientRepository refactorisé
- [ ] JpaContractRepository refactorisé

### Phase 6 : Tests
- [ ] Tests unitaires domain toujours verts
- [ ] Tests d'intégration adaptés
- [ ] Tests de mapping

### Phase 7 : Application Layer
- [ ] ClientApplicationService adapté
- [ ] ContractApplicationService adapté

---

## 🎓 Avantages Obtenus

Après le refactoring :

✅ **Domain 100% pur**
- Aucune dépendance à un framework
- Testable sans infrastructure
- Réutilisable dans n'importe quel contexte

✅ **Migration facilitée**
```bash
# Changer PostgreSQL → MongoDB
# Avant : 3 semaines
# Après : 2 jours (juste les entités JPA → Document)
```

✅ **Tests ultra-rapides**
```bash
# Tests domain avant : 2s (Spring context)
# Tests domain après : 0.1s (POJO purs)
```

✅ **Flexibilité**
- Ajouter une API GraphQL sans toucher le domain
- Migrer vers Event Sourcing possible
- Changer de framework (Spring → Quarkus) : 1 semaine au lieu de 1 mois

---

## 📚 Ressources

- Robert C. Martin - Clean Architecture
- Vaughn Vernon - Implementing Domain-Driven Design
- ArchUnit - Pour enforcer les règles d'architecture


