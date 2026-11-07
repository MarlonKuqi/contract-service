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
| `ETag` / `If-Match` (étape 2 reprise) : ETag fort sur le `GET` unitaire (read models `ClientDetails` / `ContractDetails`), `If-Match` obligatoire sur `PUT`/`PATCH` client et `PATCH` du coût (`428` / `412`), `ensureVersion` dans les ports d'écriture, conflit au commit traduit en `412` sans réessai. Breaking change | `feat/etag-if-match` (non commité) | ADR-011 |

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

## CI/CD et `.github`

Revue de `.github/` du 2026-11-07. Corrections appliquées sur `feat/pipeline` (non commité). GitHub ne lit les templates et `dependabot.yml` que sur la branche par défaut (`master`) : c'est pour ça que les templates ne s'affichaient pas. Ils ne s'appliqueront qu'une fois mergés dans `master`.

- [x] **Dependabot** : une entrée par écosystème pour `master` (ce qui tourne en prod), `develop` (la prochaine version) et `release/2.0.0`. `target-branch` n'accepte pas de joker : ajouter chaque nouvelle release à la main.
- [x] **Image Docker** : construite sur les pushs vers `master`, `develop` et `release/**`, et à la demande sur une autre branche (`workflow_dispatch`, case « Build and scan the Docker image »). Plus de `continue-on-error` sur le job : un `docker build` cassé fait échouer la CI. Trivy reste informatif (`exit-code: '0'`). Cache des couches via Buildx (`type=gha`).
- [x] **Seuil de couverture supprimé** (`pom.xml`, CI, ADR-014, README) : JaCoCo reste un rapport, PIT est le seul critère bloquant.
- [x] **`permissions`** : `contents: read` par défaut, `pull-requests: write` pour le seul job qui commente la PR, `security-events: write` pour Trivy. `persist-credentials: false` sur les checkouts.
- [x] **Codecov supprimé** : sans seuil, le commentaire JaCoCo sur la PR et l'artefact `coverage-report` suffisent.
- [x] **Déclencheurs** : `push` limité aux branches majeures, `pull_request` sur toutes les cibles, `concurrency` qui annule les exécutions dépassées d'une PR (jamais celles des branches majeures).
- [x] **Pipeline par étapes conservée** (échec rapide) : compilation → tests unitaires + rapport JaCoCo → en parallèle tests d'acceptation, PIT et image. Doublons retirés : `install`, étape `jacoco:check` (déjà dans la phase `test`), cache `actions/cache` en plus de `setup-java`. Maven passe par le wrapper (`sh mvnw -B`).
- [x] **Résumé de tests** : `.github/scripts/test-summary.sh` lit les rapports Surefire / Failsafe (total, réussis, échecs, erreurs, ignorés, classes en échec), à la place du comptage de fichiers XML.
- [x] **Résumé PIT** : `.github/scripts/mutation-summary.sh` lit `target/pit-reports/mutations.xml` (mutants, tués, survivants, non couverts, score, solidité, liste des mutants non détectés) ; affiché dans le résumé du run et dans un commentaire de PR unique mis à jour à chaque push (`marocchino/sticky-pull-request-comment`). Pas d'outil externe : pour aller plus loin, PIT en local.
- [x] **Actions tierces épinglées par SHA**, `timeout-minutes` sur chaque job, `retention-days: 14` sur les rapports, artefact JAR, `fetch-depth: 0` et `TESTCONTAINERS_RYUK_DISABLED` retirés.
- [x] **Templates** : PR, feature et bug réécrits, courts et propres au projet (titre = commit squashé, branche cible et méthode de merge ADR-020, impacts ADR / Flyway / API / breaking change, TDD, `./mvnw clean verify`). Issues vierges désactivées.

### Reste à faire

- [ ] **`mvnw` non exécutable dans git** (mode `100644`) : la CI l'appelle avec `sh mvnw`. Pour revenir à `./mvnw` : `git update-index --chmod=+x mvnw`.
- [x] **Error Prone + NullAway** (ADR-023), sur `feat/pipeline` (non commité) : contrôles par défaut d'Error Prone, NullAway en erreur sur `src/main` (`AnnotatedPackages=com.mk.contractservice`, le `@NullMarked` du package racine ne couvrant pas les sous-packages), désactivé sur les tests, entités JPA exclues. `--add-exports` du compilateur dans `.mvn/jvm.config`.
  - `@NullMarked` dans un `package-info.java` de chacun des 22 packages, pour que l'IDE applique la même règle ; `ArchitectureTest.every_package_should_be_null_marked` refuse un package sans.
  - 39 erreurs corrigées : génériques `@Nullable` dans `ValueObjectUtils`, `@Nullable` manquants (fabrique de contrat, `ExpiredContractException`, `DomainValidationException`, `detail` du `ProblemDetail`, `clientId` de `ContractDetails` / `ContractResponse` tant que la colonne accepte `NULL`), `Objects.requireNonNull` sur l'id dans les mappers de réponse.
  - No-op des mises à jour : `updated.equals(loaded)` au lieu de `updated == loaded` (`PatchClient`, `UpdateClient`, `PatchContractCost`), règle mise à jour dans `CLAUDE.md` et l'ADR-002.
  - Avertissements Error Prone corrigés : méthodes `public` des builders privés, lambda constante de `ContractPeriod`, `toUpperCase(Locale.ROOT)`, champ inutilisé, `LocalDateTime.of` au lieu de `now(FIXED_CLOCK)` en initialiseur statique, assertions `float` remplacées par `comparesEqualTo(BigDecimal)` (`AcceptanceRestAssured.exactNumbers()`), vérifiées en échec sur une erreur d'un centime.
- [x] **Spotless** (2026-11-07) : `spotless-maven-plugin` 3.11.0 avec le profil `config/formatter/eclipse-java-formatter.xml`, ordre d'imports d'IntelliJ, imports inutilisés et espaces de fin supprimés, `ratchetFrom origin/develop` : seuls les fichiers modifiés sont vérifiés. En CI, `spotless:check` est la première étape du job unitaire, comparée à la branche cible de la PR (`fetch-depth: 0`). En local : `./mvnw spotless:apply`.
  - Profil corrigé pour garder le style du code : paramètres, composants de record et arguments indentés de 8 espaces sans alignement sur la parenthèse, chaînes d'appels à un appel par ligne.
  - Écarts acceptés : annotations des composants de record remontées sur la ligne du composant (DTO), arguments tous à la ligne dès qu'un appel est coupé, ternaires recollés.
- [ ] **Règles de branche** (paramètre GitHub, hors dépôt) : jobs CI obligatoires sur `develop`, `release/*` et `master`, rebase-merge interdit (ADR-020).
- [ ] **Dependabot (basse)** : login `reviewers` à vérifier (champ remplacé par `CODEOWNERS`), regroupement hebdomadaire, `ignore` sans effet, image JDK du `Dockerfile` non suivie car passée par un `ARG`, écosystème `docker-compose`, préfixe `ci` hors des types de `CLAUDE.md`.

### Pour aller plus loin (CD, entraînement)

- [ ] **Publication de l'image** : sur un tag `vX.Y.Z` ou un push sur `release/*`, pousser l'image vers GHCR avec SBOM et attestation de provenance (`docker/build-push-action` avec `sbom: true` / `provenance: true`), puis la scanner. Créer la release GitHub avec les notes de `docs/cli/RELEASE_NOTES_*.md`. Pas de cible de déploiement aujourd'hui : rester à la publication d'image.

## Conditionnel

- [ ] **FK composite `(id, type)`** entre `client` et ses sous-tables (ADR-007) : seulement si plusieurs applications écrivent un jour dans la base.
- [ ] **Accès client à ses propres contrats** (suite de l'authentification Keycloak) : un rôle `CLIENT` en lecture seule, limité à ses propres données. Il faut relier le compte Keycloak au client (`sub` ↔ `client.id`) et contrôler la propriété dans les use cases, pas seulement par rôle. Seulement quand un portail client est demandé.

## Pour aller plus loin (entraînement)

Sujets non exigés par `docs/sujet.txt` ni justifiés à l'échelle actuelle (une dizaine de conseillers). Ils servent à s'exercer : chaque ADR le dit explicitement plutôt que d'inventer un besoin.

- [ ] **Types de contrats** : un type qui porte des règles, pas un enum décoratif. Éligibilité selon le type de client (LAMal pour une personne, LAA / LPP pour une entreprise), une seule LAMal active par personne (service de domaine), attributs propres au type (franchise LAMal), droit d'option des frontaliers. Migration, changement d'API et ADR.
- [x] **Import en masse de contrats** (ADR-021) : `POST /v2/contracts/imports`, synchrone, JSON, tout ou rien avec rapport par ligne, 1000 lignes au plus. JDBC batching mesuré (`batch_size=50` : 20 lots au lieu de 1000 exécutions, environ 10 fois plus rapide ; `flush`/`clear` et `order_inserts` sans intérêt ici). Extension possible : import asynchrone (`202 Accepted` + ressource d'import).
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
