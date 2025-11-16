# Migration des Tests vers le Pattern Immutable

## Contexte

Suite à la migration du domaine `Client` vers une approche immutable (DDD pur), tous les tests ont été adaptés pour refléter le nouveau comportement.

## Changements dans les tests

### 1. Tests unitaires du domaine - ClientTest.java

#### Avant (pattern mutable)
```java
@DisplayName("updateCommonFields")
class UpdateCommonFieldsValidation {
    @Test
    void shouldUpdateAllCommonFields() {
        Person person = Person.builder()...build();
        
        person.updateCommonFields(newName, newEmail, newPhone);
        
        assertThat(person.getName()).isEqualTo(newName);
    }
}
```

#### Après (pattern immutable)
```java
@DisplayName("withCommonFields - Immutable update pattern")
class WithCommonFieldsValidation {
    @Test
    void shouldCreateNewInstanceWithUpdatedFields() {
        Person original = Person.builder()...build();
        
        Person updated = original.withCommonFields(newName, newEmail, newPhone);
        
        // Vérifier la nouvelle instance
        assertThat(updated.getName()).isEqualTo(newName);
        
        // Vérifier que l'originale n'a PAS changé (immutabilité)
        assertThat(original.getName()).isEqualTo(originalName);
        
        // Même ID (même entité, état différent)
        assertThat(updated.getId()).isEqualTo(original.getId());
    }
}
```

**Changements clés** :
- ✅ Les méthodes retournent maintenant une **nouvelle instance**
- ✅ Tests ajoutés pour vérifier que **l'instance originale ne change pas**
- ✅ Vérification que **l'ID reste le même** (même entité)

### 2. Tests d'application - ClientApplicationServiceTest.java

#### Avant
```java
@Test
void shouldUpdateAllowedFields() {
    Person existingPerson = ...;
    when(clientRepository.findById(id)).thenReturn(Optional.of(existingPerson));
    
    service.updateCommonFields(id, newName, newEmail, newPhone);
    
    // Vérifier la mutation
    assertThat(existingPerson.getName()).isEqualTo(newName);
}
```

#### Après
```java
@Test
void shouldUpdateAllowedFields() {
    Person existingPerson = Person.builder().id(id)...build();
    when(clientRepository.findById(id)).thenReturn(Optional.of(existingPerson));
    when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));
    
    Client result = service.updateCommonFields(id, newName, newEmail, newPhone);
    
    // Vérifier la nouvelle instance retournée
    assertThat(result.getName()).isEqualTo(newName);
    
    // L'instance originale n'est PAS modifiée (en mémoire)
    // Mais une nouvelle instance est sauvegardée
}
```

**Changements clés** :
- ✅ La méthode retourne maintenant `Client` au lieu de `void`
- ✅ Mock de `save()` pour retourner l'argument
- ✅ Vérification du **résultat retourné**
- ✅ Ajout de l'`id` dans le builder pour les tests

### 3. Tests PersonTest.java

#### Avant
```java
@Test
void shouldKeepBirthdateImmutable() {
    Person person = Person.builder()...build();
    
    person.updateCommonFields(newName, newEmail, newPhone);
    
    assertThat(person.getBirthDate()).isEqualTo(originalBirthDate);
}
```

#### Après
```java
@Test
void shouldKeepBirthdateImmutable() {
    Person person = Person.builder()...build();
    
    Person updated = person.withCommonFields(newName, newEmail, newPhone);
    
    // Nouvelle instance a le nouveau nom MAIS le birthDate inchangé
    assertThat(updated.getName().value()).isEqualTo("Jane Doe");
    assertThat(updated.getBirthDate()).isEqualTo(originalBirthDate);
    
    // Instance originale complètement inchangée
    assertThat(person.getName().value()).isEqualTo("John Doe");
}
```

## Méthodes renommées

| Avant (Mutable) | Après (Immutable) | Retour |
|-----------------|-------------------|--------|
| `updateCommonFields()` | `withCommonFields()` | `Person/Company` |
| `changeName()` | `withName()` | `Person/Company` |
| `changeEmail()` | `withEmail()` | `Person/Company` |
| `changePhone()` | `withPhone()` | `Person/Company` |

## Pattern des nouvelles méthodes `withXxx()`

Toutes les méthodes `withXxx()` suivent ce pattern :

```java
public Person withName(final ClientName name) {
    return new Person(
        this.getId(),      // Même ID
        name,              // Nouveau champ
        this.getEmail(),   // Champs inchangés
        this.getPhone(),   // Champs inchangés
        this.birthDate     // Champs inchangés
    );
}
```

**Caractéristiques** :
1. Retourne une **nouvelle instance**
2. **Réutilise l'ID** (même entité logique)
3. **Copie tous les champs non modifiés**
4. **Valide via le constructeur** (validation garantie)

## Avantages du pattern immutable dans les tests

### ✅ Tests plus explicites
```java
Person updated = original.withName(newName);
// Le nom "updated" indique clairement qu'il s'agit d'une nouvelle instance
```

### ✅ Vérification de l'immutabilité
```java
Person updated = original.withCommonFields(...);
assertThat(original.getName()).isEqualTo(originalName); // Prouve l'immutabilité
```

### ✅ Thread-safety implicite
Les tests n'ont pas besoin de gérer la concurrence car les objets sont immutables.

### ✅ Pas d'effets de bord
```java
Person person1 = original.withName(name1);
Person person2 = original.withName(name2);
// person1 et person2 sont indépendants
```

## Tests de régression ajoutés

### Immutabilité de l'instance originale
```java
@Test
void shouldCreateNewInstanceWithUpdatedFields() {
    Person original = ...;
    Person updated = original.withName(newName);
    
    // CRITIQUE : Vérifier que l'original n'a PAS changé
    assertThat(original.getName()).isEqualTo(originalName);
}
```

### Conservation de l'ID
```java
@Test
void shouldPreserveIdAcrossUpdates() {
    Person original = Person.builder().id(uuid)...build();
    Person updated = original.withCommonFields(...);
    
    // Même entité logique
    assertThat(updated.getId()).isEqualTo(original.getId());
}
```

### Conservation des champs non modifiés
```java
@Test
void shouldKeepBirthdateImmutable() {
    Person updated = person.withCommonFields(newName, newEmail, newPhone);
    
    // birthDate DOIT rester inchangé
    assertThat(updated.getBirthDate()).isEqualTo(originalBirthDate);
}
```

## Checklist de migration des tests

- [x] Renommer les nested classes de test (`UpdateCommonFieldsValidation` → `WithCommonFieldsValidation`)
- [x] Capturer la valeur de retour des méthodes `withXxx()`
- [x] Ajouter des assertions pour vérifier l'immutabilité de l'original
- [x] Vérifier la conservation de l'ID
- [x] Ajouter l'`id` dans les builders pour les tests
- [x] Adapter les mocks pour retourner la nouvelle instance sauvegardée
- [x] Mettre à jour les DisplayName pour refléter le pattern immutable

## Conclusion

La migration des tests vers le pattern immutable a permis de :

1. **Renforcer la confiance** : Les tests vérifient explicitement l'immutabilité
2. **Clarifier les intentions** : `withXxx()` est plus clair que `changeXxx()`
3. **Documenter le comportement** : Les tests montrent comment utiliser correctement l'API
4. **Détecter les régressions** : Impossible de réintroduire accidentellement de la mutabilité

Les tests sont maintenant **alignés avec les principes DDD** de l'article de référence.

