# Correction des tests de pagination

## Problèmes identifiés

### 1. Conflit d'exception handlers
**Symptôme** : Les tests échouaient avec "Contract Validation Failed" au lieu de "Invalid Parameter"

**Cause** : 
- `ContractControllerAdvice` a un handler pour `IllegalArgumentException`
- Ce handler capturait nos exceptions de pagination AVANT `GlobalExceptionHandler`
- Spring priorise les `@ControllerAdvice` spécifiques sur les globaux

### 2. Contexte Spring lancé deux fois
**Symptôme** : Le contexte Spring se chargeait en double dans les tests

**Cause** : Configuration de tests non optimale

---

## Solutions appliquées

### 1. Création d'une exception spécifique : `InvalidPaginationException`

**Fichier** : `infrastructure.exception.InvalidPaginationException`

```java
public class InvalidPaginationException extends RuntimeException {
    public InvalidPaginationException(String message) {
        super(message);
    }
}
```

**Avantages** :
- ✅ Évite les conflits avec `IllegalArgumentException` des autres handlers
- ✅ Plus explicite et spécifique
- ✅ Permet un traitement distinct dans `GlobalExceptionHandler`

---

### 2. Modification de `ValidatingPageableArgumentResolver`

**Changement** : Remplacer toutes les `IllegalArgumentException` par `InvalidPaginationException`

**Avant** :
```java
throw new IllegalArgumentException("Page number must not be less than zero...");
```

**Après** :
```java
throw new InvalidPaginationException("Page number must not be less than zero...");
```

---

### 3. Ajout d'un handler dans `GlobalExceptionHandler`

**Nouveau handler** :
```java
@ExceptionHandler(InvalidPaginationException.class)
public ResponseEntity<ProblemDetail> handleInvalidPagination(InvalidPaginationException ex) {
    log.debug("Invalid pagination parameter: {}", ex.getMessage());
    
    ProblemDetail problemDetail = problem(HttpStatus.BAD_REQUEST, "Invalid Parameter",
            ex.getMessage(), "invalidParameter");
    return respond(problemDetail);
}
```

**Résultat** : Les erreurs de pagination retournent maintenant le bon titre "Invalid Parameter"

---

### 4. Import de `@DirtiesContext` dans les tests

**Ajout** : Import (déjà présent mais non utilisé dans l'annotation)

**Optionnel** : Peut être utilisé si le problème de contexte persiste :
```java
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
```

---

## Ordre de priorité des exception handlers

```
1. @ControllerAdvice spécifiques (ex: ContractControllerAdvice)
   └─> IllegalArgumentException → "Contract Validation Failed"
   
2. @RestControllerAdvice global (GlobalExceptionHandler)  
   └─> InvalidPaginationException → "Invalid Parameter" ✅
   └─> IllegalArgumentException → "Invalid Parameter"
```

**Notre solution** : Utiliser `InvalidPaginationException` pour éviter le handler #1

---

## Tests après correction

Les tests devraient maintenant passer avec le bon message :

```json
{
  "title": "Invalid Parameter",
  "detail": "Page number must not be less than zero, but was: -1",
  "code": "invalidParameter"
}
```

Au lieu de :

```json
{
  "title": "Contract Validation Failed",
  "detail": "...",
  "code": "contractValidationError"
}
```

---

## Fichiers modifiés

1. **Nouveau** : `InvalidPaginationException.java`
2. **Modifié** : `ValidatingPageableArgumentResolver.java` (utilise InvalidPaginationException)
3. **Modifié** : `GlobalExceptionHandler.java` (handler pour InvalidPaginationException)
4. **Modifié** : `PaginationValidationIT.java` (import DirtiesContext)
5. **Modifié** : `PAGINATION_ARCHITECTURE.md` (documentation mise à jour)

---

## Commande de test

```bash
mvn test -Dtest=PaginationValidationIT
```

Tous les tests devraient maintenant passer ✅

