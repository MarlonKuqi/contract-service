# ✅ API Finale du Domain Model - Client

## 📋 Méthodes publiques

### Méthodes statiques (Factory Methods - Création)

| Méthode | Usage | Paramètres | Retour |
|---------|-------|------------|--------|
| `of()` | Créer nouvelle instance (sans ID) | name, email, phone, birthDate/companyId | Person/Company |
| `reconstitute()` | Reconstruire depuis DB (avec ID) | **id**, name, email, phone, birthDate/companyId | Person/Company |

### Méthodes d'instance (Modification - Immutabilité)

| Méthode | Usage | Paramètres | Retour | Sémantique |
|---------|-------|------------|--------|-----------|
| `withCommonFields()` | **PUT** - Update complet | name, email, phone (tous requis) | Nouvelle instance | Remplace tous les champs communs |
| `updatePartial()` | **PATCH** - Update partiel | name, email, phone (null = garder actuel) | Nouvelle instance | Merge des changements |

## 🔍 Différence entre withCommonFields() et updatePartial()

### withCommonFields() - PUT complet

```java
// Tous les paramètres sont REQUIS (non null)
Person updated = person.withCommonFields(
    ClientName.of("New Name"),
    Email.of("new@email.com"),
    PhoneNumber.of("+33999999999")
);
// Remplace TOUS les champs communs
```

**Cas d'usage** : 
- Endpoint PUT `/clients/{id}`
- Formulaire complet d'édition
- Remplacement total des données

### updatePartial() - PATCH partiel

```java
// Paramètres null = GARDER la valeur actuelle
Person updated = person.updatePartial(
    ClientName.of("New Name"),  // Change le nom
    null,                        // Garde l'email actuel
    null                         // Garde le téléphone actuel
);
// Merge uniquement les champs fournis
```

**Cas d'usage** :
- Endpoint PATCH `/clients/{id}`
- Formulaire partiel
- Modification d'un seul champ

## 🎯 Nommage DDD

### Pourquoi `updatePartial()` et pas `patch()` ?

En DDD, on privilégie des noms **métier** plutôt que techniques :
- ❌ `patch()` → Trop technique (HTTP)
- ✅ `updatePartial()` → Explicite sur le comportement

Alternatives considérées :
- `mergeChanges()` - verbeux
- `apply()` - pas assez clair
- `modify()` - trop générique
- **`updatePartial()`** - **CHOISI** : clair et explicite

## 📐 Architecture

### Séparation des responsabilités

```
Domain Model (Person/Company)
  ├─ of() → Création
  ├─ reconstitute() → Reconstruction
  ├─ withCommonFields() → Modification complète
  └─ updatePartial() → Modification partielle
       ↑
Application Service (ClientApplicationService)
  ├─ createPerson() → Appelle of()
  ├─ updateCommonFields() → Appelle withCommonFields()
  └─ patchClient() → Appelle updatePartial()
       ↑
Controller (ClientController)
  ├─ POST /clients → createPerson()
  ├─ PUT /clients/{id} → updateCommonFields()
  └─ PATCH /clients/{id} → patchClient()
```

### Flow d'un PATCH

```
HTTP PATCH /clients/123
Body: { "name": "New Name" }
  ↓
ClientController.patchClient()
  ↓
ClientApplicationService.patchClient(id, name, null, null)
  ↓
client.updatePartial(name, null, null)  ← DOMAIN MODEL
  ↓
toBuilder()
  .name(name != null ? name : this.getName())
  .email(null != null ? null : this.getEmail())  → Garde l'email actuel
  .phone(null != null ? null : this.getPhone())  → Garde le téléphone actuel
  .build()
  ↓
Nouvelle instance Person avec nom modifié, email et phone inchangés ✅
```

## ✅ Principes respectés

1. **Immutabilité** : Toutes les méthodes retournent de **nouvelles instances**
2. **DDD** : Logique métier dans le domaine, pas dans l'application
3. **Single Responsibility** : Chaque méthode a un rôle clair
4. **Validation** : `checkInvariants()` appelé lors de `build()`
5. **Explicite** : Noms de méthodes clairs sur leur comportement

## 📝 Récapitulatif

| Opération HTTP | Méthode Domain | Comportement |
|----------------|----------------|--------------|
| POST | `of()` | Créer nouvelle instance |
| GET | `reconstitute()` | Reconstruire depuis DB |
| PUT | `withCommonFields()` | Remplacer tous les champs |
| PATCH | `updatePartial()` | Merger les changements |

Date : 2025-01-17
Statut : ✅ **API FINALE VALIDÉE**

