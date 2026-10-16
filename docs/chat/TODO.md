# TODO — contract-service

Suivi des sujets en cours. Les décisions sont détaillées dans [ADR.md](ADR.md).

## Fait

| Sujet | Où | ADR |
|---|---|---|
| Suppression de l'unicité email / téléphone | `develop` (471a837) | ADR-008 |
| `Clock` unique injectée, le domaine reçoit le temps | `develop` (471a837) | ADR-003 |
| Suppression du cache de la somme des contrats actifs | `develop` (#71) | ADR-010 |
| `lastModified` porté par le domaine, builder `Contract` privé | `feat/contract-last-modified-in-domain` | ADR-005 |
| Soft delete des clients, FK `ON DELETE RESTRICT` | `feat/client-soft-delete` | ADR-009 |
| Fermeture en masse qui incrémente `version` | `bugfix/contract-closure-lost-update` | ADR-011 |

## 0. À faire maintenant

- [ ] **Merger les trois branches en attente** dans `develop`.
  - Conflit probable dans `JpaContractRepository.closeAllActiveByClientId` : la branche `lastModified` supprime les deux commentaires au-dessus de `update.set`, la branche bugfix ajoute la ligne `version` juste en dessous, et #71 a retiré les `@CacheEvict` du même fichier. Résolution : garder les trois changements.
  - Après chaque merge : `./mvnw clean verify`. Supprimer le conteneur Testcontainers réutilisé si des erreurs de schéma apparaissent.
- [ ] **Remplacer le `CLAUDE.md` racine** par [docs/chat/CLAUDE.md](CLAUDE.md). La version actuelle décrit une structure qui n'existe pas (`application/`, controllers dans `infrastructure/`), présente Java 27 comme LTS (ce n'est pas le cas), et renvoie vers un ADR absent (`docs-claude/ADR_CONTRACT_ACTIVE_DEFINITION.md`).

## 1. Priorité haute

- [ ] **Fuseau `Europe/Zurich` explicite** (ADR-013). L'image Docker tourne en UTC, ce qui décale les heures saisies d'1 à 2 h.
  - `ClockConfiguration` : `Clock.system(ZoneId.of(...))`, avec une propriété `app.time-zone`.
  - `spring.jpa.properties.hibernate.jdbc.time_zone=Europe/Zurich`.
  - `ENV TZ=Europe/Zurich` dans le `Dockerfile`, ou `-Duser.timezone`.
  - Test : une date saisie est relue identique, quel que soit le fuseau de la JVM.
- [ ] **Optimistic locking entre requêtes HTTP** (ADR-011, étape 2).
  - `ETag` = version sur `GET /clients/{id}` et `GET /contracts/{id}`.
  - `If-Match` obligatoire sur `PUT`/`PATCH` ; `412` si la version a changé, `428` si l'en-tête manque.
  - La version voyage dans la commande ; l'agrégat la porte comme jeton opaque.
  - Faire évoluer `UpdateClientAcceptanceTest` « last one wins ».
  - Mettre à jour les collections Postman (`api-collections/`) et le Swagger.

## 2. Priorité moyenne

- [ ] **UUID v7** (ADR-012) : `@UuidGenerator(style = VERSION_7)` sur les deux entités, et migration `DEFAULT uuidv7()` sur `client.id` et `contract.id`.
- [ ] **Nettoyage du code mort**
  - `ClientAlreadyExistsException` et son handler dans `ClientControllerAdvice` (jamais levée).
  - Index `ix_client_type` (migration `DROP INDEX`).
  - Mentions de 409 email / téléphone restantes dans la doc et les collections.
- [ ] **Builders privés pour `Person` et `Company`** (règle CLAUDE.md, déjà appliquée à `Contract`). Les tests passent alors par `of()` ou les factories.
- [ ] **Docs utilisateur**
  - README : la section « Project Structure » décrit `application/` et `web/` (inexistants) ; la ligne « Soft Delete » est à réécrire selon ADR-009.
  - Release notes 2.0.0 :
    - plus de 409 sur email / téléphone ;
    - `DELETE` d'un client inconnu ou déjà supprimé : 404 ;
    - soft delete ;
    - plus de cache sur la somme.
  - `docs-claude/` : plus de 60 fichiers, dont certains se contredisent. Archiver ce qui est périmé et garder `docs/chat/ADR.md` comme référence.

## 3. Priorité basse

- [ ] **Audit technique sur la `Clock`** : `created_at` (client, contrat) et `last_modified` (client) via l'audit Spring Data (`@EnableJpaAuditing(dateTimeProviderRef = ...)`) au lieu de `@CreationTimestamp` / `@UpdateTimestamp`.
- [ ] **RGPD** : anonymiser les clients supprimés (nom, email, téléphone) après la durée légale de conservation, sans toucher aux contrats. Une ADR dédiée est nécessaire (durée, déclenchement, irréversibilité).
- [ ] **`contract.client_id NOT NULL`** : vérifier en base qu'aucun contrat n'a de `client_id` à `NULL` (héritage des anciennes suppressions physiques), puis ajouter la migration.
- [ ] **Tests et base réutilisée** : base Testcontainers dédiée par branche, ou `withReuse(false)` en CI, pour éviter les migrations « futures » ignorées par Flyway.
- [ ] **`PatchContractCost`** : ne pas appeler `save` quand `changeCost` renvoie le même contrat (aucun SQL n'est émis aujourd'hui grâce au dirty checking, mais l'intention serait explicite, comme dans `PatchClient`).

## Conditionnel

- [ ] **`Instant` / `OffsetDateTime` / UTC** (ADR-013) : seulement si l'application s'ouvre à des utilisateurs hors du fuseau `Europe/Zurich`. Change le format des dates de l'API.
- [ ] **FK composite `(id, type)`** entre `client` et ses sous-tables (ADR-007) : seulement si plusieurs applications écrivent un jour dans la base.

## Hygiène locale (à ta main)

- [ ] 4 worktrees `copilot/worktree-2026-05-22T*` toujours enregistrés (`git worktree list`).
- [ ] Conteneurs Testcontainers arrêtés dans Docker (`docker ps -a --filter label=org.testcontainers=true`).
