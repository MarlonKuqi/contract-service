# TODO — contract-service

Suivi des sujets en cours. Les décisions sont détaillées dans [ADR.md](docs/ADR.md).

## Fait

| Sujet | Où | ADR |
|---|---|---|
| Suppression de l'unicité email / téléphone | `develop` (471a837) | ADR-008 |
| `Clock` unique injectée, le domaine reçoit le temps | `develop` (471a837) | ADR-003 |
| Suppression du cache de la somme des contrats actifs | `develop` (#71) | ADR-010 |
| `lastModified` porté par le domaine, builder `Contract` privé | `develop` (#72) | ADR-005 |
| Fermeture en masse qui incrémente `version` | `develop` (#73) | ADR-011 |
| Soft delete des clients, FK `ON DELETE RESTRICT` | `develop` (#74) | ADR-009 |
| `CLAUDE.md` racine remplacé, ADR déplacées dans `docs/ADR.md` | `develop` (7961d1d, #75) | — |
| Test d'alignement de la règle « contrat actif » domaine / persistance | `develop` (abb9bd2) | ADR-004 |
| Suppression de `ClientAlreadyExistsException`, `ContractNotOwnedByClientException` et de leurs handlers | `develop` (0751719) | ADR-008 |
| No-op uniforme sur les mises à jour, builders `Person` / `Company` privés, `PatchContractCost` sans `save` inutile | `develop` (#75) | ADR-002 |
| Fermeture des contrats directement dans `DeleteClient` (plus d'événement de domaine) | `develop` (b96c762) | ADR-009 |
| Contrôles « not found » portés par `ClientRepository`, seuls les vrais services de domaine restent (`CompanyIdentifierUniqueness`) | `develop` (ecc1199) | ADR-001 |
| Version de l'application exposée dans l'OpenAPI | `develop` (1e762ec) | — |
| UUID v7 générés par Hibernate et par défaut en base (`uuidv7()`) | `develop` (non commité) | ADR-012 |
| Code mort : index `ix_client_type`, `InvalidClientException`, `InvalidContractException` | `develop` (non commité) | ADR-007 |
| Collections Bruno / Postman alignées sur les contrôleurs, `Close Contract.bru` supprimée | `develop` (non commité) | — |
| README racine (structure, soft delete, endpoints `/v2`), `docs/DDD/` supprimé | `develop` (non commité) | ADR-009 |
| Fuseau `Europe/Zurich` explicite (`app.time-zone`, converter JPA, `TZ` Docker) ; `hibernate.jdbc.time_zone` écarté (sans effet sur `LocalDateTime` en Hibernate 7) | `develop` (7b8b366) | ADR-013 |

## 1. Priorité haute

- [ ] **Authentification des conseillers via Keycloak** (branche `feat/add-auth-keycloak`, ADR à écrire). Spring Security vérifie que chaque appel API vient d'un conseiller authentifié ; certains endpoints leur sont réservés.
  - État de la branche : resource server JWT, `KeycloakJwtConverter`, rôles `ADMIN` / `COUNSELOR` en `@PreAuthorize`, realm Keycloak dans `docker-compose.yml`. Elle part de `f2409d7` et a 9 commits de retard sur `develop` (dont le fuseau 7b8b366) : la remettre à jour avant de continuer.
  - ADR sécurité d'abord : rôles nécessaires (`ADMIN` est-il utile pour ~10 conseillers ?), endpoints réservés aux conseillers, réponses `401` / `403` en `ProblemDetail` avec un `code` stable.
  - Tests : `TestSecurityConfig` ouvre tout (`permitAll`), donc l'autorisation n'est vérifiée nulle part. Ajouter des tests d'acceptance avec un JWT (sans jeton → `401`, mauvais rôle → `403`, conseiller → `2xx`).
  - Docs : collections Bruno / Postman avec l'authentification, README (lancer Keycloak en local, obtenir un jeton), OpenAPI (`securityScheme` bearer).
  - Trier en même temps les fichiers Keycloak non suivis à la racine (`KEYCLOAK_QUICK_START.md`, `SECURITY_README.md`…), voir Priorité moyenne.

## 2. Priorité moyenne

- [x] **ADR à mettre à jour** : ADR-012 passe en « Acceptée », retirer la ligne `ix_client_type` des conséquences de l'ADR-007.
- [ ] **Docs utilisateur**
  - Release notes 2.0.0 : fait, voir `docs/cli/RELEASE_NOTES_2.0.0.md`.
    - plus de 409 sur email / téléphone ;
    - `DELETE` d'un client inconnu ou déjà supprimé : 404 ;
    - soft delete ;
    - plus de cache sur la somme ;
    - mise à jour avec une valeur identique : no-op (pas de changement de `lastModified`) ;
    - identifiants UUID v7 pour les nouveaux clients et contrats, PostgreSQL 18 requis.
  - `docs-claude` : plus de 100 fichiers, dont certains se contredisent. Archiver ce qui est périmé et garder `../ADR.md` comme référence.
  - Fichiers non suivis à la racine (`DOCUMENTATION_INDEX.md`, `GETTING_STARTED.md`, `IMPLEMENTATION_SUMMARY.md`, `KEYCLOAK_QUICK_START.md`, `SECURITY_README.md`, `START_HERE.md`, `scripts/`) : décider s'ils sont versionnés, déplacés ou supprimés. S'ils portent sur Keycloak, une ADR sécurité est nécessaire avant tout code.

## 3. Priorité basse

- [ ] **Audit technique sur la `Clock`** : `created_at` (client, contrat) et `last_modified` (client) via l'audit Spring Data (`@EnableJpaAuditing(dateTimeProviderRef = ...)`) au lieu de `@CreationTimestamp` / `@UpdateTimestamp`.
- [ ] **RGPD** : anonymiser les clients supprimés (nom, email, téléphone) après la durée légale de conservation, sans toucher aux contrats. Une ADR dédiée est nécessaire (durée, déclenchement, irréversibilité).
- [ ] **`contract.client_id NOT NULL`** : vérifier en base qu'aucun contrat n'a de `client_id` à `NULL` (héritage des anciennes suppressions physiques), puis ajouter la migration.
- [ ] **Tests et base réutilisée** : base Testcontainers dédiée par branche, ou `withReuse(false)` en CI, pour éviter les migrations « futures » ignorées par Flyway.

## Conditionnel

- [ ] **Optimistic locking entre requêtes HTTP** (ADR-011, étape 2, différée) : `ETag` / `If-Match`. Seulement si l'un des signaux de l'ADR-011 apparaît : clients en self-service, imports automatiques, forte hausse des utilisateurs ou premier incident de modification perdue.
- [ ] **FK composite `(id, type)`** entre `client` et ses sous-tables (ADR-007) : seulement si plusieurs applications écrivent un jour dans la base.
- [ ] **Accès client à ses propres contrats** (suite de l'authentification Keycloak) : un rôle `CLIENT` en lecture seule, limité à ses propres données. Il faut relier le compte Keycloak au client (`sub` ↔ `client.id`) et contrôler la propriété dans les use cases, pas seulement par rôle. Seulement quand un portail client est demandé.

## Hygiène locale (à ta main)

- [ ] 4 worktrees `copilot/worktree-2026-05-22T*` toujours enregistrés (`git worktree list`).
- [ ] Conteneurs Testcontainers arrêtés dans Docker (`docker ps -a --filter label=org.testcontainers=true`).
