# Import asynchrone de contrats (CSV) — notes de version (non publiées)

Décision : ADR-022. Traitement par Spring Batch. Migrations `V2.0.0.6` (trois tables du domaine), `V2.0.0.7` (tables de Spring Batch) et `V2.0.0.8` (raison de l'échec) et `V2.0.0.9` (auteur, idempotence, annulation), aucun changement cassant. L'import synchrone `POST /v2/contracts/imports` (ADR-021) ne change pas.

## Nouveaux endpoints

Rôle `COUNSELOR`.

| Endpoint | Rôle | Réponse |
|---|---|---|
| `POST /v2/contracts/import-jobs` (`multipart/form-data`, partie `file`) | Soumet un fichier | `202`, en-tête `Location`, état du job |
| `GET /v2/contracts/import-jobs/{jobId}` | Suit un job | Statut et compteurs |
| `GET /v2/contracts/import-jobs/{jobId}/rejections?page&size` | Liste les rejets, par ligne | Page de 100 lignes au plus |
| `GET /v2/contracts/import-jobs?submittedBy&page&size` | Liste les jobs, du plus récent au plus ancien | Page de 100 jobs au plus, avec leur auteur |
| `POST /v2/contracts/import-jobs/{jobId}/cancellation` | Annule le job et ferme ses contrats | `200`, `{ job, closedContracts }` |
| `GET /v2/contracts/import-jobs/{jobId}/rejections/export` | Télécharge les rejets en CSV | `text/csv`, en pièce jointe |

## Format du fichier

UTF-8, séparateur `;`, 50 Mo et 500 000 contrats au plus. Dates ISO, décimales avec un point, `startDate` et `endDate` vides autorisés, pas de guillemets. `type` est obligatoire : `LAMAL`, `LAA` ou `LPP`, en majuscules (voir [CONTRACT_TYPES.md](CONTRACT_TYPES.md)).

```csv
clientId;type;startDate;endDate;costAmount
550e8400-e29b-41d4-a716-446655440000;LAMAL;;;1500.00
550e8400-e29b-41d4-a716-446655440000;LAMAL;2027-01-01T00:00:00;2028-01-01T00:00:00;900.00
```

## Réponses à la soumission

| Réponse | Signification | Base de données |
|---|---|---|
| `202` | Le fichier est enregistré et sera traité | Job `PENDING` et fichier |
| `202` (même `Idempotency-Key`, même fichier) | Le job déjà créé est renvoyé | Rien de plus |
| `400 invalidIdempotencyKey` | `Idempotency-Key` vide ou de plus de 255 caractères | Rien |
| `409 dataIntegrityViolation` | Même `Idempotency-Key` envoyée deux fois au même instant | Rien de plus : renvoyer la requête |
| `400 missingParameter` | Pas de partie `file` | Rien |
| `413 importFileTooLarge` | Plus de 50 Mo | Rien |
| `422 invalidImportFile` | Aucune ligne, en-tête incorrect, encodage invalide ou plus de 500 000 lignes | Rien |
| `422 idempotencyKeyReused` | `Idempotency-Key` déjà utilisée par ce conseiller pour un autre fichier | Rien |

## Import partiel

Les lignes valides créent leur contrat, les autres sont rapportées. Un job `COMPLETED` peut donc contenir des rejets.

| Statut | Signification |
|---|---|
| `PENDING` | En attente d'un worker |
| `RUNNING` | En cours de traitement |
| `COMPLETED` | Toutes les lignes sont traitées |
| `FAILED` | Le job s'est arrêté, voir `failureCode` : les lots déjà traités restent importés |
| `CANCELLED` | Le job a été annulé, voir `cancelledAt` et `cancelledBy` : ses contrats encore actifs ont été fermés |

| `failureCode` | Signification |
|---|---|
| `unexpectedError` | Erreur inattendue pendant le traitement |
| `incompleteProcessing` | Le traitement s'est terminé sans lire toutes les lignes |
| `tooManyAttempts` | Cinq tentatives n'ont pas suffi (pannes ou erreurs transitoires répétées) |

Exemple de rejet (`lineNumber` est le numéro de ligne dans le fichier, l'en-tête étant la ligne 1) :

```json
{
  "content": [
    { "lineNumber": 3, "clientId": "550e8400-e29b-41d4-a716-446655440000", "code": "invalidContractCost", "message": "..." },
    { "lineNumber": 7, "clientId": null, "code": "invalidLineFormat", "message": "Expected 5 columns (clientId;type;startDate;endDate;costAmount), but found 1" }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

Codes d'une ligne rejetée : `invalidLineFormat` (y compris un type inconnu), `invalidContractCost`, `invalidContractPeriod`, `clientNotFound` (client inconnu ou supprimé), `contractTypeNotAllowed` (type réservé à une autre nature de client).

## À savoir

- **Ne renvoyer que les lignes rejetées**, dans un nouveau fichier : l'export CSV donne chaque ligne d'origine avec la raison de son rejet. Renvoyer le fichier complet créerait une seconde fois les contrats déjà importés.
- **Envoyer une `Idempotency-Key`** pour pouvoir soumettre de nouveau sans risque après un timeout : la même clé avec le même fichier renvoie le premier job. Utiliser une nouvelle clé pour un nouveau fichier.
- **Après un `FAILED`, ou pour revenir sur un mauvais fichier, annuler le job** : ses contrats encore actifs sont fermés à l'instant de l'annulation, comme ceux d'un client supprimé, et un fichier corrigé peut être soumis sans doublon actif. Un job en cours s'arrête au lot en cours, un job en attente ne démarre jamais. Annuler de nouveau ne change rien.
- Un contrat fermé par l'annulation n'est pas supprimé : il reste lisible par `GET /v2/contracts/{contractId}`.
- Si l'application s'arrête proprement, le job revient en `PENDING` à la fin du lot en cours et reprend sans doublon. Après un crash, il reprend à l'expiration du bail (5 minutes).
- Une erreur transitoire de la base (deadlock par exemple) remet le job en attente, sans passer par `FAILED`.
- Une ligne commençant par `#` n'est pas un commentaire : elle est rejetée avec `invalidLineFormat`, comme une ligne contenant un caractère NUL.
- Une date hors des années 1 à 9999 est rejetée avec `invalidContractPeriod`. La règle vaut aussi pour `POST /v2/contracts` et l'import synchrone.
- Un client supprimé pendant le traitement ne garde aucun contrat actif : ses lignes restantes sont rejetées avec `clientNotFound`, et ses contrats déjà importés sont fermés.

## Changements de configuration

| Propriété | Valeur | Rôle |
|---|---|---|
| `spring.servlet.multipart.max-file-size` | `50MB` | Taille maximale du fichier |
| `spring.servlet.multipart.max-request-size` | `51MB` | Taille maximale de la requête |
| `spring.servlet.multipart.resolve-lazily` | `true` | Fait passer le dépassement de taille par `ContractControllerAdvice` |
| `spring.jpa.properties.hibernate.jdbc.batch_size` | `1000` (au lieu de `50`) | Taille des paquets d'écriture, pour toute l'application |
| `spring.datasource.hikari.data-source-properties.reWriteBatchedInserts` | `true` | Fusionne les `INSERT` d'un paquet en requêtes multi-valeurs |
| `spring.batch.job.enabled` | `false` | Empêche Spring Boot de lancer le job au démarrage |
| `spring.batch.jdbc.initialize-schema` | `never` | Les tables sont créées par Flyway |
| `spring.batch.jdbc.table-prefix` | `contracts.BATCH_` | Place les tables de Spring Batch dans le schéma `contracts` |
| `app.contract-import.worker-enabled` | `true` | Active le worker (`false` dans le profil `test`) |
| `app.contract-import.poll-delay` | `PT2S` | Attente entre deux recherches de job |

## Changement de comportement mineur

`DeleteClient` écrit désormais `deleted_at` avant de fermer les contrats (`flush`). Rien ne change pour l'appelant ; cet ordre est nécessaire pour que le verrou posé par un lot protège la suppression (ADR-022).
