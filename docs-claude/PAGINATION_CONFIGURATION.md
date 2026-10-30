# Configuration de la Pagination - Documentation

## ✨ Vue d'ensemble

La pagination est maintenant **configurable** via `application.yml` au lieu d'être codée en dur dans le code.

---

## 📝 Configuration

### Fichier : `application.yml`

```yaml
app:
  pagination:
    default-page-size: 20   # Taille de page par défaut (si non spécifiée)
    max-page-size: 100      # Taille de page maximale (limite de sécurité)
```

### Valeurs par Défaut

| Paramètre | Valeur par défaut | Min | Max | Description |
|-----------|-------------------|-----|-----|-------------|
| `default-page-size` | 20 | 1 | 100 | Taille utilisée si `?size` n'est pas fourni |
| `max-page-size` | 100 | 1 | 1000 | Taille maximale autorisée (sécurité) |

---

## 🔧 Utilisation dans l'API

### Endpoint : `GET /v1/clients/{clientId}/contracts`

#### Paramètres de Pagination

| Paramètre | Type | Requis | Défaut | Validation | Description |
|-----------|------|--------|--------|------------|-------------|
| `page` | int | Non | 0 | ≥ 0 | Numéro de page (base 0) |
| `size` | int | Non | 20 (configurable) | 1-100 | Nombre d'éléments par page |
| `sort` | string | Non | `lastModified,desc` | - | Champ et direction de tri |

#### Exemples de Requêtes

```bash
# Utilise les valeurs par défaut (page=0, size=20, sort=lastModified,desc)
GET /v1/clients/{clientId}/contracts

# Page 2, taille 10
GET /v1/clients/{clientId}/contracts?page=2&size=10

# Tri par costAmount ascendant
GET /v1/clients/{clientId}/contracts?sort=costAmount,asc

# Filtre par updatedSince + pagination
GET /v1/clients/{clientId}/contracts?updatedSince=2025-11-01T00:00:00&page=0&size=25
```

### Réponse JSON

```json
{
  "content": [
    {
      "id": "uuid",
      "period": {
        "startDate": "2025-01-01T00:00:00",
        "endDate": null
      },
      "costAmount": 1000.00
    }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

---

## 🛡️ Sécurité et Limites

### Protection contre les requêtes abusives

Si un client demande `?size=10000`, la valeur sera **automatiquement plafonnée** à `max-page-size` (100).

```java
// Dans ContractController
final int effectiveSize = size != null 
        ? Math.min(size, paginationProperties.maxPageSize())  // ✅ Plafonné
        : paginationProperties.defaultPageSize();
```

### Validation

- `@Min(0)` sur `page` → Empêche les pages négatives
- `@Min(1)` et `@Max(100)` sur `size` → Validation Bean Validation
- Plafonnement supplémentaire dans le code → Double protection

---

## 🔧 Configuration par Environnement

### Développement (`application-dev.yml`)
```yaml
app:
  pagination:
    default-page-size: 10   # Plus petit pour faciliter les tests
    max-page-size: 50
```

### Production (`application-prod.yml`)
```yaml
app:
  pagination:
    default-page-size: 20
    max-page-size: 100
```

### Tests (`application-test.yml`)
```yaml
app:
  pagination:
    default-page-size: 20
    max-page-size: 100
```

---

## 🧪 Tests

### Tests Unitaires

**Fichier** : `PaginationPropertiesTest.java`

- ✅ Création de propriétés valides
- ✅ Rejet si `defaultPageSize > maxPageSize`
- ✅ Accepte `defaultPageSize == maxPageSize`

### Tests d'Intégration

**Fichier** : `ContractPaginationIT.java`

- ✅ `shouldPaginateContractsAcrossMultiplePages` - Navigation multi-pages
- ✅ `shouldUseDefaultPageSizeWhenNotSpecified` - Taille par défaut
- ✅ `shouldHandleDifferentPageSizes` - Différentes tailles (5, 25, 50)
- ✅ `shouldEnforceMaxPageSizeLimit` - **NOUVEAU** - Plafonnement à maxPageSize

---

## 📊 Architecture

```
┌─────────────────────────────────────────┐
│  application.yml                        │
│  app.pagination.default-page-size: 20   │
│  app.pagination.max-page-size: 100      │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  PaginationProperties (record)          │
│  - Validation (@Min, @Max)              │
│  - Custom validation logic              │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  ContractController                     │
│  - Injecte PaginationProperties         │
│  - Calcule effectiveSize                │
│  - Applique Math.min() pour plafonner   │
└─────────────────────────────────────────┘
```

---

## 🎯 Avantages de cette Approche

| Avantage | Description |
|----------|-------------|
| ✅ **Configurable** | Changement sans recompiler |
| ✅ **Sécurisé** | Limite max empêche les abus |
| ✅ **Flexible** | Config par environnement |
| ✅ **Testé** | Tests unitaires + intégration |
| ✅ **Documenté** | OpenAPI mis à jour |
| ✅ **Validé** | Bean Validation + logique custom |

---

## 🚀 Migration

### Avant
```java
@PageableDefault(size = 20, sort = "lastModified")
```
❌ Valeur codée en dur, impossible à changer sans redéployer

### Après
```yaml
app:
  pagination:
    default-page-size: 20
    max-page-size: 100
```
✅ Configurable, modifiable à chaud (avec redémarrage du service)

---

## 📚 Références

- **Code** :
  - `PaginationProperties.java` - Configuration properties
  - `PaginationConfig.java` - EnableConfigurationProperties
  - `ContractController.java` - Utilisation

- **Tests** :
  - `PaginationPropertiesTest.java` - Tests unitaires
  - `ContractPaginationIT.java` - Tests d'intégration

- **Configuration** :
  - `application.yml` - Config par défaut
  - `application-dev.yml` - Config dev
  - `application-prod.yml` - Config prod

