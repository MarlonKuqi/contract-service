# 📝 Décision de Design - Codes HTTP pour Erreurs de Déserialisation

**Date** : 2025-11-13  
**Contexte** : Choix entre 400 (Bad Request) et 422 (Unprocessable Entity) pour `HttpMessageNotReadableException`

---

## 🎯 Décision Finale

**Tous les problèmes de déserialisation Jackson → 422 Unprocessable Entity**

---

## 💡 Raisonnement

### Option A (Complexe - REJETÉE) ❌
Distinguer syntaxe (400) vs validation (422) :
```java
if (!"type".equals(fieldName)) {
    return 422; // Champ requis manquant
} else {
    return 400; // Champ discriminateur manquant
}
```

**Problèmes** :
- ❌ Code complexe à comprendre
- ❌ Logique arbitraire (pourquoi `type` serait différent ?)
- ❌ Maintenabilité difficile
- ❌ Peu de gain pour l'utilisateur

### Option B (Simple - CHOISIE) ✅
Tout traiter comme erreur de validation :
```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<ProblemDetail> handleNotReadable(...) {
    // Tous les cas → 422
    return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Validation Failed", ...);
}
```

**Avantages** :
- ✅ **Simplicité** : Code facile à lire et maintenir
- ✅ **Cohérence** : Aligné avec Jakarta Validation (erreurs de données = 422)
- ✅ **Standards REST** : 422 = "l'entité ne peut pas être traitée"
- ✅ **Pragmatisme** : L'utilisateur reçoit un message clair dans tous les cas

---

## 📊 Cas Couverts

| Scénario | JSON Envoyé | Code | Message |
|----------|-------------|------|---------|
| **Champ discriminateur manquant** | `{"name": "Alice"}` (pas de `type`) | 422 | Missing or invalid field: 'type' |
| **Champ requis manquant** | `{"type": "PERSON", "name": "Bob"}` (pas de `email`) | 422 | Missing or invalid field: 'email' |
| **Type invalide** | `{"birthDate": "invalid"}` | 422 | Invalid value 'invalid' for field 'birthDate' |
| **JSON malformé** | `{invalid json}` | 422 | Malformed JSON or invalid payload |

---

## 🔍 Comparaison avec Standards REST

### RFC 7231 (HTTP)
- **400 Bad Request** : "la requête ne peut pas être comprise par le serveur en raison d'une **syntaxe malformée**"
- **422 Unprocessable Entity** (RFC 4918 WebDAV) : "le serveur comprend le content-type et la syntaxe, mais ne peut pas **traiter les instructions contenues**"

### Interprétation
- JSON `{invalid}` : Techniquement syntaxe → devrait être 400
- JSON `{"type": "PERSON"}` (email manquant) : Validation → devrait être 422

**MAIS** : Dans la pratique, Jackson lance `HttpMessageNotReadableException` pour **tous** les cas (syntaxe ET champs manquants).

### Notre Choix
Privilégier **422 pour tout** car :
1. L'utilisateur envoie des **données** (pas juste une requête malformée)
2. Le message d'erreur indique **quel champ** est le problème
3. C'est cohérent avec Jakarta Validation
4. C'est plus simple à implémenter et maintenir

---

## 🌐 Exemples d'APIs Réelles

### GitHub API
```
POST /repos/owner/repo/issues
Body: {"title": ""}

Response: 422 Unprocessable Entity
{
  "message": "Validation Failed",
  "errors": [{"field": "title", "code": "missing"}]
}
```

### Stripe API
```
POST /v1/customers
Body: {"email": "invalid"}

Response: 422 Unprocessable Entity
```

**Constat** : Les grandes APIs utilisent 422 pour les erreurs de validation/données, même quand le JSON est syntaxiquement correct.

---

## ✅ Conclusion

**Choix final** : 422 pour tous les `HttpMessageNotReadableException`

**Raison principale** : **Simplicité et maintenabilité** > purisme théorique

**Impact utilisateur** : Aucun (le message d'erreur est clair dans tous les cas)

**Impact développeur** : Code facile à comprendre et modifier

---

## 📝 Implémentation

### ClientControllerAdvice.java
Gère les erreurs de déserialisation pour **ClientController** (`POST /v1/clients`, `PUT /v1/clients/{id}`)

```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<ProblemDetail> handleNotReadable(final HttpMessageNotReadableException ex) {
    log.debug("Malformed JSON in client request: {}", ex.getMessage());

    String detail = "Malformed JSON or invalid payload.";

    Throwable cause = ex.getCause();
    if (cause instanceof InvalidFormatException ife) {
        detail = String.format("Invalid value '%s' for field '%s'. Expected type: %s",
                ife.getValue(), ife.getPath().get(0).getFieldName(), 
                ife.getTargetType().getSimpleName());
    } else if (cause instanceof MismatchedInputException mie) {
        if (!mie.getPath().isEmpty()) {
            detail = String.format("Missing or invalid field: '%s'", 
                    mie.getPath().get(0).getFieldName());
        }
    }

    // Simple et cohérent : tous les problèmes de données = 422
    final ProblemDetail problemDetail = problem(
        HttpStatus.UNPROCESSABLE_ENTITY, 
        "Validation Failed", 
        detail, 
        "validationError"
    );
    return respond(problemDetail);
}
```

### ContractControllerAdvice.java
Gère les erreurs de déserialisation pour **ContractController** (`POST /v1/contracts`, `PATCH /v1/contracts/{id}/cost`)

```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex) {
    log.debug("Malformed JSON in contract request: {}", ex.getMessage());

    String detail = "Malformed JSON or invalid payload.";

    Throwable cause = ex.getCause();
    if (cause instanceof InvalidFormatException ife) {
        detail = String.format("Invalid value '%s' for field '%s'. Expected type: %s",
                ife.getValue(), ife.getPath().get(0).getFieldName(), 
                ife.getTargetType().getSimpleName());
    } else if (cause instanceof MismatchedInputException mie) {
        if (!mie.getPath().isEmpty()) {
            detail = String.format("Missing or invalid field: '%s'", 
                    mie.getPath().get(0).getFieldName());
        }
    }

    // Tous les problèmes de déserialisation = 422 (validation)
    ProblemDetail pd = problem(
        HttpStatus.UNPROCESSABLE_ENTITY, 
        "Validation Failed", 
        detail, 
        "validationError"
    );
    return respond(pd);
}
```

### GlobalExceptionHandler.java
Gère uniquement les exceptions **non spécifiques** à Client ou Contract :
- `ContractNotFoundException` (404)
- `ContractNotOwnedByClientException` (403)
- `ExpiredContractException` (422)
- `MissingServletRequestParameterException` (400)
- `Exception` (500 - catch-all)

**Note** : `HttpMessageNotReadableException` n'est **plus** dans GlobalExceptionHandler car géré par les ControllerAdvice spécifiques.

---

**Décision validée par l'équipe** : ✅ Approche pragmatique et maintenable

