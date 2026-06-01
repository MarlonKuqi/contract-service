# Flux complet de la validation de pagination

## Au démarrage de l'application (une seule fois)

```
1. Spring Boot démarre
         ↓
2. Lit application.yml
   app:
     pagination:
       default-page-size: 20
       max-page-size: 100
         ↓
3. Crée PaginationProperties
   - defaultPageSize = 20
   - maxPageSize = 100
         ↓
4. Valide avec @Min/@Max
   - defaultPageSize >= 1 ✅
   - defaultPageSize <= 100 ✅
   - maxPageSize >= 1 ✅
   - maxPageSize <= 1000 ✅
   ❌ Si invalide → Application ne démarre PAS
   ✅ Si valide → Continue
         ↓
5. PaginationConfig utilise PaginationProperties
   - Crée ValidatingPageableArgumentResolver(paginationProperties)
   - Configure maxPageSize = 100
   - Configure defaultPageSize = 20
         ↓
6. Enregistre le resolver dans Spring MVC
   - Tous les paramètres Pageable passeront par ce resolver
         ↓
7. Application prête ✅
```

---

## À chaque requête HTTP (runtime)

```
Requête HTTP: GET /v2/contracts?page=-1&size=20&clientId=xxx
         ↓
┌─────────────────────────────────────────────────────┐
│ Spring MVC détecte la méthode du controller :      │
│ listActive(@RequestParam UUID clientId,            │
│            Pageable pageable, ...)                  │
│                                                     │
│ → Besoin de créer un Pageable                      │
└─────────────────┬───────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────┐
│ ValidatingPageableArgumentResolver.resolveArgument()│
│                                                     │
│ 1. Extrait les paramètres HTTP :                   │
│    - pageParam = "-1"                               │
│    - sizeParam = "20"                               │
│                                                     │
│ 2. Valide page :                                    │
│    - Parse: page = -1                               │
│    - Vérifie: page >= 0 ? ❌ NON                    │
│    → Lance: InvalidPaginationException(             │
│        "Page number must not be less than zero")   │
└─────────────────┬───────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────┐
│ GlobalExceptionHandler                              │
│ @ExceptionHandler(InvalidPaginationException.class) │
│                                                     │
│ Attrape l'exception                                 │
│ Crée un ProblemDetail (400 Bad Request)            │
└─────────────────┬───────────────────────────────────┘
                  ↓
HTTP Response: 400 Bad Request
{
  "title": "Invalid Parameter",
  "detail": "Page number must not be less than zero, but was: -1",
  "code": "invalidParameter"
}

❌ Le controller ContractController.listActive() N'EST JAMAIS APPELÉ
❌ Le repository N'EST JAMAIS APPELÉ
❌ Aucun Pageable N'EST CRÉÉ
```

---

## Si la requête est valide

```
Requête HTTP: GET /v2/contracts?page=0&size=20&clientId=xxx
         ↓
┌─────────────────────────────────────────────────────┐
│ ValidatingPageableArgumentResolver.resolveArgument()│
│                                                     │
│ 1. Extrait les paramètres :                        │
│    - pageParam = "0"                                │
│    - sizeParam = "20"                               │
│                                                     │
│ 2. Valide page :                                    │
│    - page = 0 >= 0 ? ✅ OUI                         │
│                                                     │
│ 3. Valide size :                                    │
│    - size = 20 >= 1 ? ✅ OUI                        │
│    - size = 20 <= maxPageSize(100) ? ✅ OUI         │
│                                                     │
│ 4. Appelle super.resolveArgument()                 │
│    → Délègue à Spring Data pour créer le Pageable  │
└─────────────────┬───────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────┐
│ Spring Data PageableHandlerMethodArgumentResolver  │
│ (classe parente)                                    │
│                                                     │
│ Crée: PageRequest.of(0, 20, Sort.by(...))          │
└─────────────────┬───────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────────────────┐
│ ContractController.listActive()                     │
│                                                     │
│ Reçoit:                                             │
│  - clientId = xxx                                   │
│  - pageable = PageRequest{page=0, size=20}          │
│                                                     │
│ Appelle: contractApplicationService.               │
│          getActiveContractsPageable(clientId,       │
│                                     updatedSince,   │
│                                     pageable)       │
└─────────────────┬───────────────────────────────────┘
                  ↓
Repository appelé avec Pageable valide ✅
         ↓
HTTP Response: 200 OK
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150
}
```

---

## Rôle de chaque composant

### PaginationProperties
- ⚙️ **Rôle** : Stocke les limites de configuration (defaultPageSize, maxPageSize)
- 📍 **Quand** : Créé au démarrage, lu depuis `application.yml`
- ✅ **Validation** : Annotations `@Min/@Max` validées par Spring au démarrage
- ❌ **Ne crée PAS** le Pageable
- ❌ **Ne valide PAS** les requêtes HTTP

### ValidatingPageableArgumentResolver
- 🛡️ **Rôle** : Valide les paramètres `page` et `size` des requêtes HTTP
- 📍 **Quand** : À chaque requête HTTP contenant un paramètre `Pageable`
- ✅ **Validation** : page >= 0, size >= 1, size <= maxPageSize
- ✅ **Utilise** : `paginationProperties.maxPageSize()` pour valider
- ❌ **Ne crée PAS** le Pageable (délègue à Spring Data)
- ✅ **Lance** : `InvalidPaginationException` si invalide

### Spring Data PageableHandlerMethodArgumentResolver (parent)
- 🏗️ **Rôle** : Crée le `Pageable` à partir des paramètres HTTP
- 📍 **Quand** : Après validation réussie par notre resolver
- ✅ **Crée** : `PageRequest.of(page, size, sort)`
- ⚠️ **Ne valide PAS** les valeurs négatives (notre resolver le fait avant)

### GlobalExceptionHandler
- 🚨 **Rôle** : Convertit `InvalidPaginationException` en réponse HTTP 400
- 📍 **Quand** : Quand une exception de pagination est lancée
- ✅ **Retourne** : ProblemDetail avec message d'erreur clair

---

## Points clés à retenir

1. **PaginationProperties** = Configuration statique (lue au démarrage)
2. **ValidatingPageableArgumentResolver** = Validation dynamique (à chaque requête)
3. **Spring Data** = Création du Pageable (après validation)
4. **Le controller ne voit JAMAIS de paramètres invalides**
5. **Le repository ne voit JAMAIS de Pageable invalide**

---

## Pourquoi PaginationProperties est une classe, pas un record ?

### Problème avec les records
```java
// ❌ NE FONCTIONNE PAS BIEN
@ConfigurationProperties(prefix = "app.pagination")
public record PaginationProperties(@Min(1) int defaultPageSize, ...) {
```

**Problèmes** :
- Spring ne peut pas facilement setter les valeurs (les records sont immutables)
- Les annotations de validation peuvent ne pas fonctionner correctement
- Binding des properties complexe

### Solution avec une classe
```java
// ✅ FONCTIONNE BIEN
@ConfigurationProperties(prefix = "app.pagination")
public class PaginationProperties {
    @Min(1) private int defaultPageSize;
    // + getters/setters
}
```

**Avantages** :
- ✅ Spring injecte via les setters
- ✅ Validation fiable avec `@Validated`
- ✅ Pattern standard pour `@ConfigurationProperties`

