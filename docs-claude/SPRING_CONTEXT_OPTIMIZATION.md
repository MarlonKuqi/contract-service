# Optimisation du contexte Spring pour les tests d'intégration

## Problème identifié

Le contexte Spring était lancé **deux fois** lors de l'exécution des tests d'intégration :

1. **Contexte 1** : Pour les tests avec `@SpringBootTest(webEnvironment = RANDOM_PORT)` (la majorité)
2. **Contexte 2** : Pour `ContractIsActiveConsistencyIT` qui utilisait `@SpringBootTest` sans paramètre

## Problème secondaire : Gestion des exceptions

Une `MethodArgumentNotValidException` pour les contrats était gérée par le `GlobalExceptionHandler` (générique) au lieu du `ContractControllerAdvice` (spécifique), causant des logs ERROR inutiles.

## Solutions appliquées

### ✅ Correction 1 : Unification du contexte Spring
- Ajout de `webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT` à `ContractIsActiveConsistencyIT`
- Maintenant tous les tests partagent la **même configuration Spring**

### ✅ Correction 2 : Handler d'exceptions pour ContractController
- Ajout de `@ExceptionHandler(MethodArgumentNotValidException.class)` dans `ContractControllerAdvice`
- Les erreurs de validation de contrats sont maintenant gérées correctement (422 au lieu de 500)
- Plus de logs ERROR pour les validations normales

### ✅ Classe de base créée (optionnel mais recommandé)
Une classe abstraite `AbstractIntegrationTest` a été créée avec :
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {
}
```

## Recommandations pour aller plus loin

### Option 1 : Utiliser la classe de base (recommandé)
Faire hériter tous les tests d'intégration de `AbstractIntegrationTest` :

**Avant :**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ClientCrudIT {
    // ...
}
```

**Après :**
```java
class ClientCrudIT extends AbstractIntegrationTest {
    // ...
}
```

**Avantages :**
- ✅ Garantit que tous les tests partagent exactement la même configuration
- ✅ Réduit le code dupliqué (DRY)
- ✅ Plus facile à maintenir (un seul endroit pour changer la config)
- ✅ Spring réutilise le contexte entre tous les tests

### Option 2 : Garder l'état actuel
Si vous préférez garder les annotations explicites, c'est OK **tant que** :
- ✅ Tous les tests utilisent `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- ✅ Tous utilisent `@ActiveProfiles("test")`
- ✅ Tous utilisent `@Import(TestcontainersConfiguration.class)`

## Configuration Testcontainers actuelle

```java
@Bean
@ServiceConnection
PostgreSQLContainer<?> postgresContainer() {
    return new PostgreSQLContainer<>(DockerImageName.parse("postgres:18.0-trixie"))
            .withDatabaseName("contract_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);  // ✅ Conteneur réutilisé entre les exécutions
}
```

Le `.withReuse(true)` permet de réutiliser le conteneur Docker entre plusieurs exécutions de tests (même après redémarrage).

## Résultat attendu

Avec cette correction, le contexte Spring ne devrait démarrer **qu'une seule fois** pour tous les tests d'intégration, ce qui :
- ⚡ Réduit significativement le temps d'exécution des tests
- 💾 Économise la mémoire
- 🚀 Améliore l'expérience développeur

## Tests concernés

Tous les tests d'intégration partagent maintenant la même configuration :
- ✅ `ClientCrudIT`
- ✅ `CompanyLifecycleIT`
- ✅ `ContractIsActiveConsistencyIT` (corrigé)
- ✅ `ContractLifecycleIT`
- ✅ `ContractPaginationIT`
- ✅ `ContractSumRestAssuredIT`
- ✅ `PerformanceAndEdgeCasesIT`
- ✅ `PersonLifecycleIT`

## Vérification

Pour vérifier que le contexte ne démarre qu'une fois, regardez les logs :
```
Starting ContractServiceApplication using Java...
```

Ce message ne devrait apparaître **qu'une seule fois** au début de l'exécution de tous les tests.

