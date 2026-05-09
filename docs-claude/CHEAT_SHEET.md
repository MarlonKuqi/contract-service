# Antisèche : Refactoring DDD en 1 Page

## 🎯 Concepts Clés

### Value Object
```java
public final class Email {
    private final String value;  // Immuable
    
    public static Email of(String raw) {
        validate(raw);  // Auto-validant
        return new Email(raw);
    }
}
```
- 🔒 Immuable (final, pas de setters)
- ✅ Auto-validant (validation au constructeur)
- ⚖️ Égalité par valeur
- 🚫 Pas d'ID

### Entity
```java
public class Person {
    @Setter
    private UUID id;  // Identité
    private final PersonBirthDate birthDate;  // Immuable
}
```
- 🆔 A une identité (ID)
- ⚖️ Égalité par ID (pas valeur)
- 🔄 Peut changer dans le temps

### Invariant
```java
// Si l'objet existe, la règle est GARANTIE vraie
Email email = Email.of("john@example.com");
// ✅ email est GARANTI valide (RFC 5321)
```

### Aggregate
```java
public class Contract {  // Aggregate Root
    private ContractPeriod period;  // Fait partie de l'aggregate
    
    public void changeCost(ContractCost newCost) {
        this.cost = newCost;
        touch();  // Cohérence garantie
    }
}
```
- 🏛️ Racine unique (Aggregate Root)
- 🔗 Cohérence interne garantie
- 💾 Sauvegarde atomique

---

## 🏗️ Architecture

```
┌─────────────────┐
│   Controller    │  ← Web/REST
├─────────────────┤
│    Service      │  ← Use Cases
├─────────────────┤
│    Domaine      │  ← Logique Métier (PURE)
├─────────────────┤
│  Infrastructure │  ← JPA/Mappers
├─────────────────┤
│      BDD        │  ← PostgreSQL
└─────────────────┘
```

**Règle** : Le domaine ne dépend de RIEN !

---

## 🔄 Flux Persistence

```
Domaine (Person)
    ↓ mapper.toJpaEntity()
JPA Entity (PersonJpaEntity)
    ↓ jpa.save()
BDD (Tables SQL)
    ↓ jpa.findById()
JPA Entity
    ↓ mapper.toDomain()
Domaine (Person)
```

---

## 📁 Structure Fichiers

```
domain/
  ├─ client/
  │   ├─ Client.java          ← Entity (pure)
  │   ├─ Person.java          ← Entity (pure)
  │   ├─ Company.java         ← Entity (pure)
  │   └─ ClientRepository.java ← Interface (pure)
  ├─ valueobject/
  │   ├─ Email.java           ← Value Object (pure)
  │   ├─ ClientName.java      ← Value Object (pure)
  │   └─ PhoneNumber.java     ← Value Object (pure)
  └─ contract/
      └─ Contract.java        ← Entity (pure)

infrastructure/
  └─ persistence/
      ├─ entity/
      │   ├─ ClientJpaEntity.java     ← JPA Entity
      │   ├─ PersonJpaEntity.java     ← JPA Entity
      │   └─ ContractJpaEntity.java   ← JPA Entity
      ├─ mapper/
      │   ├─ ClientMapper.java        ← Traducteur
      │   └─ ContractMapper.java      ← Traducteur
      ├─ ClientJpaRepository.java     ← Spring Data
      └─ JpaClientRepository.java     ← Implémentation
```

---

## ✅ Checklist Refactoring

### Domaine Pur
- [ ] Aucune annotation JPA (@Entity, @Column, etc.)
- [ ] Aucune dépendance technique (imports)
- [ ] Validation dans Value Objects
- [ ] Constructeurs privés + factory methods
- [ ] Fields final quand possible

### Infrastructure
- [ ] JPA Entities séparées
- [ ] Mappers pour conversion
- [ ] Repositories implémentent interfaces du domaine
- [ ] Pas de logique métier dans l'infra

---

## 🎓 Règles d'Or

### 1. Always Valid
```java
// Si l'objet existe, il est VALIDE
Email email = Email.of("john@example.com");
// Pas besoin de if (email.isValid())
```

### 2. Make Illegal States Unrepresentable
```java
// ❌ IMPOSSIBLE :
Email email = new Email("invalid");  // Constructeur privé

// ✅ OBLIGATOIRE :
Email email = Email.of("invalid");  // Lance exception
```

### 3. Immutability
```java
public final class Email {
    private final String value;  // Immuable
    // Pas de setter !
}
```

### 4. Fail Fast
```java
public static Email of(String raw) {
    validate(raw);  // ✅ Échoue IMMÉDIATEMENT
    return new Email(raw);
}
// Pas de validation tardive à la persistence !
```

### 5. Domain-Centric
```java
// Le domaine dicte, l'infrastructure exécute
interface ClientRepository {  // Dans le domaine
    Client save(Client c);
}

class JpaClientRepository implements ClientRepository {
    // Implémentation dans l'infrastructure
}
```

---

## 🚀 Avantages Concrets

### Testabilité
```java
// AVANT : Besoin de Spring + JPA + BDD
@SpringBootTest
void testEmail() {
    // Configuration lourde...
}

// APRÈS : Test pur, rapide
@Test
void testEmail() {
    Email email = Email.of("john@example.com");
    assertThat(email.value()).isEqualTo("john@example.com");
}
```

### Maintenance
```java
// Changer MongoDB à la place de PostgreSQL ?
// ✅ Domaine : AUCUN changement
// ✅ Infrastructure : Nouveau mapper + repo
```

### Qualité
- ✅ Moins de bugs (validation garantie)
- ✅ Code clair (Value Objects expressifs)
- ✅ Évolution facile (domaine stable)

---

## 📚 Références Rapides

- **Value Object** : Défini par valeur, immuable
- **Entity** : Défini par ID, mutable
- **Aggregate** : Groupe cohérent d'objets
- **Invariant** : Règle toujours vraie
- **Bounded Context** : Frontière du modèle
- **ACL** : Protège le domaine (Mapper)

---

## 💡 Un Exemple Complet

```java
// DOMAINE (Pure)
public final class Email {
    private final String value;
    public static Email of(String raw) {
        if (!raw.matches("...")) throw new InvalidEmailException();
        return new Email(raw);
    }
}

public class Person {
    private Email email;
    public Person(Email email) {
        if (email == null) throw new IllegalArgumentException();
        this.email = email;
    }
}

// INFRASTRUCTURE (Technique)
@Entity
public class PersonJpaEntity {
    @Column
    private String email;  // String simple
}

@Component
public class ClientMapper {
    PersonJpaEntity toJpa(Person p) {
        return new PersonJpaEntity(p.getEmail().value());
    }
    Person toDomain(PersonJpaEntity e) {
        return new Person(Email.of(e.getEmail()));
    }
}
```

**Voilà ! C'est tout ce qu'il faut savoir.** 🎯

