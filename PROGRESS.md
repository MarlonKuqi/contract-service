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
| Authentification des conseillers par Keycloak : rôle unique `counselor` par règles d'URL, audience vérifiée, rôles du realm uniquement, 401/403 en `ProblemDetail`, realm versionné, tests avec JWT signés localement | `feat/add-auth-keycloak` (non commité) | ADR-017 |
| `ETag` / `If-Match` (étape 2 reprise) : ETag fort sur le `GET` unitaire (read models `ClientDetails` / `ContractDetails`), `If-Match` obligatoire sur `PUT`/`PATCH` client et `PATCH` du coût (`428` / `412`), `ensureVersion` dans les ports d'écriture, réessai étendu au `PUT`. Breaking change | `feat/etag-if-match` (non commité) | ADR-011 |

## 1. Priorité haute

- [ ] **Fichiers Keycloak non suivis à la racine** (`KEYCLOAK_QUICK_START.md`, `SECURITY_README.md`…) : les comparer au README et à l'ADR-017, puis les supprimer ou les fusionner.

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

- [ ] **FK composite `(id, type)`** entre `client` et ses sous-tables (ADR-007) : seulement si plusieurs applications écrivent un jour dans la base.
- [ ] **Accès client à ses propres contrats** (suite de l'authentification Keycloak) : un rôle `CLIENT` en lecture seule, limité à ses propres données. Il faut relier le compte Keycloak au client (`sub` ↔ `client.id`) et contrôler la propriété dans les use cases, pas seulement par rôle. Seulement quand un portail client est demandé.

## Pour aller plus loin (entraînement)

Sujets non exigés par `docs/sujet.txt` ni justifiés à l'échelle actuelle (une dizaine de conseillers). Ils servent à s'exercer : chaque ADR le dit explicitement plutôt que d'inventer un besoin.

- [ ] **Types de contrats** : un type qui porte des règles, pas un enum décoratif. Éligibilité selon le type de client (LAMal pour une personne, LAA / LPP pour une entreprise), une seule LAMal active par personne (service de domaine), attributs propres au type (franchise LAMal), droit d'option des frontaliers. Migration, changement d'API et ADR.
- [ ] **Import en masse de contrats** : JDBC batching Hibernate (`jdbc.batch_size`, `order_inserts`, `flush`/`clear` par lot ; possible car les UUID sont générés côté application). Sujet principal : la sémantique d'échec partiel (tout ou rien, ou rapport par ligne). Mesurer le nombre de requêtes avec et sans batching. Extension : import asynchrone (`202 Accepted` + ressource d'import).
- [ ] **Transactional outbox et événements de domaine** : `ContractCreated`, `ContractCostChanged`, `ClientDeleted` écrits dans une table `outbox` dans la même transaction, publiés vers Kafka (Testcontainers). Livraison au moins une fois, consommateur idempotent, ordre par client, version de l'agrégat dans l'événement. Comparer polling `SKIP LOCKED`, Debezium et Spring Modulith dans l'ADR.
- [ ] **`Idempotency-Key` sur `POST /contracts`** : un retry réseau ne crée pas de doublon ; gérer deux requêtes concurrentes avec la même clé.
- [ ] **Reconduction tacite des contrats** : job `@Scheduled` + ShedLock, idempotent, traitement par lots, temps injecté par la `Clock`. Décision métier préalable, impact sur l'ADR-004.
- [ ] **Résilience** : vérification du `companyIdentifier` auprès du registre IDE/UID suisse via `RestClient`, Resilience4j (retry, circuit breaker, timeout), WireMock, comportement en mode dégradé.
- [ ] **Historique des coûts** : répondre à « quel était le coût au 1er janvier ? » (table d'historique ou Envers, requêtes « as-of »).
- [ ] **Propriété du portefeuille** : un conseiller ne voit que ses clients ; règle de propriété dans les use cases, en complément des règles d'URL de l'ADR-017.
- [ ] **Observabilité** : métriques métier Micrometer, traces OpenTelemetry, logs structurés avec corrélation.
- [ ] **Performance** : test de charge Gatling sur la somme des contrats actifs, `EXPLAIN ANALYZE`, ADR-010 confrontée aux chiffres.
- [ ] **Export en streaming** : CSV via `StreamingResponseBody` et curseur JDBC, mémoire constante, comparé à la pagination par curseur de l'ADR-019.

## Hygiène locale (à ta main)

- [ ] 4 worktrees `copilot/worktree-2026-05-22T*` toujours enregistrés (`git worktree list`).
- [ ] Conteneurs Testcontainers arrêtés dans Docker (`docker ps -a --filter label=org.testcontainers=true`).
