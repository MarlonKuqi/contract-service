# Import en masse de contrats — notes de version (non publiées)

Décision : ADR-021. Aucune migration, aucun changement cassant.

## Nouvel endpoint

`POST /v2/contracts/imports`, rôle `COUNSELOR`, synchrone, 1000 lignes au plus.

```json
{
  "contracts": [
    { "clientId": "550e8400-e29b-41d4-a716-446655440000", "costAmount": "1500.00" },
    { "clientId": "550e8400-e29b-41d4-a716-446655440000", "startDate": "2027-01-01T00:00:00", "endDate": "2028-01-01T00:00:00", "costAmount": "900.00" }
  ]
}
```

## Tout ou rien

| Réponse | Signification | Base de données |
|---|---|---|
| `201` | Tous les contrats sont créés. `importedCount` et `contractIds`, dans l'ordre des lignes | Toutes les lignes créées |
| `422 contractImportRejected` | Au moins une ligne enfreint une règle métier. `rejections` les liste toutes | Rien |
| `422 invalidImportSize` | Aucune ligne, ou plus de 1000 | Rien |
| `422 validationError` | Champ manquant ou mal typé, nommé `contracts[n].champ` | Rien |

Exemple de rejet (`index` commence à 0) :

```json
{
  "code": "contractImportRejected",
  "detail": "No contract was created. Fix the rejected lines and send the whole import again.",
  "rejections": [
    { "index": 1, "clientId": "550e8400-e29b-41d4-a716-446655440000", "code": "invalidContractCost", "message": "..." },
    { "index": 3, "clientId": "0190a6f2-7b1e-7c3a-9d41-2f6e8c5b1a07", "code": "clientNotFound", "message": "Client not found with id: ..." }
  ]
}
```

Codes d'une ligne rejetée : `invalidContractCost`, `invalidContractPeriod`, `clientNotFound` (client inconnu ou supprimé).

## À savoir

- Après un `422`, corriger les lignes et renvoyer **tout** l'import : rien n'a été créé, donc il n'y a pas de doublon.
- Après un `201`, ne pas renvoyer l'import : les contrats seraient créés une seconde fois. Un contrat n'a pas de clé naturelle, et il n'y a pas encore d'`Idempotency-Key`.
- Une ligne produit un seul rejet, dans cet ordre : client, période, coût. Une fois corrigée, elle peut donc produire un nouveau rejet.
- `clientNotFound` : l'import ne crée pas les clients. Créer le client avec `POST /v2/clients`, remplacer `clientId` dans la ligne par l'identifiant renvoyé, puis renvoyer l'import. Ce code couvre aussi un client supprimé : vérifier avant d'en recréer un.
- Une erreur de syntaxe (`validationError`) est renvoyée avant toute règle métier.

## Changement de configuration

`hibernate.jdbc.batch_size: 50` (`application.yml`). Pour 1000 lignes : 20 lots JDBC au lieu de 1000 exécutions, environ 27 ms au lieu de 296 ms en local (détail dans l'ADR-021). Cette valeur est depuis portée à 1000 par l'import asynchrone (ADR-022).

## Changement de comportement mineur

Le message d'un champ mal typé donne désormais son chemin complet quand le corps a plusieurs niveaux (`contracts[1].costAmount`). Les messages de `POST /v2/contracts` et de `PATCH .../cost` ne changent pas.
