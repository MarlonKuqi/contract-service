# Guide de Lancement des Tests d'Intégration avec Testcontainers

## ✅ Prérequis

### 1. Docker Desktop doit être démarré
```bash
# Vérifier que Docker est actif
docker ps
```

Si Docker n'est pas démarré :
- **Windows** : Démarrer "Docker Desktop" depuis le menu Démarrer
- **Mac** : Ouvrir l'application Docker Desktop
- **Linux** : `sudo systemctl start docker`

### 2. Configuration Testcontainers

Le projet utilise **Testcontainers** pour les tests d'intégration avec PostgreSQL.

**Fichier de configuration** : `src/test/java/com/mk/contractservice/integration/config/TestcontainersConfiguration.java`

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:18.0-trixie"))
                .withDatabaseName("contract_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true); // ✨ Réutilise le container entre les tests !
    }
}
```

### 3. Utilisation dans les Tests

Chaque test d'intégration doit importer la configuration :

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class) // ✅ Important !
class MonTestIT {
    // ...
}
```

---

## 🧪 Tests Disponibles

### Tests d'Intégration

1. **`ContractLifecycleIT`** - Scénarios complets de cycle de vie
   - Création, mise à jour, calcul de somme
   - Contrats expirés vs actifs
   - Filtrage par `updatedSince`

2. **`ContractPaginationIT`** - Tests de pagination (NOUVEAU ✨)
   - Navigation multi-pages
   - Tri par `lastModified`
   - Métadonnées de pagination
   - Combinaison filtres + pagination
   - Page sizes variables
   - Pages vides et au-delà des données

3. **`ClientCrudIT`** - CRUD pour les clients
4. **`CompanyLifecycleIT`** - Cycle de vie des entreprises
5. **`PersonLifecycleIT`** - Cycle de vie des personnes
6. **`PerformanceAndEdgeCasesIT`** - Performance et cas limites

---

## 🚀 Commandes de Lancement

### Lancer TOUS les tests d'intégration
```bash
mvn verify
```

### Lancer UN test d'intégration spécifique
```bash
# Test de pagination
mvn test -Dtest=ContractPaginationIT

# Test de cycle de vie
mvn test -Dtest=ContractLifecycleIT
```

### Lancer UNE méthode de test spécifique
```bash
mvn test -Dtest=ContractPaginationIT#shouldPaginateContractsAcrossMultiplePages
```

### Lancer les tests avec logs DEBUG
```bash
mvn test -Dtest=ContractPaginationIT -Dlogging.level.com.mk.contractservice=DEBUG
```

---

## 🐛 Résolution de Problèmes

### ❌ Erreur : "Connection to localhost:5432 refused"

**Cause** : Docker Desktop n'est pas démarré OU Testcontainers n'est pas importé

**Solutions** :
1. Vérifier que Docker Desktop est bien démarré
   ```bash
   docker ps
   ```

2. Vérifier que le test a `@Import(TestcontainersConfiguration.class)`
   ```java
   @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
   @ActiveProfiles("test")
   @Import(TestcontainersConfiguration.class) // ✅ Doit être présent !
   class MonTestIT {
   ```

3. Vérifier les logs pour voir si Testcontainers démarre le container
   ```
   [main] INFO  🐳 [postgres:18.0-trixie] - Creating container for image: postgres:18.0-trixie
   [main] INFO  🐳 [postgres:18.0-trixie] - Container postgres:18.0-trixie is starting
   [main] INFO  🐳 [postgres:18.0-trixie] - Container postgres:18.0-trixie started
   ```

### ⚠️ Tests très lents

**Cause** : Container PostgreSQL démarre à chaque exécution

**Solution** : Activer la réutilisation du container (déjà fait ✅)
```java
.withReuse(true)
```

Pour que la réutilisation fonctionne, créer `~/.testcontainers.properties` :
```properties
testcontainers.reuse.enable=true
```

### 🔍 Voir les containers Testcontainers actifs
```bash
docker ps --filter "label=org.testcontainers"
```

---

## 📊 Résumé des Tests de Pagination

| Test | Description | Ce qui est vérifié |
|------|-------------|-------------------|
| `shouldPaginateContractsAcrossMultiplePages` | Navigation sur 3 pages | Pages 0, 1, 2 avec métadonnées |
| `shouldReturnEmptyPageWhenBeyondAvailableData` | Page au-delà des données | Page vide avec bonnes métadonnées |
| `shouldSortContractsByLastModifiedDescending` | Tri par date | Ordre décroissant par lastModified |
| `shouldFilterAndPaginateByUpdatedSince` | Filtre + pagination | Combinaison updatedSince + page |
| `shouldUseDefaultPageSizeWhenNotSpecified` | Page size par défaut | Size = 20 par défaut |
| `shouldHandleDifferentPageSizes` | Variations de taille | size=5, size=25, size=50 |
| `shouldReturnCorrectMetadataForSinglePage` | Page unique | isFirst=true, isLast=true |
| `shouldReturnEmptyPageWhenNoContracts` | Aucun contrat | totalElements=0, content=[] |
| `shouldOnlyReturnActiveContractsInPagination` | Filtrage actifs | Exclut contrats expirés |

**Total : 9 tests de pagination ✅**

---

## 🎯 Checklist avant de merger

- [x] Docker Desktop est démarré
- [x] Tous les tests passent localement
- [x] Testcontainers configuré avec `@Import`
- [x] Tests de pagination créés (9 tests)
- [x] Documentation à jour
- [ ] Tests lancés avec `mvn verify`
- [ ] Pas de tests flaky (réexécuter 2-3 fois)

---

## 📚 Ressources

- [Testcontainers Documentation](https://www.testcontainers.org/)
- [Spring Boot Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [RestAssured Documentation](https://rest-assured.io/)

