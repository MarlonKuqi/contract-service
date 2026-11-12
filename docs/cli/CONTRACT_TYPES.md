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
{ "clientId": "550e8400-e29b-41d4-a716-446655440000", "type": "LAMAL", "franchise": 300, "costAmount": "350.00" }
```

La réponse, comme toutes les lectures de contrats (`GET /v2/contracts/{id}`, liste, export), contient `type` et `franchise` (`null` pour une LAA ou une LPP).

## Erreurs

| Cas | Statut | `code` |
|---|---|---|
| `type` absent | 422 | `validationError` (`validations[].field = "type"`) |
| `type` inconnu (`CASCO`, `lamal`) | 422 | `validationError` |
| `LAMAL` pour une entreprise, `LAA` ou `LPP` pour une personne | 422 | `contractTypeNotAllowed` |
| LAMal sans franchise, franchise sur une LAA ou une LPP, montant hors barème | 422 | `invalidContractFranchise` |
| Franchise non autorisée pour l'âge de l'assuré, LAMal d'enfant sans fin ou qui finit après l'enfance | 422 | `invalidContractFranchise` |
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

## Franchise LAMal

Décision : [ADR-024, incrément 3](../ADR.md#incrément-3--franchise-lamal).

Une LAMal porte toujours une franchise annuelle en CHF (`franchise`, un entier). Une valeur décimale (`300.5`) est refusée en `422 validationError` au lieu d'être tronquée. Une LAA ou une LPP n'en a pas : on n'envoie pas le champ, ou on envoie `null`.

| Assuré | Franchises autorisées |
|---|---|
| Adulte (dès le 1er janvier de l'année de ses 19 ans) | 300, 500, 1000, 1500, 2000, 2500 |
| Enfant (jusqu'au 31 décembre de l'année de ses 18 ans) | 0, 100, 200, 300, 400, 500, 600 |

- La catégorie dépend de l'année de naissance et de la date de début du contrat, pas du jour d'anniversaire.
- La LAMal d'un enfant doit finir au plus tard le 1er janvier de l'année de ses 19 ans. Une LAMal d'adulte prend la suite (voir plus haut, pas de chevauchement).
- La règle s'applique aussi au changement de période (`PUT /v2/contracts/{id}/period`) : retirer la date de fin de la LAMal d'un enfant, ou déplacer le début d'une LAMal à 1000 CHF dans l'enfance de l'assuré, répond `422 invalidContractFranchise`.
- La franchise ne se modifie pas. Pour en changer, fermer la LAMal au 31 décembre et créer celle de l'année suivante.

| Personne née le | LAMal | Résultat |
|---|---|---|
| 1985-03-20 | franchise 2500, sans fin | Acceptée |
| 1985-03-20 | franchise 0 | `422` : barème enfant |
| 2016-05-01 | franchise 0, fin 2035-01-01 | Acceptée |
| 2016-05-01 | franchise 0, sans fin | `422` : doit finir au plus tard le 2035-01-01 |
| 2016-05-01 | franchise 1000, fin 2035-01-01 | `422` : barème adulte |
| 2008-12-31 | début 2027-01-01, franchise 300, sans fin | Acceptée : adulte dès le 1er janvier de ses 19 ans |

```json
{
  "status": 422,
  "title": "Invalid Contract Franchise",
  "detail": "A LAMAL franchise of 1000 CHF is not allowed for a child. Allowed: [0, 100, 200, 300, 400, 500, 600]",
  "code": "invalidContractFranchise"
}
```

## Imports

- Import synchrone (`POST /v2/contracts/imports`) : `type` est obligatoire sur chaque ligne, `franchise` sur chaque ligne LAMal. Une franchise refusée rejette la ligne avec `invalidContractFranchise`. Un type non autorisé pour le client rejette la ligne avec `contractTypeNotAllowed`, une LAMal qui chevauche une LAMal existante ou une ligne précédente du même import avec `contractOverlap`, et donc tout l'import.
- Import par fichier (`POST /v2/contracts/import-jobs`) : l'en-tête devient `clientId;type;franchise;startDate;endDate;costAmount` (franchise vide pour une LAA ou une LPP). Un type inconnu ou une franchise illisible rejette la ligne avec `invalidLineFormat`, une franchise refusée avec `invalidContractFranchise`, un type non autorisé avec `contractTypeNotAllowed`, une LAMal en chevauchement (avec la base, une ligne précédente du fichier) avec `contractOverlap` ; les autres lignes sont importées.

## Changements pour les appelants

- `POST /v2/contracts` et `POST /v2/contracts/imports` sans `type` répondent 422.
- Un fichier CSV avec un ancien en-tête (4 ou 5 colonnes) est refusé (`invalidImportFile`).
- La base ne doit contenir aucun contrat avant la migration `V2.0.0.10` : la colonne `type` est `NOT NULL` sans valeur par défaut.
- `POST /v2/contracts` peut répondre `409 contractOverlap`, et les imports rejettent une ligne avec ce code.
- Une LAMal sans `franchise` répond `422 invalidContractFranchise` (création et imports). La base ne doit contenir aucune LAMal avant la migration `V2.0.0.11` : la colonne `franchise` est obligatoire pour une LAMal, sans valeur par défaut.

## Hors périmètre

Changement de franchise d'une LAMal existante, droit d'option des frontaliers : voir `PROGRESS.md`.
