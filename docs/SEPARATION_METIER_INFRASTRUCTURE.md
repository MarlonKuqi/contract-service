# Séparation Métier/Infrastructure : Concept Expliqué

## 🎯 Le Principe Fondamental

**Clean Architecture = Séparer MÉTIER et TECHNIQUE**

```
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN (Métier)                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Person                                                │  │
│  │ ├─ EmailValue (validation RFC 5321)                  │  │
│  │ ├─ PhoneNumber (validation internationale)           │  │
│  │ ├─ PersonBirthDate (doit être > 18 ans)              │  │
│  │ └─ Règles métier complexes                           │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  💡 Aucune dépendance technique                            │
│  💡 Peut tourner dans n'importe quel contexte              │
│  💡 Tests ultra-rapides (POJO purs)                        │
└─────────────────────────────────────────────────────────────┘
                             ↕
                        Mapper (traducteur)
                             ↕
┌─────────────────────────────────────────────────────────────┐
│                INFRASTRUCTURE (Technique)                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ PersonJpaEntity (@Entity)                            │  │
│  │ ├─ String email (format BDD)                         │  │
│  │ ├─ String phone (format BDD)                         │  │
│  │ ├─ LocalDate birthDate (type SQL)                    │  │
│  │ └─ Annotations JPA pour Hibernate                    │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  💾 Dépend de JPA, Hibernate, PostgreSQL                   │
│  💾 Optimisé pour le stockage                              │
│  💾 Changeable sans impacter le métier                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Exemple Concret : Migration PostgreSQL → MongoDB

### Scénario : Votre entreprise décide de passer à MongoDB

#### ❌ AVANT (situation actuelle - JPA dans le domain)

```java
// domain/Person.java - COUPLÉ à JPA
@Entity  // ← MongoDB ne connaît pas @Entity
@Table(name = "person")  // ← MongoDB n'a pas de tables
public class Person {
    @Embedded  // ← MongoDB n'utilise pas @Embedded
    private PersonBirthDate birthDate;
}

// Résultat : IL FAUT MODIFIER LE DOMAIN 😱
// Temps : 2-3 semaines
```

**Fichiers à modifier :**
- ✏️ `domain/Person.java` (changer @Entity en @Document)
- ✏️ `domain/Company.java` (idem)
- ✏️ `domain/Contract.java` (idem + refactorer @ManyToOne)
- ✏️ `domain/valueobject/Email.java` (changer @Embeddable)
- ✏️ `domain/valueobject/ContractCost.java` (idem)
- ✏️ Tous les autres value objects
- ✏️ Refactorer l'héritage (MongoDB ne supporte pas les jointures)
- ✏️ Réécrire les tests qui utilisent les entités

**Total : ~15-20 fichiers domain modifiés** 😱

---

#### ✅ APRÈS (Clean Architecture - domain pur)

```java
// domain/Person.java - AUCUNE annotation
public final class Person {
    private final PersonBirthDate birthDate;  // Value object pur
    // Pas d'annotation !
}
```

**Fichiers à modifier :**
- ❌ `domain/Person.java` → **AUCUN changement** ✅
- ❌ `domain/valueobject/Email.java` → **AUCUN changement** ✅
- ✏️ `infrastructure/persistence/jpa/PersonJpaEntity.java` → **SUPPRIMÉ**
- ✅ `infrastructure/persistence/mongo/PersonDocument.java` → **CRÉÉ**

```java
// NOUVEAU : infrastructure/persistence/mongo/PersonDocument.java
@Document(collection = "persons")
public class PersonDocument {
    @Id
    private String id;
    
    @Field("email")
    private String email;  // String plat
    
    @Field("phone")
    private String phone;  // String plat
    
    @Field("birth_date")
    private LocalDate birthDate;  // LocalDate plat
}
```

```java
// NOUVEAU : infrastructure/persistence/mongo/PersonMapper.java
@Component
class PersonMongoMapper {
    public Person toDomain(PersonDocument doc) {
        return new Person(
            ClientName.of(doc.getName()),
            EmailValue.of(doc.getEmail()),  // String → EmailValue
            PhoneNumber.of(doc.getPhone()),
            PersonBirthDate.of(doc.getBirthDate())
        );
    }
    
    public PersonDocument toDocument(Person domain) {
        PersonDocument doc = new PersonDocument();
        doc.setEmail(domain.getEmail().value());  // EmailValue → String
        doc.setPhone(domain.getPhone().value());
        doc.setBirthDate(domain.getBirthDate().value());
        return doc;
    }
}
```

**Total : 0 fichiers domain modifiés, 2-3 fichiers infrastructure créés** 🎉

---

## 🎯 "Duplication" des Valeurs : Pourquoi c'est OK

### Question : N'est-ce pas une violation de DRY ?

```
Domain                       Infrastructure
━━━━━━━━━━━━━━━━━━━━━       ━━━━━━━━━━━━━━━━━━━━━━━━━━━
EmailValue                   PersonJpaEntity
├─ String value             ├─ String email
└─ validate()               └─ (pas de validation)

PhoneNumber                  PersonJpaEntity
├─ String value             ├─ String phone
└─ validate()               └─ (pas de validation)
```

### Réponse : NON, ce n'est PAS de la duplication !

**Pourquoi ?**

1. **Responsabilités différentes**
   - `EmailValue` : **Valider** et **encapsuler** la logique métier
   - `String email` dans JPA : **Stocker** une valeur validée

2. **La complexité métier reste unique**
   ```java
   // La validation est UNIQUE dans EmailValue
   EmailValue.of("invalid") → ❌ Exception
   
   // JPA reçoit une valeur déjà validée
   entity.setEmail("valid@example.com") → ✅ OK (pas de re-validation)
   ```

3. **DRY s'applique à la LOGIQUE, pas aux DONNÉES**
   - ❌ Violation DRY : Copier la validation email dans PersonJpaEntity
   - ✅ OK : Avoir `String email` dans JPA qui reçoit `EmailValue.value()`

---

## 💡 Analogie : Traducteur Français ↔ Anglais

```
┌──────────────────────┐         ┌──────────────────────┐
│   Pensée (Domain)    │         │   Parole (Infra)     │
│                      │         │                      │
│  Concept: "Bonjour"  │ ─────→  │  Mot: "Hello"        │
│  Concept: "Maison"   │ ─────→  │  Mot: "House"        │
│                      │         │                      │
│  Logique complexe    │         │  Format simple       │
│  Invariants métier   │         │  Optimisé stockage   │
└──────────────────────┘         └──────────────────────┘
```

- Le **concept** (domain) existe indépendamment du langage
- Le **mot** (infrastructure) est lié à un langage spécifique (SQL, MongoDB, etc.)
- Si vous changez de langue (PostgreSQL → MongoDB), vous changez les mots, pas les concepts

---

## 📦 Exemple Complet : ContractCost

### Domain (métier pur)

```java
// domain/valueobject/ContractCost.java
public final class ContractCost {
    private final BigDecimal value;
    
    private ContractCost(BigDecimal value) {
        this.value = value;
    }
    
    public static ContractCost of(BigDecimal rawValue) {
        validate(rawValue);
        return new ContractCost(rawValue);
    }
    
    private static void validate(BigDecimal rawValue) {
        if (rawValue == null) {
            throw new IllegalArgumentException("Cost must not be null");
        }
        if (rawValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cost must not be negative: " + rawValue);
        }
        if (rawValue.scale() > 2) {
            throw new IllegalArgumentException("Cost must have at most 2 decimal places");
        }
    }
    
    public BigDecimal value() {
        return value;
    }
    
    // Business logic
    public ContractCost add(ContractCost other) {
        return ContractCost.of(this.value.add(other.value));
    }
    
    public ContractCost multiplyBy(BigDecimal factor) {
        return ContractCost.of(this.value.multiply(factor).setScale(2, RoundingMode.HALF_UP));
    }
}
```

### Infrastructure PostgreSQL

```java
// infrastructure/persistence/jpa/entity/ContractJpaEntity.java
@Entity
@Table(name = "contract")
class ContractJpaEntity {
    
    @Column(name = "cost_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal costAmount;  // Valeur brute, pas de logique
    
    // Getters/Setters simples
    BigDecimal getCostAmount() { return costAmount; }
    void setCostAmount(BigDecimal cost) { this.costAmount = cost; }
}
```

### Infrastructure MongoDB (si migration)

```java
// infrastructure/persistence/mongo/ContractDocument.java
@Document(collection = "contracts")
class ContractDocument {
    
    @Field("cost_amount")
    private Decimal128 costAmount;  // Type MongoDB
    
    // Getters/Setters
    Decimal128 getCostAmount() { return costAmount; }
    void setCostAmount(Decimal128 cost) { this.costAmount = cost; }
}
```

### Mapper (traducteur)

```java
// PostgreSQL
entity.setCostAmount(domain.getCostAmount().value());  // ContractCost → BigDecimal

// MongoDB
doc.setCostAmount(new Decimal128(domain.getCostAmount().value()));  // ContractCost → Decimal128
```

**Remarquez :** 
- ✅ Domain `ContractCost` **INCHANGÉ**
- ✏️ Infrastructure adaptée (BigDecimal vs Decimal128)

---

## 🎯 Résumé : Métier vs Infrastructure

| Aspect | Domain (Métier) | Infrastructure (Technique) |
|--------|----------------|---------------------------|
| **Contenu** | Logique complexe, validations, invariants | Valeurs plates, types BDD |
| **Dépendances** | AUCUNE (POJO pur) | JPA, Hibernate, Spring, etc. |
| **Évolution** | Stable (métier change peu) | Variable (technos évoluent) |
| **Tests** | Ultra-rapides (ms) | Plus lents (context Spring) |
| **Réutilisable** | Oui (CLI, API, batch, etc.) | Non (lié à la BDD) |
| **Exemple** | `EmailValue.validate()` | `String email` dans JPA |

---

## ✅ Pourquoi c'est Génial

### 1. Changement de BDD : Impact Limité

```diff
Domain:
  ✅ Person.java          (unchanged)
  ✅ EmailValue.java      (unchanged)
  ✅ Contract.java        (unchanged)

Infrastructure:
- ❌ PersonJpaEntity.java   (deleted)
- ❌ JpaRepository.java     (deleted)
+ ✅ PersonDocument.java    (created)
+ ✅ MongoRepository.java   (created)
```

**Temps :** 2-3 jours au lieu de 2-3 semaines

### 2. Domain Testable Partout

```java
// Tests sans base de données
@Test
void shouldValidateEmail() {
    assertThatThrownBy(() -> EmailValue.of("invalid"))
        .isInstanceOf(IllegalArgumentException.class);
}

// Temps d'exécution : < 1ms
```

### 3. Réutilisabilité du Métier

```java
// Même domain utilisable dans :
// ✅ API REST (Spring Boot)
// ✅ API GraphQL
// ✅ CLI tool (migration de données)
// ✅ Batch job (calcul de coûts)
// ✅ Lambda AWS (serverless)

// Toujours le MÊME code métier !
```

---

## 🎬 Conclusion

### Vous avez dit :

> *"On aurait du code dupliqué, tant pis pour DRY. Mais en cas de changement de DB, on modifierait les entités qui contiennent directement les valeurs des value objects. Notre domain contiendrait la complexité métier. C'est une séparation métier/infra."*

### Ma réponse :

**PARFAIT ! Vous avez 100% compris !** 🎉

- ✅ Ce n'est **PAS du code dupliqué** (responsabilités différentes)
- ✅ En cas de migration BDD : **seule l'infra change**
- ✅ Le domain contient **toute la complexité métier**
- ✅ C'est **exactement** le principe de Clean Architecture

**Vous êtes prêt à implémenter !** 🚀

---

**Date :** 2025-10-31  
**Concept validé par :** GitHub Copilot  
**Niveau de compréhension :** Expert 🏆

