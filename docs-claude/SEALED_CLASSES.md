# Sealed Classes : Client comme Type Algébrique

## 🔒 Pourquoi `sealed` ?

### Avant (abstract)
```java
public abstract class Client {
    // ...
}

// N'importe qui peut créer un sous-type !
public class VIPClient extends Client { } // ❌ Non désiré
```

### Après (sealed)
```java
public abstract sealed class Client permits Person, Company {
    // ...
}

public final class Person extends Client { }
public final class Company extends Client { }

// ❌ Impossible de faire ça :
public class VIPClient extends Client { } // Erreur de compilation !
```

---

## 🎯 Avantages Concrets

### 1. Switch Exhaustif (sans default)

```java
// ✅ AVEC sealed class
public ClientJpaEntity toJpaEntity(Client domain) {
    return switch (domain) {
        case Person person -> mapPerson(person);
        case Company company -> mapCompany(company);
        // Pas de default ! Le compilateur garantit l'exhaustivité
    };
}

// Si on ajoute un nouveau type de Client (ex: Organization),
// le compilateur FORCE à gérer ce cas partout où on fait un switch !
```

**Comparaison AVANT** :
```java
// ❌ SANS sealed class
public ClientJpaEntity toJpaEntity(Client domain) {
    if (domain instanceof Person person) {
        return mapPerson(person);
    } else if (domain instanceof Company company) {
        return mapCompany(company);
    }
    throw new IllegalArgumentException("Unknown type"); // ⚠️ Runtime error !
}
```

### 2. Protection contre les Extensions Non Désirées

```java
// Dans le domaine, on veut UNIQUEMENT Person et Company
// sealed garantit ça au niveau du compilateur !

public abstract sealed class Client permits Person, Company {
    // Le contrat est clair : Client = Person OU Company, rien d'autre !
}
```

### 3. Expressivité du Domaine

```java
// Le code exprime clairement le modèle métier :
// "Un Client est soit une Person, soit une Company"

// C'est un Type Algébrique (Algebraic Data Type)
// Client = Person | Company
```

---

## 📊 Exemple Complet dans notre Code

### Domaine (sealed)
```java
public abstract sealed class Client permits Person, Company {
    @Setter
    private UUID id;
    private ClientName name;
    private Email email;
    private PhoneNumber phone;
}

public final class Person extends Client {
    private final PersonBirthDate birthDate;
}

public final class Company extends Client {
    private final CompanyIdentifier companyIdentifier;
}
```

### Mapper avec Switch Expressif
```java
@Component
public class ClientMapper {
    
    public ClientJpaEntity toJpaEntity(Client domain) {
        if (domain == null) return null;
        
        return switch (domain) {
            case Person person -> {
                PersonJpaEntity entity = new PersonJpaEntity(
                    person.getName().value(),
                    person.getEmail().value(),
                    person.getPhone().value(),
                    person.getBirthDate().value()
                );
                entity.setId(person.getId());
                yield entity;
            }
            case Company company -> {
                CompanyJpaEntity entity = new CompanyJpaEntity(
                    company.getName().value(),
                    company.getEmail().value(),
                    company.getPhone().value(),
                    company.getCompanyIdentifier().value()
                );
                entity.setId(company.getId());
                yield entity;
            }
            // Pas de default ! Exhaustivité garantie par sealed
        };
    }
}
```

---

## 💡 Quand Utiliser `sealed` ?

### ✅ Utiliser sealed quand :
1. **Hiérarchie fermée** : Vous connaissez tous les sous-types possibles
2. **Domaine métier strict** : "Un Client est EXACTEMENT Person OU Company"
3. **Pattern matching** : Vous voulez des switch exhaustifs
4. **Type Safety** : Empêcher des extensions non contrôlées

### ❌ Ne PAS utiliser sealed quand :
1. **Hiérarchie ouverte** : Vous voulez permettre des extensions futures
2. **Plugins** : D'autres modules doivent pouvoir étendre la classe
3. **Framework** : La classe est destinée à être étendue par les utilisateurs

---

## 🎓 Comparaison avec d'autres Langages

### Kotlin
```kotlin
sealed class Client {
    data class Person(val birthDate: LocalDate) : Client()
    data class Company(val identifier: String) : Client()
}

fun map(client: Client) = when(client) {
    is Person -> "Person: ${client.birthDate}"
    is Company -> "Company: ${client.identifier}"
    // Pas de else ! Le compilateur vérifie l'exhaustivité
}
```

### Scala
```scala
sealed trait Client
case class Person(birthDate: LocalDate) extends Client
case class Company(identifier: String) extends Client

def map(client: Client) = client match {
  case Person(birthDate) => s"Person: $birthDate"
  case Company(identifier) => s"Company: $identifier"
  // Pas de default ! Exhaustivité garantie
}
```

### TypeScript
```typescript
type Client = Person | Company;

interface Person {
  type: 'PERSON';
  birthDate: Date;
}

interface Company {
  type: 'COMPANY';
  identifier: string;
}

function map(client: Client) {
  switch(client.type) {
    case 'PERSON':
      return `Person: ${client.birthDate}`;
    case 'COMPANY':
      return `Company: ${client.identifier}`;
    // TypeScript détecte si on oublie un cas !
  }
}
```

---

## 🚀 Bénéfices dans notre Architecture

### 1. Refactoring Sûr
```java
// Si on décide d'ajouter un nouveau type de Client :
public sealed class Client permits Person, Company, Organization {
    // ...
}

// Le compilateur nous FORCERA à modifier tous les switch !
// ✅ Aucun risque d'oublier un endroit
```

### 2. Documentation Vivante
```java
// Le code est auto-documenté :
public abstract sealed class Client permits Person, Company {
    // "Je suis soit Person, soit Company. Point final."
}
```

### 3. Performance
```java
// Le compilateur peut optimiser les switch
// Car il connaît TOUS les cas possibles à la compilation
```

---

## 📝 Résumé

| Aspect | abstract | sealed |
|--------|----------|--------|
| **Extension** | Ouverte (partout) | Fermée (contrôlée) |
| **Switch** | Besoin de default | Exhaustif sans default |
| **Sécurité** | Runtime errors possibles | Compile-time safety |
| **Expressivité** | Implicite | Explicite (permits) |
| **Intent** | Flou | Clair ("Uniquement ces types") |

**sealed = Type Algébrique en Java** ✨

C'est exactement comme les `enum`, mais pour des classes !

```java
// enum = liste fermée de valeurs
enum Color { RED, GREEN, BLUE }

// sealed = liste fermée de types
sealed class Client permits Person, Company
```

**Parfait pour DDD où le domaine métier est bien défini !** 🎯

