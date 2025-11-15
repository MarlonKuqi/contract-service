# Méthode isActive() - Intégration dans la Logique Métier

## ✅ Résumé des Changements

### 1. **ContractPeriod.java**
- ❌ Supprimé `isActiveAt(LocalDateTime referenceTime)` - Jamais utilisé
- ✅ Conservé `isActive()` - Vérifie si la période est active maintenant
- ✅ Tests nettoyés dans `ContractPeriodTest.java`

### 2. **Contract.java**
- ❌ Supprimé `isActive(LocalDateTime referenceTime)` - Pas nécessaire
- ✅ Conservé `isActive()` - Délègue à `period.isActive()`
- ✅ Tests ajoutés dans `ContractTest.java` (section `IsActiveValidation`)

### 3. **Utilisation dans la Logique Métier**
- ✅ **Nouvelle règle métier** : Seuls les contrats actifs peuvent être modifiés
- ✅ Implémentée dans `ContractApplicationService.updateCost()`
- ✅ Nouvelle exception `ExpiredContractException`

---

## 🎯 Règle Métier Implémentée

### **Contexte**
Lorsqu'un client tente de modifier le coût d'un contrat expiré, le système doit l'en empêcher.

### **Code**
```java
@Transactional
@CacheEvict(value = "contractSums", key = "#clientId")
public void updateCost(final UUID clientId, final UUID contractId, BigDecimal newAmount) {
    final Contract contract = contractRepo.findById(contractId)
            .orElseThrow(() -> new ContractNotFoundException(contractId));
    
    if (!contract.getClient().getId().equals(clientId)) {
        throw new ContractNotOwnedByClientException(contractId, clientId);
    }
    
    // ✨ NOUVELLE RÈGLE MÉTIER
    if (!contract.isActive()) {
        throw new ExpiredContractException(contractId);
    }
    
    contract.changeCost(ContractCost.of(newAmount));
    contractRepo.save(contract);
}
```

### **Tests**
```java
@Test
@DisplayName("GIVEN expired contract WHEN updateCost THEN throw ExpiredContractException")
void shouldThrowExceptionWhenContractIsExpired() {
    UUID contractId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    Contract expiredContract = Contract.builder()
            .client(testClient)
            .period(ContractPeriod.of(now.minusDays(100), now.minusDays(1)))  // Expiré hier
            .costAmount(ContractCost.of(new BigDecimal("100.00")))
            .build();

    when(contractRepository.findById(contractId)).thenReturn(Optional.of(expiredContract));

    assertThatThrownBy(() -> service.updateCost(JOHN_DOE_CLIENT_ID, contractId, new BigDecimal("200.00")))
            .isInstanceOf(ExpiredContractException.class)
            .hasMessageContaining(contractId.toString());
}
```

---

## 🌐 Réponse API

### **Cas 1 : Contrat expiré**
**Requête** :
```http
PATCH /v1/clients/{clientId}/contracts/{contractId}/cost
{
    "amount": 2000.00
}
```

**Réponse** : `422 Unprocessable Entity`
```json
{
  "type": "about:blank",
  "title": "Contract Expired",
  "status": 422,
  "detail": "Cannot modify expired contract: {contractId}",
  "code": "contractExpired",
  "timestamp": "2025-11-08T15:30:00",
  "traceId": "abc-123-def"
}
```

### **Cas 2 : Contrat actif (succès)**
**Réponse** : `204 No Content`

---

## 📊 Hiérarchie des Validations

Lors d'un appel à `updateCost()`, les validations suivent cet ordre :

```
1. ✅ Le contrat existe-t-il ?
   ❌ → 404 ContractNotFoundException

2. ✅ Le contrat appartient-il au bon client ?
   ❌ → 403 ContractNotOwnedByClientException

3. ✅ Le contrat est-il actif ? (NOUVEAU)
   ❌ → 422 ExpiredContractException

4. ✅ Mise à jour du coût
   → 204 No Content
```

---

## 🧪 Tests Ajoutés

| Test | Fichier | Description |
|------|---------|-------------|
| `shouldBeActiveWhenEndDateIsNull` | `ContractTest.java` | Contrat sans date de fin est actif |
| `shouldBeActiveWhenEndDateIsInFuture` | `ContractTest.java` | Contrat avec fin future est actif |
| `shouldNotBeActiveWhenEndDateIsInPast` | `ContractTest.java` | Contrat expiré n'est pas actif |
| `shouldThrowExceptionWhenContractIsExpired` | `ContractApplicationServiceTest.java` | Règle métier : pas de modification si expiré |

---

## 📁 Fichiers Modifiés

### Domain
- ✅ `Contract.java` - Méthode `isActive()` simplifiée
- ✅ `ContractPeriod.java` - Méthode `isActiveAt()` supprimée
- ✅ `ExpiredContractException.java` - **NOUVEAU**

### Application
- ✅ `ContractApplicationService.java` - Validation `isActive()` dans `updateCost()`

### Web
- ✅ `ContractController.java` - Documentation API mise à jour
- ✅ `GlobalExceptionHandler.java` - Handler pour `ExpiredContractException`

### Tests
- ✅ `ContractTest.java` - Tests `isActive()` ajoutés
- ✅ `ContractPeriodTest.java` - Tests `isActiveAt()` supprimés
- ✅ `ContractApplicationServiceTest.java` - Test règle métier expiré

---

## 🎯 Avantages

### **Avant** (Logique uniquement dans l'infrastructure)
```sql
-- Logique métier SEULEMENT dans l'infrastructure (JPQL)
WHERE c.endDate IS NULL OR c.endDate > :now
```
❌ Logique métier dans les requêtes uniquement  
❌ Impossible de valider dans le domaine  
❌ Pas de protection métier sur les modifications

### **Après** (Logique dans le domaine + infrastructure)
```java
// Logique métier dans le domaine (validation)
if (!contract.isActive()) {
    throw new ExpiredContractException(contractId);
}
```
```sql
-- Logique AUSSI dans l'infrastructure (performance)
WHERE c.endDate IS NULL OR c.endDate > :now
```
✅ Logique métier dans le domaine pour validation  
✅ Réutilisable partout  
✅ Validation métier claire  
✅ Tests unitaires sur la règle métier  
⚠️ **Duplication assumée pour la performance**

---

## ⚠️ Duplication de la Logique Métier (Assumée)

### **Constat**
La logique "un contrat est actif" est présente à **deux endroits** :

1. **Domaine** : `Contract.isActive()` et `ContractPeriod.isActive()`
   ```java
   public boolean isActive() {
       LocalDateTime now = LocalDateTime.now();
       return endDate == null || now.isBefore(endDate);
   }
   ```

2. **Infrastructure** : Requêtes JPQL (4 occurrences)
   ```sql
   WHERE c.endDate IS NULL OR c.endDate > :now
   ```

### **Pourquoi cette duplication ?**

| Utilisation | Localisation | Raison |
|-------------|--------------|--------|
| **Récupération de listes** | Infrastructure (JPQL) | Performance : filtrage en SQL |
| **Validation métier** | Domaine (`isActive()`) | Sécurité : empêcher modifications incorrectes |

### **Requêtes JPQL concernées**

1. `findActiveContractsPageable()` - Récupère les contrats actifs paginés
2. `findActiveContractsUpdatedAfterPageable()` - Idem avec filtre updatedSince
3. `closeAllActiveContracts()` - Ferme tous les contrats actifs (UPDATE)
4. `sumActiveContracts()` - Calcule la somme des contrats actifs

**Pourquoi garder la logique dans JPQL ?**
- ✅ **Performance** : Filtrage en base de données au lieu de charger tous les contrats en mémoire
- ✅ **Scalabilité** : Si un client a 10,000 contrats dont 5,000 actifs, on ne charge que les 5,000 actifs
- ✅ **Pagination efficace** : Les métadonnées `totalElements`, `totalPages` sont calculées côté DB

### **Pourquoi ajouter la logique dans le domaine ?**
- ✅ **Validation métier** : Empêcher la modification de contrats expirés
- ✅ **Tests unitaires** : Tester la règle métier sans base de données
- ✅ **Réutilisabilité** : Utilisable dans d'autres contextes (UI, autres services)
- ✅ **Clarté** : La règle métier est explicite dans le code domaine

### **Risque de divergence**

⚠️ **Attention** : Si la définition de "contrat actif" change, il faut modifier **deux endroits** :
1. `ContractPeriod.isActive()` (domaine)
2. Les 4 requêtes JPQL (infrastructure)

**Mitigation** :
- ✅ Tests d'intégration qui vérifient la cohérence (`ContractIsActiveConsistencyIT`)
- ✅ Documentation claire de la règle métier
- ✅ Commentaires dans le code JPQL mentionnant la duplication

### **Tests de Cohérence**

**Fichier** : `ContractIsActiveConsistencyIT.java`

Ces tests vérifient que la logique du domaine et celle de l'infrastructure restent synchronisées :

```java
@Test
void shouldHaveConsistentActiveLogicBetweenDomainAndInfrastructure() {
    // Crée 4 contrats : 2 actifs, 2 expirés
    
    // Vérifie que contract.isActive() retourne les bonnes valeurs (domaine)
    assertThat(activeContract.isActive()).isTrue();
    assertThat(expiredContract.isActive()).isFalse();
    
    // Vérifie que findActiveContractsPageable retourne les mêmes (infrastructure)
    Page<Contract> activeFromInfra = contractRepository.findActiveByClientIdPageable(...);
    
    // Assertion : Tous les contrats retournés par l'infrastructure ont isActive() == true
    activeFromInfra.getContent().forEach(contract -> 
        assertThat(contract.isActive()).isTrue()
    );
}
```

**Si ces tests échouent**, cela signifie que la logique a divergé entre le domaine et l'infrastructure.

### **Alternative étudiée et rejetée**

**Filtrage en mémoire** (DDD pur) :
```java
// Charger TOUS les contrats puis filtrer
List<Contract> allContracts = contractRepo.findAllByClientId(clientId);
List<Contract> activeContracts = allContracts.stream()
        .filter(Contract::isActive)
        .collect(Collectors.toList());
```

❌ **Rejetée car** :
- Problème de performance avec beaucoup de contrats
- Pagination inefficace (calculs en mémoire)
- Charge inutile sur la base de données

---

## 🔮 Utilisations Futures Possibles

La méthode `contract.isActive()` peut maintenant être utilisée pour :

1. **Validation avant toute modification**
   ```java
   if (!contract.isActive()) {
       throw new ExpiredContractException(contractId);
   }
   ```

2. **Filtrage en mémoire** (pour petits ensembles)
   ```java
   contracts.stream()
           .filter(Contract::isActive)
           .collect(Collectors.toList());
   ```

3. **Affichage conditionnel dans l'UI**
   ```java
   if (contract.isActive()) {
       // Afficher boutons "Modifier", "Annuler"
   } else {
       // Afficher badge "Expiré"
   }
   ```

4. **Règles métier additionnelles**
   - Empêcher la suppression de contrats actifs
   - Alertes de fin de contrat proche
   - Calculs conditionnels basés sur l'état

---

## ✅ Checklist de Validation

- [x] `isActive()` implémentée dans `Contract`
- [x] `isActive()` implémentée dans `ContractPeriod`
- [x] Méthodes `isActiveAt(referenceTime)` supprimées
- [x] Tests unitaires pour `isActive()`
- [x] Règle métier : pas de modification si expiré
- [x] Exception `ExpiredContractException` créée
- [x] Handler d'exception ajouté (422 Unprocessable Entity)
- [x] Documentation API mise à jour
- [x] Tests d'intégration (règle métier)

---

## 🎉 Conclusion

La logique métier "**un contrat est actif**" est maintenant :
- ✅ **Encapsulée dans le domaine** (`Contract.isActive()`)
- ✅ **Testable unitairement** (tests du domaine)
- ✅ **Utilisée dans la logique métier** (validation updateCost)
- ✅ **Documentée** (Javadoc + OpenAPI)
- ✅ **Protégée** (exception métier dédiée)

La séparation Domain/Infrastructure est respectée avec une logique métier claire et réutilisable ! 🚀

