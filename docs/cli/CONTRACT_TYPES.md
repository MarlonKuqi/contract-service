# Types de contrats

Décision : [ADR-024](../ADR.md#adr-024--types-de-contrats--un-type-obligatoire-qui-porte-léligibilité).

Chaque contrat porte un type, obligatoire à la création et non modifiable ensuite.

| Type | Assurance | Souscripteur |
|---|---|---|
| `LAMAL` | Assurance-maladie obligatoire | Personne |
| `LAA` | Assurance-accidents | Entreprise |
| `LPP` | Prévoyance professionnelle | Entreprise |

## Créer un contrat

```http
POST /v2/contracts
```

```json
{ "clientId": "550e8400-e29b-41d4-a716-446655440000", "type": "LAMAL", "costAmount": "350.00" }
```

La réponse, comme toutes les lectures de contrats (`GET /v2/contracts/{id}`, liste, export), contient `type`.

## Erreurs

| Cas | Statut | `code` |
|---|---|---|
| `type` absent | 422 | `validationError` (`validations[].field = "type"`) |
| `type` inconnu (`CASCO`, `lamal`) | 422 | `validationError` |
| `LAMAL` pour une entreprise, `LAA` ou `LPP` pour une personne | 422 | `contractTypeNotAllowed` |

Exemple de refus :

```json
{
  "status": 422,
  "title": "Contract Type Not Allowed",
  "detail": "A LAMAL contract cannot be taken out by a company",
  "code": "contractTypeNotAllowed"
}
```

## Imports

- Import synchrone (`POST /v2/contracts/imports`) : `type` est obligatoire sur chaque ligne. Un type non autorisé pour le client rejette la ligne avec `contractTypeNotAllowed`, et donc tout l'import.
- Import par fichier (`POST /v2/contracts/import-jobs`) : l'en-tête devient `clientId;type;startDate;endDate;costAmount`. Un type inconnu rejette la ligne avec `invalidLineFormat`, un type non autorisé avec `contractTypeNotAllowed` ; les autres lignes sont importées.

## Changements pour les appelants

- `POST /v2/contracts` et `POST /v2/contracts/imports` sans `type` répondent 422.
- Un fichier CSV avec l'ancien en-tête à 4 colonnes est refusé (`invalidImportFile`).
- La base ne doit contenir aucun contrat avant la migration `V2.0.0.10` : la colonne `type` est `NOT NULL` sans valeur par défaut.

## Hors périmètre

Une seule LAMal active par personne, franchise LAMal, droit d'option des frontaliers : voir `PROGRESS.md`.
