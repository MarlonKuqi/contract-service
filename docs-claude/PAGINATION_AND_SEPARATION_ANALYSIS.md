# Tests de Pagination Manquants et Séparation Domain/Infrastructure

## 🧪 Tests de Pagination - GAP Analysis

### ✅ Tests Existants (Unit Tests)
- `ContractApplicationServiceTest.shouldReturnActiveContracts()` - Test basique avec mock
- `ContractApplicationServiceTest.shouldReturnEmptyPage()` - Test page vide

### ✅ Tests d'Intégration Créés - `ContractPaginationIT.java`

#### 1. ✅ **Test de Navigation entre Pages**
- `shouldPaginateContractsAcrossMultiplePages()` - Navigation sur 3 pages (25 contrats, page size 10)
- Vérifie `totalElements`, `totalPages`, `isFirst`, `isLast`, `number`, `size`

#### 2. ✅ **Test des Paramètres de Tri**
- `shouldSortContractsByLastModifiedDescending()` - Tri par lastModified DESC
- Vérifie l'ordre des contrats retournés

#### 3. ✅ **Test des Métadonnées de Pagination**
- `shouldReturnCorrectMetadataForSinglePage()` - Métadonnées pour page unique
- `shouldReturnEmptyPageWhenBeyondAvailableData()` - Page au-delà des données
- `shouldReturnEmptyPageWhenNoContracts()` - Page vide (aucun contrat)

#### 4. ✅ **Test Pagination + Filtre updatedSince**
- `shouldFilterAndPaginateByUpdatedSince()` - Combinaison filtre + pagination
- Créé 5 vieux + 10 récents contrats, vérifie le filtre avec pagination

#### 5. ✅ **Test Page Size Variations**
- `shouldUseDefaultPageSizeWhenNotSpecified()` - Page size par défaut (20)
- `shouldHandleDifferentPageSizes()` - Teste size=5, size=25, size=50

#### 6. ✅ **Test Filtrage des Contrats Actifs**
- `shouldOnlyReturnActiveContractsInPagination()` - Exclut les contrats expirés
- Créé 5 expirés + 10 actifs, vérifie que seuls les actifs sont paginés

### 📊 Couverture des Tests de Pagination

| Fonctionnalité | Couverture |
|----------------|-----------|
| Navigation multi-pages | ✅ |
| Tri (sort parameter) | ✅ |
| Métadonnées pagination | ✅ |
| Filtre updatedSince | ✅ |
| Page size variations | ✅ |
| Filtrage actifs/expirés | ✅ |
| Pages vides | ✅ |
| **TOTAL** | **100%** |

---

### ⚠️ Tests Manquants (Optional - Priorité BASSE)

#### **Test Page Size Limits** (Non implémenté)
```java
@Test
@DisplayName("Should respect max page size limit")
void shouldEnforceMaxPageSizeLimit() {
    // GET /contracts?size=1000
    // Vérifier que la taille est limitée (ex: max 100)
}
```
**Note**: Spring Data ne limite pas automatiquement la page size. 
Si nécessaire, ajouter une validation dans le controller avec `@Max(100)` sur le paramètre `size`.

---

## 🏗️ Ce qui Manque pour Séparation Totale Domain/Infrastructure

### ❌ **Unique Dépendance Restante**

**Fichier**: `ContractRepository.java` (dans le domaine)  
**Dépendances**: 
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

### Solutions Possibles

#### **Option 1: Créer des Wrappers Domain (DDD Puriste)** ✨

Créer dans le domaine :

```java
// domain/common/PageRequest.java
public record PageRequest(int pageNumber, int pageSize, Sort sort) {
    // Pas de dépendance Spring
}

// domain/common/PageResult.java
public record PageResult<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean isFirst,
    boolean isLast
) {}

// domain/common/Sort.java
public record Sort(String property, Direction direction) {
    public enum Direction { ASC, DESC }
}
```

Puis dans l'infrastructure, créer un adapter :

```java
// infrastructure/adapter/PaginationAdapter.java
public class PaginationAdapter {
    public static Pageable toSpring(PageRequest domainRequest) {
        // Conversion domain → Spring
    }
    
    public static <T> PageResult<T> fromSpring(Page<T> springPage) {
        // Conversion Spring → domain
    }
}
```

**Avantages**:
- ✅ Domaine 100% pur, zéro dépendance
- ✅ Contrôle total sur le modèle de pagination
- ✅ Peut changer de framework sans toucher au domaine

**Inconvénients**:
- ❌ Beaucoup de code boilerplate
- ❌ Conversions constantes
- ❌ Réinvente la roue (Spring Data Pageable est un standard)

---

#### **Option 2: Accepter la Dépendance Spring Data (Pragmatique)** ✅ **ACTUEL**

Garder `Page` et `Pageable` dans le repository du domaine.

**Avantages**:
- ✅ Simple, standard de l'industrie
- ✅ Pas de boilerplate
- ✅ Spring Data est stable et bien documenté

**Inconvénients**:
- ⚠️ Couplage léger au framework
- ⚠️ Si un jour on change de framework → travail d'adaptation

**Justification**: 
Spring Data Pageable est devenu un **standard de facto** pour la pagination en Java. 
Le coût de créer des wrappers domain ne justifie pas le bénéfice pour ce projet.

**Status**: ✅ **Documenté dans le code et accepté**

---

### 🎯 Verdict Final

**Pour une séparation STRICTEMENT PURE (100% DDD orthodoxe):**
- [ ] Implémenter Option 1 (wrappers domain pour pagination)
- [ ] Ajouter des adapters dans l'infrastructure

**Pour une séparation PRAGMATIQUE et MAINTENABLE:**
- [x] **Conserver la dépendance Spring Data Page/Pageable** ✅
- [x] **Bien documentée dans le code** ✅
- [x] **Mentionnée dans DDD_SEPARATION_CHECKUP.md** ✅

---

## 📊 Score de Séparation Domain/Infrastructure

| Critère | Status | Note |
|---------|--------|------|
| Aucune annotation JPA dans le domaine | ✅ | 10/10 |
| Aucune entité JPA dans le domaine | ✅ | 10/10 |
| Pattern Repository (interface/implémentation) | ✅ | 10/10 |
| Anti-Corruption Layer (Assemblers) | ✅ | 10/10 |
| Exceptions domaine sans dépendances HTTP | ✅ | 10/10 |
| Aucune dépendance Spring dans domaine | ⚠️ (Page/Pageable) | 8/10 |
| Tests unitaires domaine isolés | ✅ | 10/10 |
| **SCORE GLOBAL** | | **9.7/10** |

**Conclusion**: La séparation est **excellente** avec un compromis pragmatique bien documenté sur la pagination.

---

## 🚀 Recommandations

### ✅ Complété
1. ✅ **Tests d'intégration pour la pagination** - `ContractPaginationIT.java` créé avec 9 tests
2. ✅ **Testcontainers configuré** - `@Import(TestcontainersConfiguration.class)` ajouté

### Priorité MOYENNE
3. ⚠️ Documenter les paramètres de pagination dans l'OpenAPI du controller
4. ⚠️ Considérer l'ajout de contraintes sur la page size (max 100 items) avec `@Max(100)`
5. ⚠️ Ajouter des tests de performance pour la pagination avec JOIN FETCH

### Priorité BASSE (Optionnel)
6. 💭 Si le projet évolue vers un domaine multi-framework, implémenter Option 1 (wrappers domain)
7. 💭 Sinon, **conserver l'approche actuelle** qui est saine et maintenable

