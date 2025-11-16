# Migration vers l'Immutabilité DDD - Client Domain

## Contexte

Suite à la lecture de l'article [Clean DDD Lessons: Validation and Immutability](https://medium.com/unil-ci-software-engineering/clean-ddd-lessons-validation-and-immutability-a82292ba2a93), nous avons décidé d'adopter l'approche **immutable** pour le domaine `Client`.

## Changements effectués

### 1. Domain Layer - Entités immutables

#### Client (classe abstraite)
- ✅ Tous les champs sont maintenant `final`
- ❌ Suppression des méthodes de mutation (`updateCommonFields()`, `changeName()`, etc.)
- ✅ Validation stricte dans le constructeur

#### Person et Company
- ✅ Ajout de **factory methods statiques** :
  - `create()` : Crée une nouvelle instance avec un UUID généré
  - `reconstitute()` : Reconstruit une instance depuis des paramètres individuels
  - `fromJpaEntity()` : Reconstruit une instance depuis une entité JPA (infrastructure)
  - `withXxx()` : Crée une nouvelle instance avec des champs modifiés

**Exemple Person** :
```java
// Création
Person person = Person.create(name, email, phone, birthDate);

// Modification (retourne une NOUVELLE instance)
Person updatedPerson = person.withName(newName);
Person fullyUpdated = person.withCommonFields(newName, newEmail, newPhone);

// Reconstruction depuis DB via entité JPA
Person reconstituted = Person.fromJpaEntity(personJpaEntity);

// Reconstruction depuis paramètres (rarement utilisé directement)
Person reconstituted = Person.reconstitute(id, name, email, phone, birthDate);
```

### 2. Application Layer - Adaptation au pattern immutable

**ClientApplicationService** :
```java
// AVANT (mutation)
public void updateCommonFields(UUID id, ClientName name, Email email, PhoneNumber phone) {
    Client client = getClientById(id);
    client.updateCommonFields(name, email, phone);
    clientRepo.save(client);
}

// APRÈS (immutabilité)
public Client updateCommonFields(UUID id, ClientName name, Email email, PhoneNumber phone) {
    Client client = getClientById(id);
    
    Client updatedClient = switch (client) {
        case Person p -> p.withCommonFields(name, email, phone);
        case Company c -> c.withCommonFields(name, email, phone);
    };
    
    return clientRepo.save(updatedClient);
}
```

### 3. Infrastructure Layer - Utilisation de fromJpaEntity()

**ClientAssembler** :
```java
// AVANT
return Person.builder()
    .id(entity.getId())
    .name(ClientName.of(entity.getName()))
    .build();

// APRÈS - Version simplifiée avec fromJpaEntity()
return Person.fromJpaEntity(entity);

// Note: fromJpaEntity() appelle en interne reconstitute() 
// avec tous les Value Objects construits
```

**Avantages de fromJpaEntity()** :
- ✅ Code plus concis dans l'assembler
- ✅ Encapsulation du mapping JPA Entity → Domain dans le domaine
- ✅ Facilite les tests (pas besoin de construire tous les VO manuellement)

### 4. Domain Service - Utilisation de create()

**ClientService** :
```java
// AVANT
return Person.builder()
    .name(name)
    .email(email)
    .build();

// APRÈS
return Person.create(name, email, phone, birthDate);
```

## Avantages de cette approche

### ✅ Conformité DDD
1. **Validation garantie** : Impossible de créer un objet invalide
2. **Immutabilité** : Les objets ne peuvent pas être modifiés après création
3. **Thread-safe** : Pas de problèmes de concurrence
4. **Traçabilité** : Chaque modification crée un nouvel objet

### ✅ Compatibilité avec l'architecture existante
- **Domain pur** : Entités immutables
- **Infrastructure pragmatique** : `ClientJpaEntity` reste mutable pour JPA
- **Séparation claire** : Mapping explicite Domain ↔ Infrastructure

### ✅ Clarté du code
- `create()` : Intent clair - création d'une nouvelle entité
- `reconstitute()` : Intent clair - chargement depuis DB
- `withXxx()` : Intent clair - modification (nouvelle instance)

## Compromis et considérations

### ⚠️ Performance
- Chaque modification crée une nouvelle instance
- Dans votre cas : **Impact négligeable** car vous avez déjà un mapping Domain/Infrastructure
- JPA ne voit que la modification de `ClientJpaEntity`, pas les instances de `Client`

### ⚠️ Complexité du code
- Plus de code dans Application Service (pattern matching switch)
- Mais code plus **explicite** et **prévisible**

### ⚠️ Tests à adapter
- Les tests qui utilisent `updateCommonFields()` doivent être mis à jour
- Voir les failures actuels dans `ClientTest`, `PersonTest`, etc.

## Prochaines étapes

### 1. ✅ Adapter les tests unitaires
- ClientTest
- PersonTest
- CompanyTest

### 2. ✅ Adapter les tests d'intégration
- ClientApplicationServiceTest

### 3. ⚠️ Décider pour Contract
Faut-il aussi rendre `Contract` immutable ?
- **Arguments POUR** : Cohérence avec `Client`
- **Arguments CONTRE** : `Contract` a plus de mutations (`updateCost()`, `close()`, etc.)

### 4. 📝 Documentation
- Mettre à jour le README
- Documenter le pattern dans `docs/`

## Références

- Article de référence : https://medium.com/unil-ci-software-engineering/clean-ddd-lessons-validation-and-immutability-a82292ba2a93
- Discussion complète : `docs-claude/DDD_IMMUTABILITY_DECISION.md`

## Conclusion

Cette migration respecte **l'esprit du DDD tout en restant pragmatique** avec les contraintes techniques (JPA/Hibernate). 

Le domaine est maintenant **pur et immutable**, tandis que l'infrastructure reste **mutable et compatible JPA**.


