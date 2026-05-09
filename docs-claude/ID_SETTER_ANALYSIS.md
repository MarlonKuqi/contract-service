# Setter d'ID : Analyse et Bonnes Pratiques

## 📝 Le Commentaire Analysé

Le commentaire que tu as trouvé dit :

> "It's a good idea to use immutable properties. `public long Id { get; private set; }`"
> 
> Les 4 options pour setter l'ID :
> 1. **set id from the class method** → ❌ Confusing
> 2. **set id from constructor** → ⚠️ Pourquoi pas, mais...
> 3. **factory** → ✅ Pour assemblages complexes
> 4. **set id during deserialisation** → ✅ Simple et clair

---

## 🎯 Notre Situation Actuelle

```java
public abstract sealed class Client permits Person, Company {
    
    @Setter  // ← On utilise Lombok @Setter
    private UUID id;
    
    // ...
}

public final class Person extends Client {
    public Person(ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
        super(name, email, phone);
        // ❌ On ne set PAS l'ID ici
        this.birthDate = birthDate;
    }
}
```

**Problème** : L'ID est mutable (`@Setter`), alors qu'il devrait être immuable après création !

---

## ✅ Solution : Rendre l'ID Immuable (sauf pour la désérialisation)

### Option 1 : Setter Package-Private (Recommandé)

```java
public abstract sealed class Client permits Person, Company {
    
    @Setter(AccessLevel.PACKAGE)  // ← Setter package-private
    private UUID id;
    
    // Ou manuellement :
    void setId(UUID id) {  // ← package-private (pas de modificateur)
        this.id = id;
    }
}
```

**Avantages** :
- ✅ L'ID est immuable pour le code externe
- ✅ Le mapper (même package) peut le setter
- ✅ Pas besoin de passer l'ID au constructeur

**Dans le Mapper** :
```java
// infrastructure/persistence/mapper/ClientMapper.java
public Client toDomain(ClientJpaEntity entity) {
    Client domain = switch (entity) {
        case PersonJpaEntity p -> new Person(...);
        case CompanyJpaEntity c -> new Company(...);
    };
    
    domain.setId(entity.getId());  // ✅ OK car même package ? Non, packages différents !
    return domain;
}
```

**Problème** : Le mapper est dans `infrastructure.persistence.mapper`, pas dans `domain.client` !

---

### Option 2 : ID dans le Constructeur (Pas Optimal)

```java
public abstract sealed class Client permits Person, Company {
    
    private final UUID id;  // ← final = vraiment immuable
    
    protected Client(UUID id, ClientName name, Email email, PhoneNumber phone) {
        this.id = id;
        this.name = name;
        // ...
    }
}

public final class Person extends Client {
    public Person(UUID id, ClientName name, Email email, ...) {
        super(id, name, email, phone);
        // ...
    }
}
```

**Problème** :
- ❌ On doit passer `null` lors de la création initiale
```java
// Création
Person person = new Person(null, name, email, ...);  // ← null bizarre
clientRepo.save(person);  // JPA génère l'ID

// Mapper
Person person = new Person(entity.getId(), name, email, ...);  // ← OK
```

---

### Option 3 : Setter Public Mais Méthode Spéciale (Notre Choix Actuel)

```java
public abstract sealed class Client permits Person, Company {
    
    @Setter  // ← Public setter (pas idéal mais pragmatique)
    private UUID id;
}
```

**Avantages** :
- ✅ Simple
- ✅ Fonctionne avec JPA
- ✅ Le mapper peut setter l'ID

**Inconvénients** :
- ⚠️ N'importe qui peut modifier l'ID (pas immuable)
- ⚠️ Pas de protection contre les modifications accidentelles

---

### Option 4 : Méthode Protected pour Reconstruction (Recommandé DDD)

```java
public abstract sealed class Client permits Person, Company {
    
    private UUID id;  // ← Pas de setter public
    
    protected void reconstitute(UUID id) {  // ← Méthode explicite
        if (this.id != null) {
            throw new IllegalStateException("ID already set");
        }
        this.id = id;
    }
}
```

**Dans le Mapper** :
```java
public Client toDomain(ClientJpaEntity entity) {
    Client domain = switch (entity) {
        case PersonJpaEntity p -> new Person(...);
        case CompanyJpaEntity c -> new Company(...);
    };
    
    domain.reconstitute(entity.getId());  // ✅ Explicite !
    return domain;
}
```

**Avantages** :
- ✅ Intent clair ("reconstitute" = reconstruction depuis BDD)
- ✅ Protection contre double assignation
- ✅ Pas de setter public

---

## 🎯 Recommandation pour NOTRE Code

### Solution Pragmatique : AccessLevel.PROTECTED pour le Mapper

```java
public abstract sealed class Client permits Person, Company {
    
    @Setter(AccessLevel.PROTECTED)  // ← Setter protected
    private UUID id;
}
```

**Mais** : Le mapper n'est pas dans la même hiérarchie de classes !

---

### Solution Finale : Package Infrastructure pour Reconstruction

Créons une méthode spéciale pour l'infrastructure :

```java
// domain/client/Client.java
public abstract sealed class Client permits Person, Company {
    
    private UUID id;
    
    // ✅ Méthode UNIQUEMENT pour reconstruction depuis l'infrastructure
    public void reconstructFromPersistence(UUID id) {
        if (this.id != null && !this.id.equals(id)) {
            throw new IllegalStateException("Cannot change existing ID");
        }
        this.id = id;
    }
}
```

**Dans le Mapper** :
```java
public Client toDomain(ClientJpaEntity entity) {
    Client domain = switch (entity) {
        case PersonJpaEntity p -> new Person(...);
        case CompanyJpaEntity c -> new Company(...);
    };
    
    domain.reconstructFromPersistence(entity.getId());  // ✅ Intent clair !
    return domain;
}
```

---

## 📊 Comparaison des Options

| Option | Avantages | Inconvénients | Note |
|--------|-----------|---------------|------|
| **@Setter public** | Simple | Pas immuable | 3/5 |
| **@Setter(PACKAGE)** | Immuable hors package | Mapper package différent | 2/5 |
| **ID au constructeur** | Vraiment immuable | null lors création | 4/5 |
| **reconstitute()** | Intent clair, protection | Méthode supplémentaire | 5/5 ⭐ |

---

## ✅ Solution Recommandée pour Notre Projet

### Changeons le code :

```java
// domain/client/Client.java
public abstract sealed class Client permits Person, Company {
    
    private UUID id;  // ← Pas de @Setter
    
    public UUID getId() {
        return id;
    }
    
    /**
     * Reconstruit l'identité depuis la persistence.
     * NE DOIT ÊTRE UTILISÉ QUE PAR L'INFRASTRUCTURE.
     */
    public void assignIdFromPersistence(UUID id) {
        if (this.id != null && !this.id.equals(id)) {
            throw new IllegalStateException("Cannot reassign existing ID");
        }
        this.id = id;
    }
}
```

```java
// domain/contract/Contract.java
public class Contract {
    
    private UUID id;  // ← Pas de @Setter
    
    public UUID getId() {
        return id;
    }
    
    /**
     * Reconstruit l'identité depuis la persistence.
     * NE DOIT ÊTRE UTILISÉ QUE PAR L'INFRASTRUCTURE.
     */
    public void assignIdFromPersistence(UUID id) {
        if (this.id != null && !this.id.equals(id)) {
            throw new IllegalStateException("Cannot reassign existing ID");
        }
        this.id = id;
    }
}
```

**Dans le Mapper** :
```java
public Client toDomain(ClientJpaEntity entity) {
    Client domain = switch (entity) {
        case PersonJpaEntity p -> new Person(...);
        case CompanyJpaEntity c -> new Company(...);
    };
    
    domain.assignIdFromPersistence(entity.getId());  // ✅ Intent explicite !
    return domain;
}
```

---

## 🎓 Pourquoi cette Solution ?

1. **Intent Explicite**
   - `assignIdFromPersistence()` dit clairement que c'est pour la reconstruction

2. **Protection**
   - Protection contre la réassignation d'ID
   - Exception si on essaie de changer un ID existant

3. **DDD-Friendly**
   - Méthode documentée comme "infrastructure only"
   - Pas de setter générique

4. **Pragmatique**
   - Pas besoin de passer l'ID au constructeur
   - Fonctionne avec JPA

---

## 🎯 Résumé

**Commentaire original** :
- ✅ Correct : "set id during deserialisation" est la meilleure approche
- ✅ Correct : L'ID devrait être immuable (`private set`)

**Notre implémentation recommandée** :
```java
// Création (pas d'ID)
Person person = new Person(name, email, ...);

// Après save (JPA génère l'ID, mapper reconstruit)
domain.assignIdFromPersistence(entity.getId());
```

**C'est un bon compromis entre DDD puriste et pragmatisme !** 🎯

