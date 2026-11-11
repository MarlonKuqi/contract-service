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
| `LAMAL` qui chevauche une autre LAMal de la même personne | 409 | `contractOverlap` |

Exemple de refus :

```json
{
  "status": 422,
  "title": "Contract Type Not Allowed",
  "detail": "A LAMAL contract cannot be taken out by a company",
  "code": "contractTypeNotAllowed"
}
```

## Pas de LAMal qui se chevauchent

Une nouvelle LAMal ne peut pas couvrir un instant déjà couvert par une LAMal active de la même personne (pas encore expirée, futures comprises). Une LAMal terminée ou fermée (client supprimé, import annulé) ne compte plus. La date de fin étant exclusive (ADR-004), une LAMal peut commencer à l'instant où la précédente finit : on peut souscrire la LAMal de l'année prochaine pendant que celle de l'année en cours court encore.

| LAMal existante | Nouvelle LAMal | Résultat |
|---|---|---|
| 2027-01-01 → 2028-01-01 | 2028-01-01 → sans fin | Acceptée |
| 2027-01-01 → 2028-01-01 | 2027-07-01 → 2029-01-01 | `409 contractOverlap` |
| 2027-01-01 → sans fin | 2030-01-01 → sans fin | `409 contractOverlap` : fermer d'abord la LAMal en cours |
| 2020-01-01 → 2021-01-01 (terminée) | 2020-06-01 → 2022-01-01 | Acceptée : seules les LAMal actives comptent |
| 2026-01-01 → fermée le 2026-11-10 (import annulé) | 2026-01-01 → sans fin | Acceptée : la LAMal fermée n’est plus active |
| LAMal d'une autre personne | même période | Acceptée |

La LAA et la LPP d'une entreprise ne sont pas concernées : une entreprise peut en avoir plusieurs en même temps.

```json
{
  "status": 409,
  "title": "Overlapping Contract",
  "detail": "Client 550e8400-e29b-41d4-a716-446655440000 already has a LAMAL contract from 2027-01-01T00:00 to 2028-01-01T00:00 overlapping this period",
  "code": "contractOverlap"
}
```

Deux créations simultanées pour la même personne sont traitées l'une après l'autre : la seconde attend la fin de la première, puis répond `409 contractOverlap` avec le message précis si les périodes se chevauchent. Si elle attend plus de 5 s (import en cours sur ce client, par exemple), elle répond `409 resourceBusy` : renvoyer la requête.

## Imports

- Import synchrone (`POST /v2/contracts/imports`) : `type` est obligatoire sur chaque ligne. Un type non autorisé pour le client rejette la ligne avec `contractTypeNotAllowed`, une LAMal qui chevauche une LAMal existante ou une ligne précédente du même import avec `contractOverlap`, et donc tout l'import.
- Import par fichier (`POST /v2/contracts/import-jobs`) : l'en-tête devient `clientId;type;startDate;endDate;costAmount`. Un type inconnu rejette la ligne avec `invalidLineFormat`, un type non autorisé avec `contractTypeNotAllowed`, une LAMal en chevauchement (avec la base, une ligne précédente du fichier) avec `contractOverlap` ; les autres lignes sont importées.

## Changements pour les appelants

- `POST /v2/contracts` et `POST /v2/contracts/imports` sans `type` répondent 422.
- Un fichier CSV avec l'ancien en-tête à 4 colonnes est refusé (`invalidImportFile`).
- La base ne doit contenir aucun contrat avant la migration `V2.0.0.10` : la colonne `type` est `NOT NULL` sans valeur par défaut.
- `POST /v2/contracts` peut répondre `409 contractOverlap`, et les imports rejettent une ligne avec ce code.

## Hors périmètre

Franchise LAMal, droit d'option des frontaliers : voir `PROGRESS.md`.
