# Modifier la période d'un contrat

Décision : [ADR-025](../ADR.md#adr-025--modification-de-la-période-dun-contrat).

```http
PUT /v2/contracts/{contractId}/period
If-Match: "3"
Content-Type: application/json
```

La période est remplacée en entier : envoyer les deux dates, telles qu'elles doivent être après la modification. La période courante est connue, puisque `If-Match` impose de lire le contrat avant (`GET /v2/contracts/{id}`).

| Corps | Effet |
|---|---|
| `{ "startDate": "2025-01-01T00:00:00", "endDate": "2027-12-31T00:00:00" }` | Nouvelle période |
| `{ "startDate": "2025-01-01T00:00:00", "endDate": null }` | Contrat sans fin (`endDate` peut aussi être omis) |
| La période actuelle | Aucune modification |

Réponse `204`, sans ETag : relire le contrat avant une autre modification.

## Règles

- Seul un contrat actif (non expiré) est modifiable : un contrat expiré ne peut pas être réactivé, il faut en créer un nouveau.
- La date de début peut être déplacée, y compris dans le passé.
- La date de fin ne peut pas être dans le passé (heure du serveur, `Europe/Zurich`). Pour résilier immédiatement, envoyer une date légèrement future, par exemple la fin de la journée.
- La date de fin doit rester après la date de début ; années entre 1 et 9999.
- Pour une LAMal, la nouvelle période ne doit chevaucher aucune autre LAMal active de la même personne (voir [CONTRACT_TYPES.md](CONTRACT_TYPES.md)). Sa propre période précédente ne compte pas. Pour prolonger une LAMal suivie d'une autre, raccourcir d'abord la suivante.
- Une modification met à jour la date de dernière modification interne, utilisée par le filtre `updatedSince` de la liste.

## Erreurs

| Cas | Statut | `code` |
|---|---|---|
| `If-Match` absent ou `*` | 428 | `preconditionRequired` |
| Contrat inconnu | 404 | `contractNotFound` |
| ETag périmé | 412 | `preconditionFailed` |
| `startDate` absent ou `null`, date illisible, corps absent | 422 | `validationError` |
| Fin dans le passé, fin pas après le début, année hors limites | 422 | `invalidContractPeriod` |
| Contrat expiré | 409 | `contractExpired` |
| Nouvelle période d'une LAMal qui chevauche une autre LAMal active de la personne | 409 | `contractOverlap` |
