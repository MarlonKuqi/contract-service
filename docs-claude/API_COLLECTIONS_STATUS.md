# 📦 État des Collections API Postman

**Dernière mise à jour** : 2025-11-09

---

## ✅ Collections Actuelles et Maintenues

### 1. ClientController.postman_collection.json

**Status** : ✅ **ACTUEL** - À jour  
**Endpoints** : 5  
**Description** : Gestion unifiée des clients (Person et Company)

#### Requêtes disponibles

| # | Nom | Méthode | URL | Description |
|---|-----|---------|-----|-------------|
| 1 | Create Person | POST | `/v1/clients` | Créer un client personne avec `"type": "PERSON"` |
| 2 | Create Company | POST | `/v1/clients` | Créer un client entreprise avec `"type": "COMPANY"` |
| 3 | Read Client | GET | `/v1/clients/{id}` | Récupérer un client par son ID |
| 4 | Update Client | PUT | `/v1/clients/{id}` | Modifier les champs communs (name, email, phone) |
| 5 | Delete Client | DELETE | `/v1/clients/{id}` | Supprimer un client (clôture automatique des contrats) |

#### Variables
```json
{
  "baseUrl": "http://localhost:8080",
  "clientId": "REPLACE_WITH_CLIENT_UUID"
}
```

---

### 2. ContractController.postman_collection.json

**Status** : ✅ **ACTUEL** - À jour  
**Endpoints** : 8  
**Description** : Gestion complète des contrats (CRUD, filtres, agrégations)

#### Requêtes disponibles

| # | Nom | Méthode | URL | Description |
|---|-----|---------|-----|-------------|
| 1 | Create Contract - Default Dates | POST | `/v1/contracts?clientId=...` | Créer un contrat (dates par défaut) |
| 2 | Create Contract - With Dates | POST | `/v1/contracts?clientId=...` | Créer un contrat avec dates spécifiques |
| 3 | Create Contract - Open Ended | POST | `/v1/contracts?clientId=...` | Créer un contrat à durée indéterminée |
| 4 | Get Active Contracts | GET | `/v1/contracts?clientId=...&page=0&size=20` | Liste paginée des contrats actifs |
| 5 | Get Contracts - Filter By Update Date | GET | `/v1/contracts?clientId=...&updatedSince=...` | Filtrer par date de modification |
| 6 | Get Contract By ID | GET | `/v1/contracts/{contractId}?clientId=...` | Récupérer un contrat spécifique |
| 7 | Update Contract Cost | PATCH | `/v1/contracts/{contractId}/cost?clientId=...` | Modifier le coût d'un contrat |
| 8 | Get Sum of Active Contracts | GET | `/v1/contracts/sum?clientId=...` | Somme des coûts (agrégation DB) |

#### Variables
```json
{
  "baseUrl": "http://localhost:8080",
  "clientId": "REPLACE_WITH_CLIENT_UUID",
  "contractId": "REPLACE_WITH_CONTRACT_UUID"
}
```

---

## ⚠️ Collections Obsolètes (Conservées pour Référence)

### 3. PersonController.postman_collection.json

**Status** : ❌ **OBSOLÈTE**  
**Raison** : Fusionné dans `ClientController` avec discriminant `"type": "PERSON"`  
**Action recommandée** : Utiliser `ClientController` → `Create Person`

---

### 4. CompanyController.postman_collection.json

**Status** : ❌ **OBSOLÈTE**  
**Raison** : Fusionné dans `ClientController` avec discriminant `"type": "COMPANY"`  
**Action recommandée** : Utiliser `ClientController` → `Create Company`

---

## 🔄 Workflow Standard

### Scénario : Création complète d'un client avec contrats

```
1. ClientController → Create Person/Company
   ↓ (copier clientId de la réponse)
   
2. ContractController → Create Contract - With Dates
   ↓ (utiliser clientId copié, copier contractId de la réponse)
   
3. ContractController → Get Active Contracts
   ↓ (visualiser tous les contrats du client)
   
4. ContractController → Update Contract Cost
   ↓ (modifier le coût d'un contrat spécifique)
   
5. ContractController → Get Sum of Active Contracts
   ↓ (obtenir le coût total)
```

---

## 📊 Statistiques

| Métrique | Valeur |
|----------|--------|
| Collections actives | 2 |
| Collections obsolètes | 2 |
| Total endpoints documentés | 13 |
| Total requêtes d'exemple | 13 |

---

## 🎯 Import dans Postman

### Méthode rapide
```bash
1. Ouvrir Postman
2. Cliquer sur "Import"
3. Glisser-déposer ces fichiers :
   - ClientController.postman_collection.json
   - ContractController.postman_collection.json
4. ✅ Prêt à utiliser !
```

### Configuration des variables
```
1. Sélectionner une collection
2. Onglet "Variables"
3. Remplacer REPLACE_WITH_CLIENT_UUID par un UUID réel
4. Remplacer REPLACE_WITH_CONTRACT_UUID par un UUID réel
```

---

## 🔐 Authentification

**Status actuel** : Aucune authentification requise (développement)

**Production** : Ajouter un header `Authorization: Bearer <token>` à toutes les requêtes.

**Comment ajouter dans Postman** :
```
1. Collection → Authorization
2. Type: Bearer Token
3. Token: {{authToken}}
4. Ajouter variable authToken dans l'environnement
```

---

## 📝 Notes de Version

### v1.0.0 (2025-11-09)
- ✅ Migration vers URLs plates (`?clientId=` au lieu de `/clients/{clientId}/...`)
- ✅ Unification Person/Company dans ClientController
- ✅ Ajout endpoint `GET /v1/contracts/{contractId}`
- ✅ Marquage collections obsolètes
- ✅ Documentation complète des 13 endpoints

---

## 🆘 Aide

**Problème** : "ClientId not found"  
**Solution** : Créer d'abord un client avec `Create Person` ou `Create Company`

**Problème** : "Contract does not belong to client" (403)  
**Solution** : Vérifier que le `clientId` passé correspond au propriétaire du contrat

**Problème** : "Contract is expired" (422)  
**Solution** : Impossible de modifier un contrat dont `endDate < maintenant`

**Problème** : "Invalid email format" (422)  
**Solution** : Utiliser un format d'email valide (ex: `test@example.com`)

---

**Pour toute question** : Consulter `/api-collections/README.md` ou `URL_MIGRATION_GUIDE.md`

