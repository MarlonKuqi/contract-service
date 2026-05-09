# Fix : Contexte Spring qui se charge deux fois

## Problème
Le contexte Spring se chargeait deux fois lors de l'exécution des tests d'intégration.

## Cause
`PaginationValidationIT` n'utilisait pas exactement la même configuration `@SpringBootTest` que les autres tests d'intégration, ce qui forçait Spring à créer un nouveau contexte.

## Solution appliquée

### Avant
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Pagination Validation Integration Tests")
class PaginationValidationIT {
```

### Après
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DisplayName("Pagination Validation Integration Tests")
class PaginationValidationIT {
```

## Annotations ajoutées

1. **`@ActiveProfiles("test")`** 
   - Active le profil "test" comme les autres tests d'intégration
   
2. **`@Import(TestcontainersConfiguration.class)`**
   - Importe la configuration Testcontainers partagée
   - Le container PostgreSQL utilise `.withReuse(true)`

## Comment Spring réutilise les contextes

Spring réutilise le même contexte ApplicationContext si **toutes** ces conditions sont remplies :
- ✅ Mêmes annotations `@SpringBootTest`
- ✅ Mêmes `@ActiveProfiles`
- ✅ Mêmes `@Import`
- ✅ Mêmes properties

Si **UNE SEULE** de ces conditions diffère, Spring crée un nouveau contexte.

## Résultat

Maintenant tous les tests d'intégration partagent le **même** contexte Spring :
- `ContractPaginationIT`
- `ContractLifecycleIT`
- `ClientCrudIT`
- `PaginationValidationIT` ← **Maintenant aussi !**
- etc.

Le contexte se charge **une seule fois** au début de la suite de tests, puis est réutilisé.

## Vérification

Pour vérifier qu'il n'y a plus qu'un seul contexte, cherchez dans les logs :

```
Started ContractServiceApplication in X.XXX seconds
```

Ce message ne devrait apparaître **qu'une seule fois** au début de tous les tests d'intégration.

## Configuration partagée par tous les tests IT

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
```

⚠️ **Important** : Ne modifiez jamais ces annotations sans vérifier l'impact sur TOUS les tests d'intégration.

