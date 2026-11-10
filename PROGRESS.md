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
| UUID v7 générés par Hibernate et par défaut en base (`uuidv7()`) | `develop` | ADR-012 |
| Code mort : index `ix_client_type`, `InvalidClientException`, `InvalidContractException` | `develop` | ADR-007 |
| Collections Bruno / Postman alignées sur les contrôleurs, `Close Contract.bru` supprimée | `develop` | — |
| README racine (structure, soft delete, endpoints `/v2`), `docs/DDD/` supprimé | `develop` | ADR-009 |
| Fuseau `Europe/Zurich` explicite (`app.time-zone`, converter JPA, `TZ` Docker) ; `hibernate.jdbc.time_zone` écarté (sans effet sur `LocalDateTime` en Hibernate 7) | `develop` (7b8b366) | ADR-013 |
| Authentification des conseillers par Keycloak : rôle unique `counselor` par règles d'URL, audience vérifiée, rôles du realm uniquement, 401/403 en `ProblemDetail`, realm versionné, tests avec JWT signés localement | `develop` (#78) | ADR-017 |
| `ETag` / `If-Match` (étape 2 reprise) : ETag fort sur le `GET` unitaire (read models `ClientDetails` / `ContractDetails`), `If-Match` obligatoire sur `PUT`/`PATCH` client et `PATCH` du coût (`428` / `412`), `ensureVersion` dans les ports d'écriture, conflit au commit traduit en `412` sans réessai. Breaking change | `develop` (#82) | ADR-011 |
| Accesseurs fluides Lombok (`lombok.config` racine), JavaBean conservé pour les entités JPA | `develop` (43bb8f6) | ADR-002 |
| Types de contrats, incrément 1 : type obligatoire et immuable (`LAMAL`, `LAA`, `LPP`), éligibilité selon la nature du client, `type` dans l'API et les deux imports, migration `V2.0.0.10` | `develop` (358aa10) | ADR-024 |
| Modification de la période d'un contrat : `PUT /v2/contracts/{id}/period` (remplacement de la période, `If-Match`), début modifiable même dans le passé, fin jamais dans le passé, `lastModified` mis à jour | `feat/patch-contract-periods` | ADR-025 |

## 1. Priorité moyenne

- [x] **ADR à mettre à jour** : ADR-012 passe en « Acceptée », retirer la ligne `ix_client_type` des conséquences de l'ADR-007.
- [ ] **Docs utilisateur**
  - Release notes 2.0.0 : à écrire dans `docs/cli/RELEASE_NOTES_2.0.0.md` (fichier absent de toutes les branches) :
    - `type` obligatoire sur les contrats (`POST /v2/contracts`, imports JSON et CSV), éligibilité selon la nature du client (ADR-024) ;
    - plus de 409 sur email / téléphone ;
    - `DELETE` d'un client inconnu ou déjà supprimé : 404 ;
    - soft delete ;
    - plus de cache sur la somme ;
    - mise à jour avec une valeur identique : no-op (pas de changement de `lastModified`) ;
    - identifiants UUID v7 pour les nouveaux clients et contrats, PostgreSQL 18 requis.
  - `docs-claude` : 56 fichiers suivis, dont certains se contredisent. Archiver ce qui est périmé et garder `docs/ADR.md` comme référence.

## 2. Priorité basse

- [ ] **Audit technique sur la `Clock`** : `created_at` (client, contrat) et `last_modified` (client) via l'audit Spring Data (`@EnableJpaAuditing(dateTimeProviderRef = ...)`) au lieu de `@CreationTimestamp` / `@UpdateTimestamp`.
- [ ] **RGPD** : anonymiser les clients supprimés (nom, email, téléphone) après la durée légale de conservation, sans toucher aux contrats. Une ADR dédiée est nécessaire (durée, déclenchement, irréversibilité).
- [ ] **`contract.client_id NOT NULL`** : vérifier en base qu'aucun contrat n'a de `client_id` à `NULL` (héritage des anciennes suppressions physiques), puis ajouter la migration.
- [ ] **Tests et base réutilisée** : base Testcontainers dédiée par branche, ou `withReuse(false)` en CI, pour éviter les migrations « futures » ignorées par Flyway.

## CI/CD et `.github`

Revue de `.github/` du 2026-11-07. Corrections mergées sur `develop` (#86). GitHub ne lit les templates et `dependabot.yml` que sur la branche par défaut (`master`) : c'est pour ça que les templates ne s'affichaient pas. Ils ne s'appliqueront qu'une fois mergés dans `master`.

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
- [x] **Error Prone + NullAway** (ADR-023), sur `develop` (#86) : contrôles par défaut d'Error Prone, NullAway en erreur sur `src/main` (`AnnotatedPackages=com.mk.contractservice`, le `@NullMarked` du package racine ne couvrant pas les sous-packages), désactivé sur les tests, entités JPA exclues. `--add-exports` du compilateur dans `.mvn/jvm.config`.
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

## Types de contrats (entraînement, ADR-024)

Non exigé par `docs/sujet.txt`, livré par incréments. Chaque incrément a sa migration et met à jour l'ADR-024. Le métier n'est pas joignable : les règles retenues sont des hypothèses, à écrire comme telles dans l'ADR.

- [x] **Incrément 1 : type et éligibilité.** Type obligatoire et immuable (`LAMAL`, `LAA`, `LPP`), `ContractType.isClosedTo(ClientKind)` vérifié par `Contract.of` : LAMal réservée aux personnes, LAA et LPP aux entreprises. `type` dans l'API et les deux imports, `422 contractTypeNotAllowed` / rejet `TYPE_NOT_ALLOWED`, migration `V2.0.0.10`.
- [x] **Incrément 2 : pas de LAMal qui se chevauchent pour une même personne** (`feat/contract-types-increment-2`, non commité).
  - Règle retenue : deux LAMal d'une même personne ne se chevauchent pas, la nouvelle étant comparée aux seules LAMal actives (fin exclusive, ADR-004). « Une seule LAMal active » est écartée : un contrat à début futur est actif, une LAMal 2028 serait donc refusée tant que celle de 2027 court, alors qu'elle lui succède sans chevauchement.
  - `ContractCoverage` (domaine) porte la règle, `ContractOverlapPolicy` (service de domaine) charge en une requête les LAMal de tous les clients d'un lot. `409 contractOverlap` à la création, rejet `OVERLAPPING_CONTRACT` / `contractOverlap` dans les imports, y compris entre lignes d'un même import.
  - Concurrence : `ContractOverlapPolicy` verrouille les clients (`FOR UPDATE … ORDER BY id`) avant de lire leurs LAMal ; le verrou de chunk de l'import par fichier passe de `FOR SHARE` à `FOR UPDATE` trié. Contrainte d'exclusion PostgreSQL envisagée puis écartée (ADR-024). Test avec deux vraies transactions sur deux threads.
  - Clôture d'un contrat et suppression d'un client : sans effet sur la règle (une fin plus tôt ne crée pas de chevauchement).
- [ ] **Incrément 3 : franchise LAMal.**
  - Hiérarchie `sealed` dans le domaine (`LamalContract` porte la franchise), une seule table : colonne `franchise` nullable, `CHECK` liée au type, lectures sans jointure.
  - Value Object validé contre les paliers légaux : adulte 300, 500, 1000, 1500, 2000, 2500 CHF ; enfant de 0 à 600 CHF par paliers de 100.
  - L'âge vient de la date de naissance du client, transmise par le use case à `Contract.of` (comme `ClientKind`, sans dépendance à l'agrégat client).
  - Changement de franchise : à étudier (en pratique au 1er janvier), méthode `changeFranchise` avec sa règle de date.
- [ ] **Incrément 4 : droit d'option des frontaliers.**
  - Notion de résidence sur le client (Suisse ou pays frontalier) : migration sur les tables client et API client.
  - Un frontalier peut choisir l'assurance-maladie de son pays de résidence au lieu de la LAMal, dans un délai après le début de son emploi en Suisse. Règle détaillée à définir avant de modéliser.
  - Créer l'agrégat `AssuranceMaladie` (table `lamal`, une racine par personne avec sa `version`) qui porte le droit d'option, et y déplacer la règle de non-chevauchement : verrouillage optimiste forcé sur la racine à la place du verrou `FOR UPDATE` sur le client (ADR-024, « Suite prévue »). Au plus tard ici ; à reconsidérer dès l'incrément 3 si la franchise ajoute des règles propres à la couverture.

## Pour aller plus loin (entraînement)

Sujets non exigés par `docs/sujet.txt` ni justifiés à l'échelle actuelle (une dizaine de conseillers). Ils servent à s'exercer : chaque ADR le dit explicitement plutôt que d'inventer un besoin.

- [x] **Import en masse de contrats** (ADR-021) : `POST /v2/contracts/imports`, synchrone, JSON, tout ou rien avec rapport par ligne, 1000 lignes au plus. JDBC batching mesuré (`batch_size=50` : 20 lots au lieu de 1000 exécutions, environ 10 fois plus rapide ; `flush`/`clear` et `order_inserts` sans intérêt ici).
- [x] **Import asynchrone de gros volumes** (ADR-022) : `POST /v2/contracts/import-jobs`, fichier CSV, import partiel, Spring Batch en chunks, worker `@Scheduled` avec `SKIP LOCKED`, bail et reprise après panne sans doublon (`JobOperator.recover`), remise en file à l'arrêt et sur erreur transitoire, raison d'échec exposée, verrou `FOR SHARE` sur les clients de chaque lot. Ajouts : `Idempotency-Key` à la soumission, auteur et liste des jobs, annulation qui ferme les contrats du job (un job `FAILED` n'est pas relancé : il est annulé, puis un fichier corrigé est soumis), export CSV des rejets avec la ligne d'origine.
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
