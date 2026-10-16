# 📦 API Collections (Standard Postman Format)

Collections simples au **format Postman v2.1** (standard universel), une par controller.

## ✅ Compatibilité Universelle

Ces fichiers JSON peuvent être importés dans **n'importe quel outil** :
- ✅ **Postman** (Desktop, Web, CLI/Newman)
- ✅ **Insomnia** (peut lire le format Postman)
- ✅ **Hoppscotch** (web-based, open-source)
- ✅ **Thunder Client** (extension VS Code)
- ✅ **REST Client** (VS Code, avec conversion)
- ✅ **Bruno** (peut importer Postman)
- ✅ Tout autre outil supportant Postman Collection v2.1

---

## 📂 Collections par Controller

### 1️⃣ ClientController (Unified API - ✅ Recommended)
**Fichier** : `ClientController.postman_collection.json`  
**Endpoints** :
- `POST /v2/clients` - Create client (Person or Company) with type discriminator
- `GET /v2/clients/{id}` - Read client
- `PUT /v2/clients/{id}` - Update client
- `PATCH /v2/clients/{id}` - Partial update (name, email, phone)
- `DELETE /v2/clients/{id}` - Delete client (soft delete, closes its active contracts)

**Requêtes** : CRUD complet sur les clients

### 2️⃣ ContractController
**Fichier** : `ContractController.postman_collection.json`  
**Endpoints** :
- `POST /v2/contracts` - Create contract (`clientId` in the body)
- `GET /v2/contracts?clientId={clientId}` - List active contracts (paginated)
- `GET /v2/contracts?clientId={clientId}&updatedSince=...` - Filter by update date
- `GET /v2/contracts/{contractId}` - Get specific contract
- `PATCH /v2/contracts/{contractId}/cost` - Update cost
- `GET /v2/contracts/total?clientId={clientId}` - Sum of active contracts

**Requêtes** : création, lecture, mise à jour du coût et agrégation. Il n'existe pas d'endpoint de suppression ou de clôture de contrat : un contrat se termine par sa `endDate`, ou à la suppression de son client.

---

## 🚀 Import Rapide

### Postman
```
1. Ouvrir Postman
2. Cliquer sur "Import"
3. Drag & Drop le(s) fichier(s) JSON
4. ✅ Collection(s) importée(s)
```

### Insomnia
```
1. Ouvrir Insomnia
2. Application > Import/Export > Import Data
3. From File > Sélectionner le fichier JSON
4. ✅ Collection importée
```

### Hoppscotch
```
1. Aller sur https://hoppscotch.io
2. Collections > Import
3. Drag & Drop le fichier JSON
4. ✅ Collection importée
```

### Thunder Client (VS Code)
```
1. Ouvrir Thunder Client
2. Collections tab > Menu (⋮) > Import
3. Sélectionner le fichier JSON
4. ✅ Collection importée
```

---

## 🔧 Variables de Collection

Chaque collection définit ses propres variables :

### ClientController
| Variable | Valeur par défaut |
|----------|-------------------|
| `baseUrl` | `http://localhost:8080` |
| `clientId` | `REPLACE_WITH_CLIENT_UUID` |

### ContractController
| Variable | Valeur par défaut |
|----------|-------------------|
| `baseUrl` | `http://localhost:8080` |
| `clientId` | `REPLACE_WITH_CLIENT_UUID` |
| `contractId` | `REPLACE_WITH_CONTRACT_UUID` |

**Comment modifier les variables** :
- **Postman** : Collection > Variables tab
- **Insomnia** : Environments > Edit
- **Hoppscotch** : Environments > Edit variables

---

## 🎯 Workflow d'Utilisation

### Créer un Client (Person)

1. Importer `ClientController.postman_collection.json`
2. Exécuter "Create Person" avec payload :
   ```json
   {
     "type": "PERSON",
     "name": "Jane Doe",
     "email": "jane@example.com",
     "phone": "+41791234567",
     "birthDate": "1985-03-20"
   }
   ```
3. Copier l'`id` retourné dans la réponse

### Créer un Client (Company)

1. Importer `ClientController.postman_collection.json`
2. Exécuter "Create Company" avec payload :
   ```json
   {
     "type": "COMPANY",
     "name": "Tech Solutions SA",
     "email": "info@techsolutions.ch",
     "phone": "+41791234567",
     "companyIdentifier": "CHE-987.654.321"
   }
   ```
3. Copier l'`id` retourné

### Créer un Contrat

1. Importer `ContractController.postman_collection.json`
2. Modifier la variable `clientId` avec l'UUID copié
3. Exécuter "Create Contract - Default Dates"
4. Le contrat est créé et l'`id` est retourné

### Mettre à Jour le Coût

1. Modifier la variable `contractId` avec l'UUID du contrat
2. Exécuter "Update Contract Cost"

### Calculer la Somme

1. Exécuter "Get Sum of Active Contracts"

---

## 📊 Comparaison avec Format Bruno

| Critère | Collections Postman | Bruno `.bru` |
|---------|---------------------|--------------|
| **Format** | JSON standard | Format propriétaire |
| **Compatibilité** | ✅ Universel (tous outils) | ❌ Bruno uniquement |
| **Lisibilité** | JSON structuré | DSL lisible |
| **Versionning Git** | ✅ Oui | ❌ Non (`.gitignore`, copie locale) |
| **Import/Export** | ✅ Tous outils | ❌ Bruno uniquement |

**Recommandation** : Utiliser les collections Postman pour **maximum de compatibilité**.

---

## 📝 Notes Importantes

### Format des Dates
Toutes les dates doivent respecter **ISO 8601**, sans fuseau (heure suisse, `Europe/Zurich`) :
- `"2025-01-15"` (date simple)
- `"2025-01-01T00:00:00"` (date et heure)

### Variables Non Auto-Remplies
Les variables `clientId` et `contractId` doivent être **manuellement copiées** depuis les réponses.

### Ordre des Requêtes
1. Créer client (Person/Company)
2. Lire/Modifier/Supprimer client
3. Créer contrat
4. Opérations sur contrats

---

## 🗂️ Structure des Fichiers

```
api-collections/
├── ClientController.postman_collection.json        (9 requêtes)
├── ContractController.postman_collection.json      (8 requêtes)
└── README.md
```

**Total : 17 requêtes réparties dans 2 fichiers**

---

## 🆘 Troubleshooting

### Les variables ne fonctionnent pas
- Vérifier que la variable est bien définie dans la collection
- Dans Postman : Collection > Variables > Current Value
- Dans Insomnia : Créer un environnement et y définir les variables

### Erreur "Connection Refused"
- ✅ Vérifier que Spring Boot tourne sur `http://localhost:8080`
- ✅ Vérifier que PostgreSQL est démarré
- ✅ Tester avec `curl http://localhost:8080/actuator/health`

### Format JSON invalide
- Les fichiers sont au format Postman v2.1 strict
- Compatible avec la majorité des outils depuis 2017

---

**Bon testing ! 🚀**
