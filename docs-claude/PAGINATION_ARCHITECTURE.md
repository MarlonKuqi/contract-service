# Architecture de Pagination

## Vue d'ensemble

La pagination dans cette application est gérée au niveau **infrastructure**, conformément aux principes DDD. Elle ne fait pas partie du domaine métier.

**Point important** : La validation des paramètres de pagination se fait au niveau **HTTP/Controller** (via `ValidatingPageableArgumentResolver`), PAS au niveau repository. Le repository reçoit toujours un `Pageable` valide.

## Composants

### 1. PaginationProperties

**Localisation** : `infrastructure.config.PaginationProperties`

**Responsabilité** : Configuration centralisée des contraintes de pagination

```java
@ConfigurationProperties(prefix = "app.pagination")
@Validated
public class PaginationProperties {
    @Min(1) @Max(100) 
    private int defaultPageSize;
    
    @Min(1) @Max(1000) 
    private int maxPageSize;
    // + getters/setters
}
```

**Pourquoi une classe et pas un record ?**
- ✅ `@ConfigurationProperties` fonctionne mieux avec des classes classiques
- ✅ Spring a besoin des setters pour injecter les valeurs depuis `application.yml`
- ✅ Les annotations de validation fonctionnent de manière fiable

**Configuration** (`application.yml`) :
```yaml
app:
  pagination:
    default-page-size: 20
    max-page-size: 100
```

**Validation** : 
- Les annotations `@Min/@Max` sont validées par Spring au **démarrage de l'application**
- Si les valeurs dans `application.yml` sont invalides, l'application **ne démarre pas**
- Le constructeur ne fait **aucune validation** (Spring s'en charge)

---

### 2. ValidatingPageableArgumentResolver

**Localisation** : `infrastructure.config.ValidatingPageableArgumentResolver`

**Responsabilité** : Validation des paramètres de pagination HTTP **AVANT** qu'ils n'arrivent au controller

**Moment d'exécution** : Spring appelle automatiquement ce resolver pour chaque paramètre `Pageable` dans les controllers. La validation se fait AVANT l'exécution de la méthode du controller.

**Validations effectuées** :
- ✅ `page >= 0` (rejette les valeurs négatives)
- ✅ `size >= 1` (rejette zéro et négatif)
- ✅ `size <= maxPageSize` (rejette les valeurs trop grandes)
- ✅ Format des nombres (rejette "abc", "xyz", etc.)

**Exceptions lancées** :
- `InvalidPaginationException` avec message descriptif (extension de `RuntimeException`)
- ⚠️ **Important** : Utilise une exception spécifique plutôt que `IllegalArgumentException` pour éviter les conflits avec d'autres exception handlers

**Exemple** :
```java
GET /v2/contracts?page=-1&size=20
→ 400 Bad Request
{
  "title": "Invalid Parameter",
  "detail": "Page number must not be less than zero, but was: -1",
  "code": "invalidParameter"
}
```

---

### 3. PaginationConfig

**Localisation** : `infrastructure.config.PaginationConfig`

**Responsabilité** : Enregistrement du resolver personnalisé dans Spring MVC

```java
@Configuration
public class PaginationConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new ValidatingPageableArgumentResolver(paginationProperties));
    }
}
```

**Avantages** :
- ✅ Automatique : tous les controllers bénéficient de la validation
- ✅ Centralisé : une seule configuration
- ✅ Transparent : les controllers ne savent pas que la validation existe

---

### 4. GlobalExceptionHandler

**Localisation** : `web.advice.GlobalExceptionHandler`

**Responsabilité** : Conversion des `IllegalArgumentException` en réponses HTTP 400

```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
    ProblemDetail problemDetail = problem(HttpStatus.BAD_REQUEST, "Invalid Parameter",
            ex.getMessage(), "invalidParameter");
    return respond(problemDetail);
}
```

---

## Flux de validation

```
1. Requête HTTP : GET /v2/contracts?page=-1&size=20
                                    ↓
2. Spring MVC détecte un paramètre Pageable dans le controller
                                    ↓
3. ValidatingPageableArgumentResolver.resolveArgument()
   - Extrait "page=-1", "size=20" de la requête HTTP
   - Valide : page < 0 ❌
   - Lance : InvalidPaginationException("Page number must not be less than zero, but was: -1")
   - ❌ Le controller n'est JAMAIS appelé
                                    ↓
4. GlobalExceptionHandler
   - Attrape : InvalidPaginationException
   - Convertit en : ProblemDetail (400 Bad Request)
                                    ↓
5. Réponse HTTP : 400 Bad Request
   {
     "title": "Invalid Parameter",
     "detail": "Page number must not be less than zero, but was: -1",
     "code": "invalidParameter",
     "timestamp": "2025-11-15T...",
     "traceId": "..."
   }

❌ Le repository n'est JAMAIS appelé si la validation échoue
✅ Le repository reçoit toujours un Pageable valide

⚠️ Note : InvalidPaginationException (au lieu de IllegalArgumentException) 
   évite les conflits avec ContractControllerAdvice qui gère aussi IllegalArgumentException
```

---

## Tests

### Tests unitaires : PaginationPropertiesTest
- ✅ Validation de la règle métier `defaultPageSize <= maxPageSize`
- ✅ Valeurs limites (1, 1) et (100, 1000)

### Tests d'intégration : PaginationValidationIT (RestAssured)
- ✅ Requêtes avec `page=-1` → 400 Bad Request
- ✅ Requêtes avec `size=0` → 400 Bad Request
- ✅ Requêtes avec `size=101` → 400 Bad Request (dépasse maxPageSize)
- ✅ Requêtes avec `page=abc` → 400 Bad Request (format invalide)
- ✅ Requêtes valides (`page=0&size=20`) → 200 OK

**Pourquoi RestAssured ?**
- Tests HTTP réels (pas de mock)
- Syntaxe fluide et lisible
- Démarre un serveur réel sur un port aléatoire
- Teste toute la chaîne : HTTP → Resolver → Controller → Repository

---

## Pourquoi cette architecture ?

### ✅ Respecte DDD
- La pagination n'est **pas** dans le domaine (elle n'a pas de sens métier)
- Elle est dans l'infrastructure (contrainte technique/applicative)

### ✅ Respecte CLAUDE.md
- Pas de complexité inutile
- Une responsabilité par classe
- Testable et maintenable

### ✅ Réutilisable
- Un seul resolver pour tous les endpoints paginés
- Configuration centralisée via `application.yml`

### ✅ Conforme à la réponse Stack Overflow
La pagination est une **contrainte applicative**, pas métier. Elle peut varier selon :
- L'application (web vs mobile)
- Le contexte (admin vs public)
- La performance

Notre architecture permet de :
- Changer les limites sans toucher au domaine
- Avoir différentes configurations par profil (dev, prod)
- Ajouter de nouvelles validations facilement

---

## Alternatives écartées

### ❌ Validator explicite dans chaque controller
```java
// À ÉVITER : répétition, oublis faciles
public ResponseEntity<?> list(Pageable pageable) {
    pageableValidator.validate(pageable); // Oubli = bug
    // ...
}
```

### ❌ Validation dans PaginationProperties
```java
// Impossible : PaginationProperties configure les limites,
// mais ne valide pas les requêtes HTTP
```

### ❌ Tests unitaires du Resolver
```java
// Impossible sans contexte Spring complet (MethodParameter null)
// → Tests d'intégration à la place
```

---

## Configuration recommandée

### Environnement de développement
```yaml
app:
  pagination:
    default-page-size: 10
    max-page-size: 50
```

### Production
```yaml
app:
  pagination:
    default-page-size: 20
    max-page-size: 100
```

### Tests
```yaml
app:
  pagination:
    default-page-size: 5
    max-page-size: 20
```

