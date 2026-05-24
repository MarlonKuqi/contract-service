# Principe "Always-Valid" et Validation

## 🎯 Principe Fondamental

En Domain-Driven Design, un **aggregate** doit toujours être dans un état valide. C'est le principe **"always-valid"**.

### Implications

- **À la création** : Validation stricte de toutes les données entrantes
- **Lors des changements d'état** : Validation des nouvelles valeurs
- **À la reconstruction** : Pas de validation - confiance en la source persistée

---

## 🔐 Zones de Confiance

### Zone de Confiance

**La base de données** fait partie de la zone de confiance car :

- Les données ont été validées lors de leur création initiale
- Elles ont passé toutes les validations métier avant persistance
- La base de données est sous notre contrôle

**Conséquence** : `reconstitute()` ne valide pas.

### Zone de Non-Confiance

Toute donnée venant de **l'extérieur du domaine** doit être validée :

- API REST (requêtes utilisateurs)
- Import de fichiers
- Services externes
- Messages d'événements

**Conséquence** : `of()` doit valider.

---

## 📐 Pattern d'Implémentation

### Value Objects

```java

@Value
public class MyValueObject {
    Type field;

    private MyValueObject(Type field) {
        this.field = field;  // Constructeur confiant
    }

    public static MyValueObject of(RawType raw) {
        Type normalized = normalize(raw);
        validate(normalized);
        return new MyValueObject(normalized);
    }
}
```

### Entities/Aggregates

```java
public final class MyEntity {

    private MyEntity(UUID id, ...) {
        this.id = id;  // Constructeur confiant
        // ...
    }

    public static MyEntity of(...) {
        validate(...);  // ✅ Validation complète
        return new MyEntity(...);
    }

    public static MyEntity reconstitute(UUID id, ...) {
        // ❌ PAS de validation - confiance DB
        return new MyEntity(id, ...);
    }
}
```

---

## 📋 Règles de Validation

| Source des Données   | Méthode          | Validation ?        |
|----------------------|------------------|---------------------|
| API/Fichiers/Externe | `of()`           | ✅ Obligatoire       |
| Base de données      | `reconstitute()` | ❌ Non               |
| Value Object validé  | `withXxx()`      | ❌ Non (déjà validé) |

---

## 💡 Justification

### Pourquoi pas de validation dans reconstitute() ?

1. **Performance** : Évite la re-validation à chaque lecture
2. **Principe "always-valid"** : Les données ont déjà été validées
3. **Séparation des responsabilités** : La corruption de données n'est pas gérée par le domaine
4. **Évolution** : Possibilité de renforcer les validations sans migration forcée

### Gestion de la corruption de données

**Ce n'est PAS le rôle du domaine** :

- **Détection** : Contraintes DB, tests d'intégrité
- **Correction** : Migrations de données
- **Prévention** : Validation stricte à la création

---

## ⚖️ Débat : Valider ou Non dans reconstituteFromDatabase() ?

### Position Adoptée : PAS de Validation ✅

**Raisons** :

- DB = zone de confiance
- Performance (lectures fréquentes)
- Principe "always-valid" respecté sur toute la durée de vie de l'aggregate

### Alternative : Validation Systématique

**Arguments** :

- Filet de sécurité contre corruption
- Code uniforme entre `of()` et `reconstituteFromDatabase()`

**Notre choix** : Nous privilégions la performance et la séparation des responsabilités.

---

## 📚 Références

- **Eric Evans** - Domain-Driven Design : "Aggregates must always be in a consistent state"
- **Vaughn Vernon** - Implementing DDD : "Trust your persistence, validate at boundaries"
- **Martin Fowler** - PoEAA : "Validation at the edges"


