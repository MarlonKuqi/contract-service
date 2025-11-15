# 🔍 Analyse: Incohérence defaultPageSize vs maxPageSize

## ❓ Question Légitime

**"Pourquoi avoir `maxPageSize=1000` si `defaultPageSize` est déjà limité à `@Max(100)` ?"**

---

## 🚨 Problème Identifié

### Configuration Actuelle

```java
@ConfigurationProperties(prefix = "app.pagination")
public class PaginationProperties {
    
    @Min(1)
    @Max(100)           // ← Défaut max = 100
    private int defaultPageSize;
    
    @Min(1)
    @Max(1000)          // ← Client peut demander jusqu'à 1000 !
    private int maxPageSize;
}
```

```yaml
# application.yml
app:
  pagination:
    default-page-size: 20
    max-page-size: 100
```

### Comportement Actuel

| Requête | Résultat | Taille effective |
|---------|----------|------------------|
| `GET /contracts` | ✅ OK | 20 (défaut) |
| `GET /contracts?size=50` | ✅ OK | 50 |
| `GET /contracts?size=100` | ✅ OK | 100 |
| `GET /contracts?size=500` | ✅ OK | 500 ⚠️ |
| `GET /contracts?size=1001` | ❌ Erreur | - |

**Incohérence** : On limite le défaut à 100 mais on autorise le client à demander jusqu'à 1000 !

---

## 🤔 Pourquoi Cette Confusion ?

### Intention Originale (Probable)

Deux propriétés séparées pour deux besoins :

1. **defaultPageSize** : "Quelle taille par défaut si client ne spécifie rien ?"
2. **maxPageSize** : "Quelle est la limite absolue qu'un client peut demander ?"

### Problème Sémantique

**`@Max(100)` sur `defaultPageSize`** ne limite PAS la valeur maximale que le client peut demander, mais uniquement ce qu'on peut configurer comme défaut !

**Exemple** :
```yaml
# ❌ REJETÉ au startup
app:
  pagination:
    default-page-size: 150  # > @Max(100)
    max-page-size: 1000
```

Mais :
```http
GET /contracts?size=500  # ✅ ACCEPTÉ (< maxPageSize=1000)
```

---

## 🎯 Solutions Possibles

### Option 1 : Unifier les Limites (Recommandé)

**Principe** : `maxPageSize` devrait être la SEULE limite

```java
@ConfigurationProperties(prefix = "app.pagination")
public class PaginationProperties {
    
    @Min(1)
    @Max(1000)          // ← Peut être configuré jusqu'à 1000
    private int defaultPageSize;
    
    @Min(1)
    @Max(1000)          // ← Limite absolue = 1000
    private int maxPageSize;
    
    // Validation logique (non Spring)
    @PostConstruct
    public void validate() {
        if (defaultPageSize > maxPageSize) {
            throw new IllegalStateException(
                "defaultPageSize (" + defaultPageSize + 
                ") cannot exceed maxPageSize (" + maxPageSize + ")"
            );
        }
    }
}
```

**Configuration recommandée** :
```yaml
app:
  pagination:
    default-page-size: 20   # Confort UX
    max-page-size: 100      # Limite sécurité
```

**Avantages** :
- ✅ Cohérence : limite unique claire
- ✅ `@Max(100)` sur `defaultPageSize` n'a plus de sens → supprimer
- ✅ `maxPageSize` devient la référence unique

---

### Option 2 : Simplifier avec UNE SEULE Propriété

**Principe** : Supprimer `maxPageSize` complètement

```java
@ConfigurationProperties(prefix = "app.pagination")
public class PaginationProperties {
    
    @Min(1)
    @Max(100)           // ← Limite UNIQUE
    private int defaultPageSize;
    
    // maxPageSize supprimé !
}
```

```java
// ValidatingPageableArgumentResolver.java
public ValidatingPageableArgumentResolver(PaginationProperties props) {
    setFallbackPageable(PageRequest.of(0, props.defaultPageSize(), ...));
    setMaxPageSize(100);  // ← Hardcodé ou via @Max annotation
}
```

**Validation** :
```java
if (size > 100) {  // Hardcodé, mais cohérent avec @Max(100)
    throw new InvalidPaginationException(
        "Page size must not exceed 100, but was: " + size);
}
```

**Avantages** :
- ✅ Une seule source de vérité
- ✅ Moins de confusion
- ❌ Perd la flexibilité de configurer séparément

---

### Option 3 : Clarifier la Sémantique (Status Quo Amélioré)

**Principe** : Garder les deux MAIS documenter et renommer

```java
@ConfigurationProperties(prefix = "app.pagination")
public class PaginationProperties {
    
    /**
     * Taille de page utilisée quand le client ne spécifie pas ?size=
     * Maximum: 100 (pour éviter un défaut trop élevé)
     */
    @Min(1)
    @Max(100)
    private int defaultPageSize;
    
    /**
     * Limite maximale qu'un client peut demander via ?size=X
     * Devrait être >= defaultPageSize pour cohérence
     */
    @Min(1)
    @Max(1000)
    private int maxPageSizeAllowedForClients;  // Renommé pour clarté
}
```

**Validation ajoutée** :
```java
@PostConstruct
public void validate() {
    if (defaultPageSize > maxPageSize) {
        throw new IllegalStateException(
            "defaultPageSize cannot exceed maxPageSize");
    }
}
```

**Avantages** :
- ✅ Flexibilité préservée
- ✅ Sémantique clarifiée
- ❌ Complexité maintenue

---

## 🔬 Analyse de Notre Configuration Actuelle

```yaml
app:
  pagination:
    default-page-size: 20    # OK, < @Max(100)
    max-page-size: 100       # OK, < @Max(1000)
```

**Observations** :
1. ✅ `defaultPageSize (20) < maxPageSize (100)` → Cohérent
2. ⚠️ `@Max(100)` sur `defaultPageSize` n'apporte RIEN ici (20 < 100)
3. ⚠️ `@Max(1000)` sur `maxPageSize` autorise config jusqu'à 1000, mais on utilise 100

**Conclusion** : Les annotations `@Max` sont **trop permissives** pour notre usage réel.

---

## ✅ Recommandation Finale

### Action : **Simplifier avec Option 1** (Unification)

**Changement proposé** :

```java
@ConfigurationProperties(prefix = "app.pagination")
@Validated
public class PaginationProperties {
    
    /**
     * Taille de page par défaut (quand ?size= non spécifié).
     * Doit être <= maxPageSize.
     */
    @Min(1)
    private int defaultPageSize;
    
    /**
     * Limite maximale de pagination (protection contre abus).
     * Valide à la fois la config ET les requêtes clients.
     */
    @Min(1)
    @Max(100)  // ← LIMITE UNIQUE
    private int maxPageSize;
    
    @PostConstruct
    public void validate() {
        if (defaultPageSize > maxPageSize) {
            throw new IllegalStateException(
                "defaultPageSize (" + defaultPageSize + 
                ") cannot exceed maxPageSize (" + maxPageSize + ")");
        }
    }
}
```

**Configuration** :
```yaml
app:
  pagination:
    default-page-size: 20   # Confort UX
    max-page-size: 100      # Limite unique claire
```

**Bénéfices** :
1. ✅ `@Max(100)` est maintenant sur la **bonne** propriété
2. ✅ Une seule limite à retenir (100)
3. ✅ Validation logique `defaultPageSize ≤ maxPageSize`
4. ✅ Plus de confusion sémantique

---

## 📊 Comparaison des Solutions

| Critère | Option 1 (Unifier) | Option 2 (Simplifier) | Option 3 (Clarifier) | Actuel |
|---------|--------------------|-----------------------|----------------------|--------|
| **Simplicité** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ |
| **Flexibilité** | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Clarté** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **Cohérence** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |

**Recommandation** : **Option 1** (meilleur compromis)

---

## 🔧 Implémentation Proposée

### Fichier: PaginationProperties.java

```java
package com.mk.contractservice.infrastructure.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.pagination")
@Validated
@Getter
@Setter
@NoArgsConstructor
public class PaginationProperties {

    /**
     * Default page size when client doesn't specify ?size= parameter.
     * Must be less than or equal to maxPageSize.
     */
    @Min(1)
    private int defaultPageSize;

    /**
     * Maximum page size allowed for client requests.
     * Protects against DoS attacks via excessive pagination.
     */
    @Min(1)
    @Max(100)  // Single source of truth for maximum limit
    private int maxPageSize;

    @PostConstruct
    public void validate() {
        if (defaultPageSize > maxPageSize) {
            throw new IllegalStateException(
                String.format(
                    "defaultPageSize (%d) cannot exceed maxPageSize (%d)", 
                    defaultPageSize, maxPageSize
                )
            );
        }
    }
}
```

### Tests à Ajouter

```java
@Test
void shouldRejectWhenDefaultExceedsMax() {
    assertThatThrownBy(() -> 
        new PaginationProperties(150, 100).validate()
    )
    .isInstanceOf(IllegalStateException.class)
    .hasMessageContaining("defaultPageSize (150) cannot exceed maxPageSize (100)");
}
```

---

## 📝 Conclusion

**La question initiale était pertinente** : avoir `@Max(100)` sur `defaultPageSize` ET `@Max(1000)` sur `maxPageSize` crée une incohérence sémantique.

**Solution** : 
- ✅ Supprimer `@Max` de `defaultPageSize`
- ✅ Garder `@Max(100)` uniquement sur `maxPageSize`
- ✅ Ajouter validation logique `defaultPageSize ≤ maxPageSize`

Cette approche clarifie que **`maxPageSize` est LA limite de référence**.

