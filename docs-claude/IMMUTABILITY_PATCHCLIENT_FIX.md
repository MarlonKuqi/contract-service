# 🔧 Corrections des Tests patchClient - Pattern Immutable

## Problème identifié

Après la migration vers l'immutabilité, 3 tests de `ClientApplicationServiceTest` échouaient :

### 1. `shouldNotSaveWhenNoChanges`
```
Never wanted here: clientRepository.save(<any>);
But invoked here: -> at ClientApplicationService.patchClient
```

**Cause** : La méthode `patchClient()` appelait toujours `save()`, même si aucun champ n'était fourni.

### 2. `shouldUpdateAllProvidedFields`
```
expected: Jane Smith
but was: John Doe
```

**Cause** : Le test vérifiait `existingPerson.getName()` au lieu du résultat retourné par `patchClient()`.

### 3. `shouldUpdateOnlyProvidedFields`
```
expected: Jane Doe
but was: John Doe
```

**Cause** : Même problème - vérification de l'instance originale au lieu de la nouvelle.

## Solutions appliquées

### Solution 1 : ClientApplicationService.patchClient()

**Avant** :
```java
@Transactional
public Client patchClient(final UUID id, ...) {
    Client client = getClientById(id);
    
    if (name != null) {
        client = switch (client) {
            case Person p -> p.withName(name);
            case Company c -> c.withName(name);
        };
    }
    // ... autres champs
    
    return clientRepo.save(client);  // ❌ Sauvegarde TOUJOURS
}
```

**Après** :
```java
@Transactional
public Client patchClient(final UUID id, ...) {
    Client client = getClientById(id);
    boolean hasChanges = false;
    
    if (name != null) {
        client = switch (client) {
            case Person p -> p.withName(name);
            case Company c -> c.withName(name);
        };
        hasChanges = true;  // ✅ Tracking des changements
    }
    if (email != null) {
        client = switch (client) {
            case Person p -> p.withEmail(email);
            case Company c -> c.withEmail(email);
        };
        hasChanges = true;
    }
    if (phone != null) {
        client = switch (client) {
            case Person p -> p.withPhone(phone);
            case Company c -> c.withPhone(phone);
        };
        hasChanges = true;
    }
    
    if (hasChanges) {
        return clientRepo.save(client);  // ✅ Sauvegarde UNIQUEMENT si changement
    }
    
    return client;  // ✅ Retourne l'original si aucun changement
}
```

### Solution 2 : Tests adaptés au pattern immutable

#### Test 1 : shouldUpdateOnlyProvidedFields

**Avant** :
```java
@Test
void shouldUpdateOnlyProvidedFields() {
    Person existingPerson = Person.builder()
            .name(ClientName.of("John Doe"))
            .email(Email.of("john@example.com"))
            .phone(PhoneNumber.of("+33111111111"))
            .birthDate(PersonBirthDate.of(LocalDate.of(1990, 5, 15)))
            .build();

    when(clientRepository.findById(clientId)).thenReturn(Optional.of(existingPerson));

    ClientName newName = ClientName.of("Jane Doe");
    service.patchClient(clientId, newName, null, null);

    assertThat(existingPerson.getName()).isEqualTo(newName);  // ❌ FAUX
    verify(clientRepository).save(existingPerson);  // ❌ FAUX
}
```

**Après** :
```java
@Test
void shouldUpdateOnlyProvidedFields() {
    Person existingPerson = Person.builder()
            .id(clientId)  // ✅ Ajout de l'ID
            .name(ClientName.of("John Doe"))
            .email(Email.of("john@example.com"))
            .phone(PhoneNumber.of("+33111111111"))
            .birthDate(PersonBirthDate.of(LocalDate.of(1990, 5, 15)))
            .build();

    when(clientRepository.findById(clientId)).thenReturn(Optional.of(existingPerson));
    when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));  // ✅ Mock save

    ClientName newName = ClientName.of("Jane Doe");
    Client result = service.patchClient(clientId, newName, null, null);  // ✅ Capture du résultat

    // Vérifier la nouvelle instance retournée
    assertThat(result.getName()).isEqualTo(newName);  // ✅ Vérifie la nouvelle instance
    assertThat(result.getEmail().value()).isEqualTo("john@example.com");
    assertThat(result.getPhone().value()).isEqualTo("+33111111111");
    
    // Vérifier que l'originale n'a pas changé (immutabilité)
    assertThat(existingPerson.getName().value()).isEqualTo("John Doe");  // ✅ Vérifie l'immutabilité
    
    verify(clientRepository).save(any(Client.class));  // ✅ Vérifie qu'une instance a été sauvegardée
}
```

#### Test 2 : shouldUpdateAllProvidedFields

**Changements identiques** :
- Ajout de l'ID au builder
- Mock de `save()`
- Capture du résultat retourné
- Vérification de la nouvelle instance
- Vérification de l'immutabilité de l'originale

#### Test 3 : shouldNotSaveWhenNoChanges

Ce test **ne nécessite aucun changement** car il vérifiait déjà correctement que `save()` n'est pas appelé.

Le changement dans `patchClient()` (ajout du flag `hasChanges`) suffit pour le faire passer.

## Changements clés à retenir

### 1. Pattern de vérification des tests avec immutabilité

```java
// ✅ Pattern correct
Client result = service.patchClient(...);
assertThat(result.getXxx()).isEqualTo(newValue);  // Vérifier la nouvelle instance
assertThat(original.getXxx()).isEqualTo(oldValue);  // Vérifier l'immutabilité
```

### 2. Optimisation des sauvegardes

```java
// ✅ Pattern correct
boolean hasChanges = false;
if (fieldToUpdate != null) {
    entity = entity.withField(fieldToUpdate);
    hasChanges = true;
}
if (hasChanges) {
    return repo.save(entity);
}
return entity;  // Pas de save inutile
```

### 3. Mock de save() dans les tests

```java
// ✅ Nécessaire pour récupérer l'instance sauvegardée
when(repo.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));
```

### 4. ID dans les builders de test

```java
// ✅ Nécessaire pour vérifier que la nouvelle instance a le même ID
Person person = Person.builder()
    .id(clientId)  // Important !
    .name(...)
    .build();
```

## Résultat final

✅ **Tous les tests passent**
✅ **Comportement immutable garanti**
✅ **Performance optimisée** (pas de save inutile)
✅ **Tests documentent le comportement attendu**

## Leçons apprises

1. **L'immutabilité change la sémantique** : Les méthodes retournent maintenant de nouvelles instances
2. **Les tests doivent refléter ce changement** : Vérifier le résultat, pas l'original
3. **L'immutabilité doit être testée** : Ajouter des assertions pour vérifier que l'original ne change pas
4. **Optimisation importante** : Ne pas sauvegarder si aucun changement (pattern `hasChanges`)

## Pattern général pour adapter les tests

Pour chaque test de mutation :
1. ✅ Capturer la valeur de retour : `Client result = service.updateXxx(...)`
2. ✅ Ajouter un mock pour `save()` : `when(repo.save(...)).thenAnswer(...)`
3. ✅ Vérifier la nouvelle instance : `assertThat(result.getXxx())`
4. ✅ Vérifier l'immutabilité : `assertThat(original.getXxx()).isEqualTo(oldValue)`
5. ✅ Vérifier que save a été appelé : `verify(repo).save(any(...))`

**Date** : 2025-01-16
**Statut** : ✅ RÉSOLU

