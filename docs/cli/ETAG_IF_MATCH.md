# ETag / If-Match — notes de version (non publiées)

Branche : `feat/etag-if-match`. Décision : ADR-011, étape 2.

## ⚠️ Breaking change

Les modifications exigent désormais l'en-tête `If-Match` avec l'ETag de la ressource lue :

| Endpoint | Sans `If-Match` ou `If-Match: *` | ETag périmé, faible ou invalide |
|---|---|---|
| `PUT /v2/clients/{id}` | `428 preconditionRequired` | `412 preconditionFailed` |
| `PATCH /v2/clients/{id}` | `428 preconditionRequired` | `412 preconditionFailed` |
| `PATCH /v2/contracts/{contractId}/cost` | `428 preconditionRequired` | `412 preconditionFailed` |

`DELETE /v2/clients/{id}` est inchangé.

## Nouveautés

- `GET /v2/clients/{id}` et `GET /v2/contracts/{contractId}` renvoient un ETag fort, par exemple `ETag: "3"`.
- Les créations et les modifications ne renvoient pas d'ETag : relire la ressource avant chaque modification.
- `GET` avec `If-None-Match` égal à l'ETag courant : `304 Not Modified`.
- CORS : `ETag`, `Location` et `Content-Range` sont lisibles par un navigateur ; `If-Match` et `If-None-Match` sont autorisés.

## Changements de comportement

- Une modification basée sur une lecture périmée donne `412` au lieu d'être appliquée (« le dernier gagne » disparaît pour le coût et le `PATCH` d'un client).
- Le coût d'un contrat fermé par la suppression de son client depuis la lecture donne `412` (et non plus `409 contractExpired`) : relire le contrat montre sa date de fin.
- `409 optimisticLockFailure` n'est plus renvoyé que par la suppression d'un client.

## Migration d'un client de l'API

1. Lire la ressource (`GET`) et garder son ETag.
2. Envoyer `If-Match: <ETag>` avec la modification.
3. Sur `412`, relire la ressource, présenter les nouvelles valeurs à l'utilisateur, puis réessayer avec le nouvel ETag.

Aucune migration de base : la colonne `version` existe depuis `V2.0.0.0`.
