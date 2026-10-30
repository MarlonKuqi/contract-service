# 📏 Explication du `maxPageSize` dans PaginationProperties

## 🎯 Rôle Principal

Le `maxPageSize` sert de **limite de sécurité** pour protéger l'application contre les abus de pagination qui pourraient surcharger le serveur et la base de données.

---

## 🔍 Utilisation Concrète

### Configuration Actuelle

```yaml
# application.yml
app:
  pagination:
    default-page-size: 20    # Taille par défaut si non spécifiée
    max-page-size: 100       # Limite maximale autorisée
```

### Validation dans ValidatingPageableArgumentResolver

```java
// Ligne 52-55 de ValidatingPageableArgumentResolver.java
if (size > paginationProperties.maxPageSize()) {
    throw new InvalidPaginationException(
        "Page size must not exceed " + paginationProperties.maxPageSize() + ", but was: " + size);
}
```

---

## 🛡️ Scénarios de Protection

### ⚡ Cas Réel : Les Deux Validations en Action

**Configuration** :
```yaml
# application.yml
app:
  pagination:
    default-page-size: 20
    max-page-size: 100      # ← Validé par @Max(1000) ✅
```

**Tentative 1 - Admin tente de configurer une valeur trop élevée** :
```yaml
# application.yml (INVALIDE)
app:
  pagination:
    max-page-size: 5000     # ← Rejeté par @Max(1000) ❌
```
→ **Résultat** : Application refuse de démarrer

**Tentative 2 - Client tente d'abuser de la pagination** :
```http
GET /contracts?size=500     # ← Rejeté par validation manuelle ❌
```
→ **Résultat** : HTTP 400 "Page size must not exceed 100, but was: 500"

**Requête Normale** :
```http
GET /contracts?size=50      # ← Validé (50 ≤ 100) ✅
```
→ **Résultat** : Retourne 50 contrats

---

## 🛡️ Scénarios de Protection Détaillés

### Scénario 1 : Requête Normale (Autorisée)
```http
GET /v2/clients/123/contracts?page=0&size=50
```
✅ **Résultat** : Accepté (50 ≤ 100)
- Retourne 50 contrats par page

### Scénario 2 : Requête Sans `size` (Utilise défaut)
```http
GET /v2/clients/123/contracts?page=0
```
✅ **Résultat** : Utilise `defaultPageSize=20`
- Retourne 20 contrats par page

### Scénario 3 : Requête Abusive (Bloquée)
```http
GET /v2/clients/123/contracts?page=0&size=1000
```
❌ **Résultat** : Rejetée avec erreur 400
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Page size must not exceed 100, but was: 1000",
  "instance": "/v2/clients/123/contracts"
}
```

### Scénario 4 : Attaque DoS Tentée
```http
GET /v2/clients/123/contracts?page=0&size=999999
```
❌ **Résultat** : Rejetée AVANT d'interroger la DB
- Protection contre surcharge mémoire
- Évite requête SQL massive

---

## 💡 Pourquoi C'est Important ?

### 1. **Protection Performance Serveur**

Sans limite :
```sql
-- Un utilisateur malveillant pourrait demander :
SELECT * FROM contract WHERE client_id = ? LIMIT 999999;
```

**Impact** :
- ❌ Charge mémoire énorme (999999 objets en RAM)
- ❌ Temps de sérialisation JSON excessif
- ❌ Bande passante réseau saturée
- ❌ Autres utilisateurs impactés

Avec `maxPageSize=100` :
```sql
-- Maximum possible :
SELECT * FROM contract WHERE client_id = ? LIMIT 100;
```

**Bénéfices** :
- ✅ Charge mémoire contrôlée (~100 objets max)
- ✅ Temps réponse prévisible
- ✅ Protection des ressources serveur

### 2. **Protection Base de Données**

**Sans limite** :
- Requêtes peuvent renvoyer des millions de lignes
- Locks de table prolongés
- Impact sur autres transactions

**Avec limite** :
- Requêtes bornées et prévisibles
- Pas de surprise dans les plans d'exécution
- Cache DB plus efficace (résultats similaires)

### 3. **Expérience Utilisateur**

**Pagination excessive = UX dégradée** :
```http
GET /contracts?size=10000
```
- ⏱️ 30 secondes de chargement
- 📦 5 MB de JSON
- 🖥️ Browser freeze lors du parsing

**Pagination raisonnable** :
```http
GET /contracts?size=20
```
- ⏱️ < 1 seconde
- 📦 50 KB de JSON
- 🖥️ Affichage fluide

---

## 📊 Valeurs Recommandées

| Contexte | defaultPageSize | maxPageSize | Justification |
|----------|----------------|-------------|---------------|
| **API Publique** | 20 | 100 | Protection DoS, UX mobile |
| **API Interne** | 50 | 500 | Utilisateurs de confiance |
| **Admin Panel** | 25 | 200 | Balance perf/visibilité |
| **Exports** | N/A | 1000 | Endpoint dédié avec streaming |

**Notre configuration actuelle** :
- ✅ `defaultPageSize: 20` → Bon pour UX mobile/web
- ✅ `maxPageSize: 100` → Protection standard API REST

---

## 🔧 Configuration Spring Interne

### ⚠️ Important: Trois Niveaux de Validation (Pas de Redondance!)

Beaucoup se demandent pourquoi on a `@Max(1000)` **ET** validation manuelle. Voici pourquoi :

#### Niveau 1️⃣ : Validation de la Configuration (`@Max(1000)`)

```java
@ConfigurationProperties(prefix = "app.pagination")
@Validated
public class PaginationProperties {
    @Max(1000)  // ← Valide application.yml au STARTUP
    private int maxPageSize;
}
```

**Cible** : Fichier `application.yml`  
**Moment** : Démarrage de l'application  
**Protection** : Empêche un admin de configurer `max-page-size: 999999` par erreur

**Exemple** :
```yaml
# ❌ Application refuse de démarrer
app:
  pagination:
    max-page-size: 5000  # > @Max(1000) → ERREUR
```

#### Niveau 2️⃣ : Validation de la Requête (Code Manuel)

```java
// ValidatingPageableArgumentResolver.java - Lignes 52-55
if (size > paginationProperties.maxPageSize()) {
    throw new InvalidPaginationException(
        "Page size must not exceed 100, but was: " + size);
}
```

**Cible** : Paramètre `?size=X` dans l'URL  
**Moment** : À chaque requête HTTP  
**Protection** : Empêche un client d'abuser avec `?size=999999`

**Exemple** :
```http
GET /contracts?size=150
# ❌ HTTP 400: "Page size must not exceed 100, but was: 150"
```

#### Niveau 3️⃣ : Fallback Spring Data

```java
setMaxPageSize(paginationProperties.maxPageSize());
```

**Cible** : Sécurité ultime si validation manuelle échoue  
**Moment** : Résolution du Pageable  
**Protection** : Limite silencieusement (notre validation rejette avant)

### Pourquoi les Trois ? (Defense in Depth)

```
┌─────────────────────────────────────────────┐
│ @Max(1000)           → Protège la CONFIG    │
├─────────────────────────────────────────────┤
│ Validation manuelle  → Protège la REQUÊTE   │
├─────────────────────────────────────────────┤
│ setMaxPageSize()     → Fallback Spring      │
└─────────────────────────────────────────────┘
```

**Ce SONT des validations différentes, pas redondantes !**

📖 **Voir détails** : `MAX_ANNOTATION_VS_MANUAL_VALIDATION.md`

---

## 🧪 Tests de Validation

### Test 1 : Validation Maximale
```java
// PaginationPropertiesTest.java
@Test
void shouldEnforceMaxConstraint() {
    PaginationProperties props = new PaginationProperties(20, 1000);
    assertThat(props.maxPageSize()).isEqualTo(1000); // Limite max = 1000
}
```

### Test 2 : Validation dans Resolver
```java
// ValidatingPageableArgumentResolver
@Test
void shouldRejectSizeAboveMax() {
    // GET /contracts?size=150 (> maxPageSize=100)
    assertThrows(InvalidPaginationException.class, 
        () -> resolver.resolveArgument(...));
}
```

---

## 📐 Architecture de Validation

```
Client Request
     │
     ├─→ GET /contracts?size=150
     │
     ▼
ValidatingPageableArgumentResolver
     │
     ├─→ Validation: size (150) > maxPageSize (100) ?
     │   └─→ YES → throw InvalidPaginationException ❌
     │   └─→ NO  → Continue ✅
     │
     ▼
Spring Data PageableHandlerMethodArgumentResolver
     │
     ├─→ setMaxPageSize(100) limite silencieuse (fallback)
     │
     ▼
Repository Query
     │
     └─→ SELECT ... LIMIT 100 (sécurisé)
```

---

## 🎓 Bonnes Pratiques

### ✅ À Faire

1. **Toujours définir un `maxPageSize`**
   ```yaml
   app:
     pagination:
       max-page-size: 100  # OBLIGATOIRE
   ```

2. **Documenter dans OpenAPI**
   ```java
   @Parameter(
       name = "size",
       description = "Number of items per page",
       schema = @Schema(minimum = "1", maximum = "100", defaultValue = "20")
   )
   ```

3. **Logger les tentatives d'abus**
   ```java
   if (size > maxPageSize) {
       log.warn("Page size abuse attempt: {} (max: {})", size, maxPageSize);
       throw new InvalidPaginationException(...);
   }
   ```

### ❌ À Éviter

1. **Pas de limite** (danger)
   ```java
   setMaxPageSize(Integer.MAX_VALUE); // ❌ JAMAIS
   ```

2. **Limite trop élevée**
   ```yaml
   max-page-size: 10000  # ❌ Risque DoS
   ```

3. **Limiter silencieusement sans erreur**
   ```java
   // ❌ Mauvais : utilisateur ne sait pas pourquoi il a 100 au lieu de 150
   int effectiveSize = Math.min(requestedSize, maxPageSize);
   ```

---

## 📝 Conclusion

### Rôle de `maxPageSize`

**C'est un garde-fou de sécurité** qui :
1. ✅ Protège le serveur contre surcharge mémoire
2. ✅ Protège la DB contre requêtes massives
3. ✅ Garantit une expérience utilisateur fluide
4. ✅ Prévient les attaques DoS par pagination

### Deux Niveaux de Protection

**Important** : `maxPageSize` est validé DEUX FOIS (ce n'est PAS redondant) :

| Validation | Cible | Quand | Exemple |
|------------|-------|-------|---------|
| **@Max(1000)** | Configuration admin | Startup | `max-page-size: 5000` → App refuse démarrage |
| **Code manuel** | Requête client | Runtime | `?size=500` → HTTP 400 |

**Les deux sont nécessaires** pour une défense en profondeur.

📖 **Voir détails** : `MAX_ANNOTATION_VS_MANUAL_VALIDATION.md`

### Différence avec `defaultPageSize`

| Propriété | Rôle | Exemple |
|-----------|------|---------|
| **defaultPageSize** | Valeur par défaut si non spécifié | `GET /contracts` → size=20 |
| **maxPageSize** | Limite de sécurité jamais dépassable | `GET /contracts?size=150` → Erreur 400 |

### Valeur Actuelle

```yaml
app:
  pagination:
    default-page-size: 20   # Confort utilisateur
    max-page-size: 100      # Protection système
```

**Verdict** : ✅ Configuration équilibrée et sécurisée

---

## 🔗 Fichiers Liés

- Configuration : `application.yml` (lignes 50-53)
- Classe : `PaginationProperties.java`
- Validation : `ValidatingPageableArgumentResolver.java` (lignes 52-55)
- Tests : `PaginationPropertiesTest.java`
- Documentation : `PAGINATION_CONFIGURATION.md`

