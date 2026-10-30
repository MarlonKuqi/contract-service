# 🔄 Guide de Migration des URLs API

## Vue d'ensemble

Les URLs de l'API ont été refactorisées pour suivre une structure **plus plate** et éviter les URLs profondément imbriquées, conformément aux bonnes pratiques REST modernes et aux recommandations des experts.

---

## 📋 Changements d'URLs

### ✅ Endpoints Clients

#### Ancienne Structure (OBSOLETE)
```
POST   /v1/persons                    → Créer une personne
POST   /v1/companies                  → Créer une entreprise
GET    /v1/clients/{id}               → Lire un client
PUT    /v1/clients/{id}               → Modifier un client
DELETE /v1/clients/{id}               → Supprimer un client
```

#### Nouvelle Structure (ACTUELLE)
```
POST   /v1/clients                    → Créer un client (Person OU Company)
GET    /v1/clients/{id}               → Lire un client
PUT    /v1/clients/{id}               → Modifier un client
DELETE /v1/clients/{id}               → Supprimer un client
```

**💡 Changement clé** : Utilisation d'un champ discriminant `"type": "PERSON"` ou `"type": "COMPANY"` dans le JSON au lieu de routes séparées.

---

### ✅ Endpoints Contrats

#### Ancienne Structure (OBSOLETE)
```
POST   /v1/clients/{clientId}/contracts
GET    /v1/clients/{clientId}/contracts
GET    /v1/clients/{clientId}/contracts?updatedSince=...
PATCH  /v1/clients/{clientId}/contracts/{contractId}/cost
GET    /v1/clients/{clientId}/contracts/sum
```

#### Nouvelle Structure (ACTUELLE)
```
POST   /v1/contracts?clientId={clientId}
GET    /v1/contracts?clientId={clientId}
GET    /v1/contracts?clientId={clientId}&updatedSince=...
GET    /v1/contracts/{contractId}?clientId={clientId}
PATCH  /v1/contracts/{contractId}/cost?clientId={clientId}
GET    /v1/contracts/sum?clientId={clientId}
```

**💡 Changement clé** : Le `clientId` est passé en **query parameter** au lieu d'être dans le path, ce qui évite les URLs à 4 niveaux.

---

## 🔍 Comparaison Avant/Après

### Exemple 1 : Créer un contrat

**Avant**
```http
POST /v1/clients/8473a879-c66b-4868-b186-5dfcf6804c3a/contracts
Content-Type: application/json

{
  "costAmount": 1500.00
}
```

**Après**
```http
POST /v1/contracts?clientId=8473a879-c66b-4868-b186-5dfcf6804c3a
Content-Type: application/json

{
  "costAmount": 1500.00
}
```

---

### Exemple 2 : Modifier le coût

**Avant**
```http
PATCH /v1/clients/8473a879-c66b-4868-b186-5dfcf6804c3a/contracts/12345/cost
Content-Type: application/json

{
  "amount": 2000.00
}
```

**Après**
```http
PATCH /v1/contracts/12345/cost?clientId=8473a879-c66b-4868-b186-5dfcf6804c3a
Content-Type: application/json

{
  "amount": 2000.00
}
```

---

### Exemple 3 : Somme des contrats

**Avant**
```http
GET /v1/clients/8473a879-c66b-4868-b186-5dfcf6804c3a/contracts/sum
```

**Après**
```http
GET /v1/contracts/sum?clientId=8473a879-c66b-4868-b186-5dfcf6804c3a
```

---

## 🎯 Justification

### Avantages de la nouvelle structure

1. **✅ URLs plus plates** : Maximum 2-3 niveaux au lieu de 4
2. **✅ Meilleure scalabilité** : Facilite l'ajout de nouveaux endpoints
3. **✅ Standards REST modernes** : Évite les deeply nested URLs
4. **✅ Flexibilité** : Le clientId en query param peut être rendu optionnel à l'avenir (ex: admin API)
5. **✅ Cohérence** : Pattern uniforme pour tous les endpoints de contrats

### Avantages de la validation côté serveur

- **✅ Sécurité renforcée** : Le serveur vérifie que le contrat appartient bien au client (403 si non-concordance)
- **✅ Même niveau de sécurité** : Identique à l'ancienne approche avec path variable
- **✅ Messages d'erreur explicites** : 403 Forbidden avec détails si tentative d'accès à un contrat non autorisé

---

## 📦 Migration des Collections Postman

### Collections obsolètes
- ❌ `PersonController.postman_collection.json` → Marquée OBSOLETE
- ❌ `CompanyController.postman_collection.json` → Marquée OBSOLETE

### Collections actuelles
- ✅ `ClientController.postman_collection.json` → Unifie Person et Company
- ✅ `ContractController.postman_collection.json` → URLs mises à jour

---

## 🔒 Sécurité

La sécurité est **identique** entre les deux approches :

### Ancienne approche
```
PATCH /v1/clients/{clientId}/contracts/{contractId}/cost
→ Serveur vérifie : contract.clientId == clientId (du path)
```

### Nouvelle approche
```
PATCH /v1/contracts/{contractId}/cost?clientId={clientId}
→ Serveur vérifie : contract.clientId == clientId (du query param)
```

**Résultat** : Dans les deux cas, retour d'une erreur 403 (Forbidden) si le contrat n'appartient pas au client.

---

## 📝 Checklist de Migration

Pour migrer vos clients API :

- [ ] Mettre à jour les URLs dans vos appels HTTP
- [ ] Remplacer les path variables `{clientId}` par des query params `?clientId=...`
- [ ] Utiliser `POST /v1/clients` avec `"type": "PERSON"` ou `"COMPANY"` au lieu de `/v1/persons` ou `/v1/companies`
- [ ] Mettre à jour vos collections Postman/Insomnia
- [ ] Tester les endpoints de contrat avec les nouvelles URLs
- [ ] Vérifier que les erreurs 403 fonctionnent correctement (tentative d'accès à un contrat d'un autre client)

---

## 🆘 Support

Si vous rencontrez des problèmes lors de la migration :

1. Consultez les collections Postman mises à jour dans `/api-collections`
2. Vérifiez le README : `/api-collections/README.md`
3. Consultez la documentation Swagger/OpenAPI : `http://localhost:8080/swagger-ui.html`
4. Vérifiez les codes HTTP retournés (400, 403, 404, 422)

---

**Date de migration** : 2025-11-09  
**Version** : 1.0.0

