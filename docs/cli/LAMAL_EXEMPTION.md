# Résidence et exemption LAMal des frontaliers

Décision : [ADR-024, incrément 4b](../ADR.md#incrément-4b--droit-doption-des-frontaliers).

## Résidence d'une personne

Une personne a toujours une résidence : `CH`, ou un pays frontalier pour un frontalier (`FR`, `DE`, `IT`, `AT`). Une entreprise n'en a pas.

| Requête | `residence` |
|---|---|
| `POST /v2/clients` (personne) | obligatoire |
| `PUT /v2/clients/{id}` | absent : le `PUT` ne modifie que nom, email et téléphone |
| `PATCH /v2/clients/{id}` | facultatif, `null` explicite refusé, ignoré pour une entreprise |
| `GET /v2/clients/{id}` (personne) | toujours renvoyé |

```json
{ "type": "PERSON", "name": "Lena Brunner", "email": "lena@example.com", "phone": "+41791234567", "birthDate": "1985-03-20", "residence": "FR" }
```

Les personnes créées avant la migration `V2.0.0.13` résident en Suisse (`CH`).

## Exemption LAMal

Un frontalier peut être exempté de la LAMal par le canton où il travaille pour s'assurer dans son pays de résidence. Le conseiller enregistre la décision avec sa date d'effet, rétroactive si besoin.

```http
PUT /v2/clients/{id}/lamal-exemption
```

```json
{ "startDate": "2026-01-01", "endDate": null }
```

- `startDate` incluse, `endDate` exclusive et facultative (exemption en cours).
- Une seule exemption par personne : le `PUT` la remplace. Pour la fermer, la renvoyer avec une `endDate`. Renvoyer la même exemption ne change rien.
- Pas d'`If-Match`. Réponse `204`.

```http
GET /v2/clients/{id}/lamal-exemption
```

```json
{ "startDate": "2026-01-01", "endDate": null }
```

## Règles

| Situation | Résultat |
|---|---|
| Exemption d'une personne résidant en Suisse, ou d'une entreprise | `422 lamalExemptionNotAllowed` |
| Exemption sans `startDate` | `422 validationError` |
| `endDate` le jour de `startDate` ou avant | `422 invalidLamalExemptionPeriod` |
| Exemption qui couvre une LAMal active de la personne | `409 lamalExemptionConflict` : fermer ou raccourcir la LAMal d'abord |
| LAMal (création, changement de période, imports) qui couvre l'exemption, même passée | `409 lamalExemptionConflict`, rejet de ligne `lamalExemptionConflict` dans les imports |
| LAMal qui commence le jour où l'exemption finit | Acceptée |
| Passage en Suisse (`residence: CH`) pendant une exemption en cours ou à venir | `409 lamalExemptionOngoing` : fermer l'exemption d'abord |
| Passage d'un pays frontalier à un autre pendant l'exemption | Accepté |
| Lecture de l'exemption d'une personne qui n'en a pas | `404 lamalExemptionNotFound` |
| Client inconnu ou supprimé | `404 clientNotFound` |

```json
{
  "status": 409,
  "title": "LAMal Exemption Ongoing",
  "detail": "Person 550e8400-e29b-41d4-a716-446655440000 is exempted from LAMal from 2026-01-01 to no end date: close the exemption before moving to Switzerland",
  "code": "lamalExemptionOngoing"
}
```

## Changements pour les appelants

- `POST /v2/clients` d'une personne sans `residence` répond `422 validationError`.
- Nouveau code de rejet de ligne dans les deux imports : `lamalExemptionConflict`.

## Hors périmètre

Historique des exemptions, suppression d'une exemption enregistrée par erreur (la fermer), membres de la famille, règles propres à chaque pays.
