# DDD Immutability & Validation - Decision Analysis

## Article de référence
https://medium.com/unil-ci-software-engineering/clean-ddd-lessons-validation-and-immutability-a82292ba2a93

## Principes clés de l'article

### 1. **Immutability First**
> "Entities should be immutable. Instead of modifying an entity, create a new one."

### 2. **Self-Validation**
> "Value Objects and Entities should validate themselves in their constructor."

### 3. **Factory Pattern**
> "Use factories to encapsulate complex creation logic and validation."

### 4. **Fail Fast**
> "Throw exceptions in constructors if validation fails - never allow invalid objects to exist."

## État actuel du code

### ✅ Ce qui respecte l'article

1. **Value Objects immutables et auto-validants**
   - `ClientName`, `Email`, `PhoneNumber`, `PersonBirthDate`, `CompanyIdentifier`
   - Tous immutables (`final` fields)
   - Validation dans le constructeur

2. **Validation dans les constructeurs d'entités**
   - `Client`, `Person`, `Company` valident à la création
   - Exceptions levées si invalide

3. **Sealed classes pour polymorphisme contrôlé**
   - `sealed class Client permits Person, Company`

### ❌ Ce qui ne respecte PAS l'article

1. **Entités MUTABLES**
   - Champs non-final : `id`, `name`, `email`, `phone`
   - Méthodes de mutation : `updateCommonFields()`, `changeName()`, `changeEmail()`, `changePhone()`
   
2. **Pas de Factory**
   - Utilisation de builders manuels
   - Logique de création dispersée

3. **Lombok @SuperBuilder incompatible avec validation**
   - Impossible d'utiliser `@SuperBuilder` car il contourne le constructeur de validation

## Options possibles

### Option 1 : DDD Pur (Article à 100%)

**Approche** : Immutabilité totale + Factory

```java
@Getter
public abstract sealed class Client permits Person, Company {
    private final UUID id;           // FINAL
    private final ClientName name;   // FINAL
    private final Email email;       // FINAL
    private final PhoneNumber phone; // FINAL
    
    // Pas de méthodes de mutation
}
```

**Avantages** :
- ✅ Respect total du DDD
- ✅ Thread-safe par défaut
- ✅ Pas de risque d'état incohérent

**Inconvénients** :
- ❌ Incompatible avec JPA/Hibernate (nécessite setters)
- ❌ Nécessite une refonte complète de la couche infrastructure
- ❌ Les mappers JPA devront recréer des instances

### Option 2 : Hybride - Domain pur + Infrastructure pragmatique

**Approche** : Domain immutable, mais couche infrastructure avec mutabilité

```java
// Domain : Client.java (immutable)
@Getter
public abstract sealed class Client {
    private final UUID id;
    private final ClientName name;
    // ...
}

// Infrastructure : ClientEntity.java (mutable pour JPA)
@Entity
public class ClientEntity {
    @Id
    private UUID id;
    private String name;  // String, pas ClientName
    
    // Getters/Setters pour JPA
}

// Mapping explicite Domain <-> Infrastructure
```

**Avantages** :
- ✅ Respect du DDD dans le domaine
- ✅ Pragmatique avec les contraintes techniques
- ✅ Séparation claire Domain/Infrastructure

**Inconvénients** :
- ⚠️ Duplication des modèles
- ⚠️ Mappers à maintenir

### Option 3 : Pragmatique - Validation forte, mutabilité contrôlée (ACTUEL)

**Approche** : Validation stricte + méthodes de mutation validées

```java
public abstract sealed class Client {
    private UUID id;              // Non-final
    private ClientName name;      // Non-final
    
    // Validation dans constructeur
    protected Client(...) {
        validate();
    }
    
    // Méthodes de mutation avec validation
    public void changeName(ClientName name) {
        validate(name);
        this.name = name;
    }
}
```

**Avantages** :
- ✅ Compatible JPA/Hibernate
- ✅ Validation garantie
- ✅ Simple à maintenir

**Inconvénients** :
- ❌ Pas "DDD pur"
- ❌ Entités mutables
- ⚠️ Risque de mutations non contrôlées

### Option 4 : Lombok @Value + Factory (Compromis)

**Approche** : Utiliser `@Value` Lombok pour immutabilité + Factory

```java
@Value
@Builder
public class Person extends Client {
    UUID id;
    ClientName name;
    Email email;
    PhoneNumber phone;
    PersonBirthDate birthDate;
    
    // Factory valide et crée
    public static class Factory {
        public static Person create(...) {
            validate();
            return Person.builder()...build();
        }
    }
}
```

**Problème** : `@Value` incompatible avec héritage et sealed classes

## Recommandation finale

### Pour votre projet : **Option 2 modifiée**

**Garder le code actuel MAIS améliorer avec :**

1. **Ajouter une Factory pour centraliser la création**
   ```java
   public final class ClientFactory {
       public static Person createPerson(...) { }
       public static Company createCompany(...) { }
   }
   ```

2. **Rendre les champs `final` quand possible**
   - `birthDate` et `companyIdentifier` sont déjà final ✅
   - Garder `id`, `name`, `email`, `phone` mutables pour JPA

3. **Documenter la raison de la mutabilité**
   ```java
   // Note: Fields are mutable for JPA/Hibernate compatibility
   // Mutations are controlled through validated methods only
   private UUID id;
   ```

4. **Garder le builder manuel** (pas Lombok @SuperBuilder car incompatible)

5. **Ajouter des tests de validation** pour garantir qu'aucune mutation invalide n'est possible

## Conclusion

**Le DDD pur de l'article est idéal dans un monde sans contraintes techniques.**

Dans votre cas avec JPA/Hibernate, un **compromis pragmatique** est acceptable SI :
- ✅ Validation stricte partout
- ✅ Value Objects immutables
- ✅ Méthodes de mutation explicites et validées
- ✅ Factory pour encapsuler la création
- ✅ Documentation claire des choix

**"Perfect is the enemy of good"** - Votre code actuel avec une Factory serait un excellent équilibre DDD/Pragmatisme.

