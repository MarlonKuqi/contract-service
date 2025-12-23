# Instructions pour GitHub Copilot - Contract Service

## 📚 Documentation
- **[project-architecture.md](project-architecture.md)** : Vue d'ensemble de l'architecture DDD/Hexagonale
- **[immutable-entities.md](immutable-entities.md)** : Règles de conception des entités immuables

## Commandes Maven
- **Toujours utiliser** `.\mvnw.cmd` au lieu de `mvn` sur Windows
- Tests unitaires : `.\mvnw.cmd test`
- Tests d'intégration : `.\mvnw.cmd test -Dtest=*IT`
- Clean + verify complet : `.\mvnw.cmd clean verify`

## Architecture
- **📖 Voir [project-architecture.md](project-architecture.md)** pour la structure détaillée
- Pattern DDD + Clean Architecture + Architecture Hexagonale
- Séparation stricte : Domain (pur) → Application → Infrastructure/Web

## Conventions de code
- **Lombok** : utiliser `@RequiredArgsConstructor` + `@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)`
- **Immutabilité** : 
  - Tous les champs de services doivent être `private final`
  - **Aggregates** : voir [immutable-entities.md](immutable-entities.md) pour les règles complètes
- **Value Objects** : utiliser des records Java pour les VOs
- **Aggregates** : Person et Company héritent de Client (sealed class)

## Tests
- Tests unitaires dans `src/test/java`
- Tests d'intégration : suffixe `*IT.java`
- **Tests d'architecture** : `HexagonalArchitectureTest.java` (ArchUnit)
  - Valide automatiquement les règles de dépendances
  - Échec du build si violation de l'architecture hexagonale
- Utiliser `@ExtendWith(MockitoExtension.class)` pour les tests unitaires
- Utiliser `@SpringBootTest` pour les tests d'intégration

## Notes importantes
- Les méthodes métier ont été déplacées des services de domaine vers des use cases
- `ClientService` et `ContractService` ne contiennent que la logique partagée
- Chaque use case a son propre service dédié

