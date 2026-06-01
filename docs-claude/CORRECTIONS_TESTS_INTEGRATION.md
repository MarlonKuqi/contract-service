# Corrections des tests d'intégration - Session du 2025-11-09

## Résumé des problèmes corrigés

### 1. ❌ Tests échouaient avec des codes HTTP incorrects

**Problèmes initiaux :**
- `ContractLifecycleIT.shouldCompleteContractLifecycle` : 404 au lieu de 200
- `ContractLifecycleIT.shouldRejectInvalidContractData` : 500 au lieu de 400/422
- `PersonLifecycleIT.shouldRejectMissingRequiredFields` : 400 au lieu de 422
- `ClientCrudIT.shouldRejectUpdateWithInvalidEmail` : 400 au lieu de 422
- `CompanyLifecycleIT` : plusieurs erreurs 400 au lieu de 422
- `PersonLifecycleIT` : erreurs de validation 400 au lieu de 422

## Solutions appliquées

### ✅ 1. Correction de l'ordre des paramètres dans les tests

**Fichier :** `ContractLifecycleIT.java`

**Problème :** L'ordre des paramètres dans les appels REST était incorrect
```java
// ❌ AVANT
get("/v1/contracts/{contractId}?clientId={clientId}", testClient.getId(), contractId)
patch("/v1/contracts/{contractId}/cost?clientId={clientId}", testClient.getId(), contractId)

// ✅ APRÈS
get("/v1/contracts/{contractId}?clientId={clientId}", contractId, testClient.getId())
patch("/v1/contracts/{contractId}/cost?clientId={clientId}", contractId, testClient.getId())
```

### ✅ 2. Unification des codes de statut HTTP pour les validations

**Stratégie adoptée :** Toutes les validations (Jakarta et Domain) retournent **422 UNPROCESSABLE_ENTITY**

#### a) ClientControllerAdvice
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
// Changé de BAD_REQUEST (400) → UNPROCESSABLE_ENTITY (422)

@ExceptionHandler(HttpMessageNotReadableException.class)
// Changé de BAD_REQUEST (400) → UNPROCESSABLE_ENTITY (422)
```

#### b) ContractControllerAdvice
```java
@ExceptionHandler(InvalidContractCostException.class)
// Changé de BAD_REQUEST (400) → UNPROCESSABLE_ENTITY (422)

@ExceptionHandler(DomainValidationException.class)
// Ajouté pour gérer les validations domain (422)

@ExceptionHandler(MethodArgumentNotValidException.class)
// Ajouté pour gérer les validations Jakarta (422)
```

### ✅ 3. Optimisation du contexte Spring

**Problème :** Le contexte Spring démarrait 2 fois
- Une fois pour les tests avec `webEnvironment = RANDOM_PORT`
- Une fois pour `ContractIsActiveConsistencyIT` sans cette configuration

**Solution :**
```java
// ❌ AVANT
@SpringBootTest
class ContractIsActiveConsistencyIT {

// ✅ APRÈS
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContractIsActiveConsistencyIT {
```

**Résultat :** Le contexte Spring ne démarre plus qu'**une seule fois** pour tous les tests.

### ✅ 4. Classe abstraite pour les tests (optionnel)

**Créée :** `AbstractIntegrationTest` pour centraliser la configuration commune

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {
}
```

**Usage possible (optionnel) :**
```java
// Au lieu de répéter les annotations
class ClientCrudIT extends AbstractIntegrationTest {
    // ...
}
```

## Stratégie de codes HTTP adoptée

| Type d'erreur | Code HTTP | Handler |
|---------------|-----------|---------|
| Champ manquant (JSON) | 422 | `HttpMessageNotReadableException` |
| Format invalide (@Email, @Pattern) | 422 | `MethodArgumentNotValidException` |
| Validation domain (ContractCost < 0) | 422 | `DomainValidationException` ou `InvalidContractCostException` |
| Ressource non trouvée | 404 | `ClientNotFoundException`, `ContractNotFoundException` |
| Accès refusé | 403 | `ContractNotOwnedByClientException` |
| Conflit (email existe déjà) | 409 | `ClientAlreadyExistsException` |
| Contrat expiré | 422 | `ExpiredContractException` |

## Tests de validation mis à jour

Tous les tests attendent maintenant **422** pour les validations :
- ✅ `PersonLifecycleIT.shouldRejectMissingRequiredFields`
- ✅ `PersonLifecycleIT.shouldRejectInvalidEmailFormat`
- ✅ `PersonLifecycleIT.shouldRejectInvalidPhoneFormat`
- ✅ `CompanyLifecycleIT.shouldRejectMissingCompanyIdentifier`
- ✅ `CompanyLifecycleIT.shouldValidateCompanyEmailFormat`
- ✅ `ClientCrudIT.shouldRejectUpdateWithInvalidEmail`
- ✅ `ContractLifecycleIT.shouldRejectInvalidContractData`

## Bénéfices

### Performance
- ⚡ **Contexte Spring démarré 1 seule fois** au lieu de 2
- 🚀 Temps d'exécution des tests réduit significativement
- 💾 Moins de mémoire consommée

### Maintenabilité
- ✅ Codes HTTP cohérents et prévisibles
- ✅ Gestion des exceptions bien séparée par contrôleur
- ✅ Moins de logs ERROR pour des validations normales
- ✅ Tests plus fiables et explicites

### Qualité
- ✅ Séparation claire : validation structurelle vs validation business (même si toutes → 422)
- ✅ Messages d'erreur détaillés avec champs et valeurs rejetées
- ✅ TraceId et timestamp dans toutes les erreurs

## Fichiers modifiés

### Advice (Exception Handlers)
1. `ClientControllerAdvice.java`
   - HttpMessageNotReadableException → 422
   - MethodArgumentNotValidException → 422

2. `ContractControllerAdvice.java`
   - Ajout de DomainValidationException → 422
   - Ajout de MethodArgumentNotValidException → 422
   - InvalidContractCostException → 422

### Tests
1. `ContractLifecycleIT.java` - Correction ordre paramètres
2. `PersonLifecycleIT.java` - Attente 422 au lieu de 400
3. `ContractIsActiveConsistencyIT.java` - Ajout webEnvironment

### Documentation
1. `SPRING_CONTEXT_OPTIMIZATION.md` - Guide d'optimisation
2. `AbstractIntegrationTest.java` - Classe de base (optionnel)
3. `CORRECTIONS_TESTS_INTEGRATION.md` - Ce fichier

## Statut final

✅ **TOUS LES TESTS PASSENT**

Le projet a maintenant :
- Une gestion cohérente des erreurs HTTP
- Des tests d'intégration optimisés
- Un contexte Spring unique réutilisé
- Une séparation claire des responsabilités dans les exception handlers

