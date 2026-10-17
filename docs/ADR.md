# Architecture Decision Records — contract-service

Registre des décisions d'architecture et d'infrastructure du service de gestion des clients et contrats d'assurance.
Contexte métier : application pour une assurance suisse (type Vaudoise), utilisée par des conseillers, clients en Suisse et frontaliers.

Format de chaque décision : **Contexte → Décision → Conséquences → Statut**.

| Statut | Signification |
|---|---|
| Acceptée | Appliquée sur `develop` |
| Acceptée (branche) | Implémentée et poussée, en attente de merge sur `develop` |
| Proposée | Décidée, pas encore implémentée |

## Sommaire

**Architecture**
- [ADR-001 — Vertical Slice + Clean Architecture](#adr-001--vertical-slice--clean-architecture)
- [ADR-002 — DDD tactique : agrégats, Value Objects, « always valid »](#adr-002--ddd-tactique--agrégats-value-objects--always-valid-)
- [ADR-003 — Le domaine reçoit le temps, il ne le lit jamais](#adr-003--le-domaine-reçoit-le-temps-il-ne-le-lit-jamais)
- [ADR-004 — Définition d'un contrat actif](#adr-004--définition-dun-contrat-actif)
- [ADR-005 — `lastModified` est une donnée du domaine](#adr-005--lastmodified-est-une-donnée-du-domaine)

**Persistance et données**
- [ADR-006 — PostgreSQL + Flyway, migrations immuables](#adr-006--postgresql--flyway-migrations-immuables)
- [ADR-007 — Modèle client à 3 tables (héritage JOINED)](#adr-007--modèle-client-à-3-tables-héritage-joined)
- [ADR-008 — Unicité : seul `company_identifier` est unique](#adr-008--unicité--seul-company_identifier-est-unique)
- [ADR-009 — Soft delete explicite des clients](#adr-009--soft-delete-explicite-des-clients)
- [ADR-010 — Pas de cache sur la somme des contrats actifs](#adr-010--pas-de-cache-sur-la-somme-des-contrats-actifs)
- [ADR-011 — Optimistic locking](#adr-011--optimistic-locking)
- [ADR-012 — Identifiants UUID v7](#adr-012--identifiants-uuid-v7)
- [ADR-013 — Fuseau horaire : `LocalDateTime` en `Europe/Zurich`](#adr-013--fuseau-horaire--localdatetime-en-europezurich)

**Qualité et exécution**
- [ADR-014 — Stratégie de tests](#adr-014--stratégie-de-tests)
- [ADR-015 — Image Docker et exécution](#adr-015--image-docker-et-exécution)

---

## ADR-001 — Vertical Slice + Clean Architecture

**Contexte.** Le service expose une dizaine de cas d'usage (créer un client, modifier un coût, lister les contrats actifs…). Une architecture en couches classique (`controller/service/repository`) disperse chaque cas d'usage dans des services fourre-tout. On veut à la fois isoler chaque cas d'usage et protéger le domaine des frameworks.

**Clean ou Onion ?** Les deux décrivent la même règle : les dépendances pointent vers le centre, le domaine ne connaît rien de l'extérieur. Onion (Palermo) met l'accent sur le modèle de domaine au centre ; Clean (Martin) nomme explicitement la couche des cas d'usage. Notre code ayant une couche de cas d'usage explicite (`features/`) et des ports dans le domaine (`@RepositoryPort`), le terme le plus juste est **Clean Architecture, avec des ports & adapters** (influence hexagonale). On n'utilise pas « Onion » dans la documentation pour éviter d'empiler les étiquettes.

**Décision.**

```
com.mk.contractservice
├── domain/            Cœur : agrégats, Value Objects, domain services, ports (interfaces de repository), exceptions
├── features/          Cas d'usage, un par fichier (vertical slice)
├── controllers/       Adapters entrants : REST, DTO, mapping, gestion des erreurs HTTP
└── infrastructure/    Adapters sortants : JPA, configuration (Clock, beans du domaine)
```

- **Un cas d'usage = une interface** dans `features/` qui contient sa `Command`/`Query` (record), sa méthode `execute` et son `Handler` (`@UseCase`, `@Transactional` explicite). Exemple : `CreateContract`, `PatchContractCost`.
- **Un endpoint = un controller** (`CreateClientController`, `DeleteClientController`…), sans logique métier.
- **Sens des dépendances** : `controllers → features → domain ← infrastructure`. Le domaine définit les ports, l'infrastructure les implémente.
- Les annotations `@UseCase`, `@DomainService`, `@RepositoryPort` sont définies dans `domain/shared` sans dépendance Spring ; `DomainBeanConfiguration` (infrastructure) les enregistre par component scan.
- Les règles sont vérifiées par **ArchUnit** (`ArchitectureTest`) : le domaine ne dépend ni de l'infrastructure, ni des features, ni des controllers, ni de Spring.

**Compromis documentés.**
- Le domaine utilise `Page`/`Pageable` de Spring Data (abstractions stables ; les recréer n'apporterait rien à cette échelle).
- Le domaine utilise Lombok et JSpecify (annotations de compilation, sans impact à l'exécution).
- Les handlers utilisent `@Transactional` de Spring : la frontière transactionnelle est une décision de cas d'usage, visible là où elle est prise.
- `controllers/` est un package de premier niveau et non un sous-package de `infrastructure/` : c'est un choix de lisibilité, les deux restent des adapters.

**Conséquences.**
- Chaque cas d'usage se lit, se teste et se supprime en un seul fichier.
- Ajouter un cas d'usage ne modifie aucun service existant.
- Changer de persistance (ex. JPA → jOOQ) ne touche que `infrastructure/` ; on l'a vérifié en déplaçant `lastModified` et en ajoutant le soft delete sans changer le contrat des ports côté features.
- Coût : un peu plus de fichiers qu'une architecture en couches.

**Statut.** Acceptée.

---

## ADR-002 — DDD tactique : agrégats, Value Objects, « always valid »

**Contexte.** Les règles métier (email valide, coût strictement positif, fin après début, champs immuables) doivent être impossibles à contourner.

**Décision.**
- **Agrégats** : `Client` (hiérarchie scellée `sealed Client permits Person, Company`) et `Contract`. Un contrat référence son client par identifiant, pas par objet.
- **Value Objects immuables et auto-validants** : `ClientName`, `ClientEmail`, `ClientPhoneNumber` (normalisé en E.164 via libphonenumber), `PersonBirthDate`, `CompanyIdentifier`, `ContractCost`, `ContractPeriod`. Implémentés en classes Lombok `@Value` à constructeur privé (un record ne permet pas de masquer son constructeur canonique).
- **Entrées contrôlées** :
  - `of(...)` valide et normalise une donnée venant de l'extérieur ;
  - `reconstituteFromDatabase(...)` reconstruit sans re-valider une donnée déjà fiable ;
  - les factories (`PersonFactory`, `CompanyFactory`, `ContractFactory`) exposent `createFromCommand(...)` et `buildFromDatabase(...)`.
- **Mutations par méthodes d'intention** qui renvoient une nouvelle instance : `changeName`, `changeEmail`, `changePhone`, `changeCoreFields`, `changeCost`. `birthDate` et `companyIdentifier` n'ont pas de méthode de modification.
- **Méthodes de comparaison métier** : `hasEmail`, `hasPhone`, `hasName`, `ContractCost.isSameAmountAs` (insensible à l'échelle : `100.0` = `100.00`). Une modification à valeur identique est un no-op : la règle vit dans l'agrégat, la méthode `change*` renvoie `this`. Les handlers ne sauvegardent que si l'objet renvoyé n'est pas l'instance chargée (`updated != loaded`).
- **Domain services réservés aux vraies règles métier** : un domain service porte une règle qu'aucun agrégat ne peut vérifier seul (`CompanyIdentifierUniqueness` : l'unicité demande de connaître les autres sociétés). « Charger ou lever 404 » et « vérifier l'existence » sont de l'accès aux données : ce sont des méthodes `default` du port (`ClientRepository.getById`, `ensureExists`), qui gardent la règle « introuvable → `ClientNotFoundException` » dans le domaine sans la dupliquer dans chaque adapter. `ClientService`, `ClientValidationService` et `ContractService`, qui ne portaient aucune règle, ont été supprimés.
- Les entités JPA ne sortent jamais de l'infrastructure ; l'API n'expose que des DTO.

**Conséquences.**
- Aucun objet du domaine ne peut exister dans un état invalide.
- Les Value Objects sont testés via les factories des entités et via l'API ; un test dédié n'existe que pour un VO partagé ou porteur d'une logique non observable autrement.
- Les builders de `Person` et `Company` sont publics, et celui de `Contract` l'est aussi sur `develop`. Ils sont privés dans la cible (déjà fait pour `Contract` sur la branche `lastModified`), voir TODO.

**Statut.** Acceptée.

---

## ADR-003 — Le domaine reçoit le temps, il ne le lit jamais

**Contexte.** Le domaine appelait `LocalDateTime.now()` à 7 endroits (activité d'un contrat, date de début par défaut, date de naissance future, date de l'événement de suppression…). Ces appels n'étaient ni cohérents entre eux ni testables.

**Décision.**
- Un **seul bean `java.time.Clock`** (`ClockConfiguration`). `Clock` vient du JDK : on ne l'emballe pas dans un port `TimeProvider`, ce serait une indirection sans valeur.
- Les **use cases, controllers et advices** injectent la `Clock` et passent l'heure au domaine : `Contract.isActiveAt(now)`, `changeCost(cost, now)`, `ContractPeriod.of(start, end, now)`, `PersonBirthDate.of(date, today)`, `ContractSearchCriteria.activeForClient(id, activeAt)`.
- **Pas d'horloge statique globale** (`DomainClock.set(...)`) : dépendance cachée, incompatible avec des tests parallèles.
- Un même use case utilise **un seul `now`** pour toutes ses décisions (ex. suppression : `deleted_at` = date de fermeture des contrats).

**Conséquences.**
- Le domaine est déterministe et testable sans base de données (`Clock.fixed` dans les tests unitaires, `MutableClock` dans les tests d'acceptance qui font avancer le temps).
- Les champs d'audit purement techniques (`created_at`, `last_modified` du client) utilisent encore l'horloge de Hibernate. L'écart avec la `Clock` est de quelques millisecondes en production, et rien ne compare ces champs. Passage à l'audit Spring Data branché sur la `Clock` : voir TODO.

**Statut.** Acceptée.

---

## ADR-004 — Définition d'un contrat actif

**Contexte.** Le sujet définit un contrat actif par « date courante < date de fin ».

**Décision.** Un contrat est actif à l'instant `t` si `endDate IS NULL OR t < endDate`. La date de fin est **exclusive**. Un contrat dont la date de début est dans le futur est **actif** : « actif » signifie « non expiré », pas « en cours ». On n'ajoute pas de condition `startDate <= t` sans exigence métier explicite.

**Conséquences.**
- La règle est portée par `ContractPeriod.isEffectiveAt(t)` côté domaine et par `ContractSpecifications.isActive(t)` côté requêtes : les deux doivent rester alignées.
- Un contrat devient inactif sans aucune écriture en base, simplement parce que le temps passe (voir ADR-010).
- Seuls les contrats actifs sont modifiables (`ExpiredContractException` sinon).

**Statut.** Acceptée.

---

## ADR-005 — `lastModified` est une donnée du domaine

**Contexte.** Le sujet impose : « mettre à jour le coût met à jour la date de mise à jour », date interne non exposée par l'API mais utilisable comme filtre. Elle était posée par `@UpdateTimestamp` (Hibernate), et à la main dans la fermeture en masse : deux sources de vérité.

**Décision.**
- `Contract` porte `lastModified`, posé à la création (`Contract.of(..., now)`) et par `changeCost(cost, now)`.
- Un changement de coût vers le même montant ne modifie ni le contrat ni `lastModified`.
- `@UpdateTimestamp` est retiré de `ContractJpaEntity` ; le mapper lit et écrit le champ explicitement.
- `ContractResponse` n'expose pas `lastModified`.

**Conséquences.**
- C'est le domaine qui décide de ce qui compte comme une modification, et non la détection des changements par Hibernate. Le filtre « modifié depuis » est exact.
- La fermeture en masse (suppression d'un client) renseigne `lastModified` avec la date de fermeture, cohérente avec la règle.

**Statut.** Acceptée (branche `feat/contract-last-modified-in-domain`).

---

## ADR-006 — PostgreSQL + Flyway, migrations immuables

**Contexte.** Les données doivent survivre à un redémarrage ; le schéma doit évoluer de façon traçable.

**Décision.**
- PostgreSQL 18, schéma dédié `contracts`. Hibernate en `ddl-auto: validate` : il vérifie le schéma, il ne le crée jamais. `open-in-view: false`.
- Toute évolution passe par une **migration Flyway SQL numérotée**. Une migration appliquée **n'est jamais modifiée** (Flyway contrôle un checksum ; réécrire une migration casse toute base existante). On ajoute un nouveau fichier.
- Numérotation alignée sur la version de l'application : `V2.0.0.x` tant que la 2.0.0 n'est pas livrée.
- Restructuration en plusieurs étapes (*expand/contract*) : créer, migrer les données, supprimer l'ancien.
- Les scripts ne créent pas la base, seulement le schéma et ses objets.

**Conséquences.**
- Migrations existantes : `V1.0.0.0` à `V1.0.0.3` (schéma, index), `V2.0.0.0` (optimistic locking, audit), `V2.0.0.1` (suppression de l'unicité email/téléphone), `V2.0.0.2` (soft delete, branche).
- Renommer une migration déjà appliquée oblige à recréer les bases qui l'ont vue (cas rencontré avec la base de test réutilisée).

**Statut.** Acceptée.

---

## ADR-007 — Modèle client à 3 tables (héritage JOINED)

**Contexte.** Deux types de clients partagent nom, email et téléphone ; chacun a un seul champ propre (`birth_date`, `company_identifier`). Options étudiées : une table (`SINGLE_TABLE` + `CHECK`), deux tables sans parent (`TABLE_PER_CLASS`), trois tables (`JOINED`).

**Décision.** On garde **trois tables** `client` / `person` / `company` (héritage `JOINED`) :
- les données des deux types ne sont pas mélangées ;
- `NOT NULL` et `UNIQUE` s'expriment naturellement sur les sous-tables ;
- une seule FK `contract → client`, quel que soit le type ;
- la structure peut accueillir des sous-types plus riches (SIRET, siège, représentants légaux).

Deux tables sans parent ont été écartées : la FK de `contract` aurait dû pointer vers deux tables (deux colonnes nullables + `CHECK`), et la recherche par id sans type aurait nécessité un `UNION`.

**Conséquences.**
- Coût : une jointure sur clé primaire en lecture et deux `INSERT` en création, négligeables face à l'aller-retour réseau.
- La cohérence entre `client.type` et la sous-table utilisée est garantie par la couche de persistance, pas par la base. Une FK composite `(id, type)` a été envisagée puis écartée : elle impose une colonne technique constante dans `person` et `company`. À réévaluer si plusieurs applications écrivent un jour dans cette base.

**Statut.** Acceptée.

---

## ADR-008 — Unicité : seul `company_identifier` est unique

**Contexte.** L'email et le téléphone étaient uniques. Or un freelance enregistré comme personne et comme société, un foyer, ou un groupe de sociétés partagent légitimement leurs coordonnées. Ces clients ne se connectent pas : l'email n'est pas un identifiant de connexion.

**Décision.**
- Suppression de l'unicité de l'email et du téléphone (migration `V2.0.0.1`, exceptions et handlers 409 supprimés).
- `company_identifier` reste unique, **y compris après suppression de la société** (ADR-009) : il identifie l'entité légale.
- Le sujet ne demande qu'une validation de format ; aucune unicité n'était exigée.

**Conséquences.**
- L'API ne renvoie plus `409 emailAlreadyExists` / `phoneAlreadyExists`. Changement de comportement compatible (une requête valide hier l'est toujours), à signaler dans les release notes ; pas de version majeure.
- Une détection de doublons non bloquante (avertissement au conseiller) reste possible plus tard.

**Statut.** Acceptée.

---

## ADR-009 — Soft delete explicite des clients

**Contexte.** La suppression était physique, avec `ON DELETE SET NULL` sur `contract.client_id`. La fermeture des contrats ne fonctionnait que grâce à l'ordre d'exécution, et l'historique des contrats perdait son client. Pour un assureur, l'historique doit être conservé.

**Décision.**
- Colonne `client.deleted_at` (migration `V2.0.0.2`), uniquement sur la table parente.
- Filtrage par `@SQLRestriction("deleted_at IS NULL")` sur `ClientJpaEntity` : un client supprimé renvoie 404 partout (lecture, liste et somme des contrats, création de contrat, nouvelle suppression).
- Port `ClientRepository.delete(id, deletedAt)` : le domaine exprime « supprimer à tel instant », l'infrastructure choisit de conserver la ligne.
- Le use case utilise le même `now` pour `deleted_at` et pour la fermeture des contrats (`end_date = deleted_at`).
- `DeleteClient` appelle directement les deux ports (`ClientRepository.delete`, puis `ContractRepository.closeAllActiveByClientId`), dans la même transaction. Un événement de domaine (`ClientDeletedEvent` écouté en `BEFORE_COMMIT`) a été retiré : il cachait la règle « supprimer un client ferme ses contrats » sans découpler réellement (même transaction, même instant, appel synchrone). La frontière entre agrégats est garantie dans le domaine par ArchUnit (`domain.client` et `domain.contract` ne dépendent pas l'un de l'autre) ; la coordination appartient au cas d'usage. Un événement redeviendra pertinent si les contrats sortent dans un autre service : il sera alors publié après le commit, via un outbox.
- FK `contract → client` en `ON DELETE RESTRICT`.
- La vérification d'unicité de `company_identifier` passe par une requête native qui inclut les sociétés supprimées.
- `@SoftDelete(TIMESTAMP)` de Hibernate a été écarté : il date la suppression lui-même, sans passer par notre `Clock`.

**Conséquences.**
- Les données personnelles d'un client supprimé restent en base : une anonymisation RGPD après la durée légale de conservation est nécessaire (TODO).
- `contract.client_id` reste nullable : d'anciennes suppressions physiques ont pu laisser des `NULL`. À vérifier avant de passer en `NOT NULL`.
- Les tests qui nettoient la base doivent supprimer les contrats avant les clients.
- **Contrat annulé.** Un contrat actif fermé par la suppression de son client n'est pas expiré : il est **annulé**, qu'il ait déjà commencé ou non.
  - Un contrat dont la date de début est dans le futur est actif (ADR-004) : il est fermé à `deleted_at` comme les autres, ce qui donne `end_date <= start_date`.
  - L'invariant « fin après début » de `ContractPeriod` n'est donc garanti qu'à la création (`ContractPeriod.of`). Il n'est **pas** vrai pour toutes les lignes en base : `ContractPeriod.reconstituteFromDatabase` ne doit jamais revalider la période, et aucune contrainte `CHECK (end_date > start_date)` ne doit être ajoutée, sinon ces contrats deviennent illisibles ou la suppression d'un client échoue. 
    - On part ici de l'idée qu'on est dans un contexte always-valid et qu'on ne peut pas se retrouver avec un contrat qui est censé être actif mais qui est invalide. Le domaine ne doit pas lever d'exception sur un contrat annulé, il doit juste le considérer comme inactif.
  - L'état « annulé » n'est pas modélisé : pas de statut, pas de règle dédiée. Aucun traitement n'est prévu sur un contrat annulé. Il est déjà exclu de tout traitement parce qu'il est inactif : absent de la liste et de la somme, non modifiable (`ExpiredContractException`).
  - Il reste lisible par `GET /contracts/{id}`, comme tout contrat inactif.
  - Une éventuelle purge (batch SQL) identifie les contrats annulés par `contract.end_date = client.deleted_at` ; ceux annulés avant d'avoir commencé vérifient en plus `end_date <= start_date`.

**Statut.** Acceptée (branche `feat/client-soft-delete`).

---

## ADR-010 — Pas de cache sur la somme des contrats actifs

**Contexte.** La somme était mise en cache par client (Caffeine) et vidée uniquement lors d'une écriture. Or un contrat devient inactif sans aucune écriture (ADR-004) : la somme mise en cache restait fausse.

**Décision.** Suppression du cache et des dépendances `spring-boot-starter-cache` / `caffeine`. La somme est toujours calculée en base, avec une agrégation couverte par l'index partiel `ix_contract_sum_performance`. Un cache à expiration variable (prochaine `endDate`) a été écarté : complexe, local à chaque instance, et pour un gain de quelques millisecondes.

**Conséquences.**
- Résultat toujours exact, quel que soit le nombre d'instances.
- Test d'acceptance `ContractTotalOverTimeAcceptanceTest` : il avance une `MutableClock` au-delà de la date de fin d'un contrat et vérifie que celui-ci sort de la somme.

**Statut.** Acceptée (merge #71).

---

## ADR-011 — Optimistic locking

**Contexte.** `@Version` existe sur `client` et `contract` (migration `V2.0.0.0`), et un conflit est traduit en `409 optimisticLockFailure` par `BaseControllerAdvice`. Mais `save()` recharge l'entité avant d'y recopier les valeurs du domaine : seuls les conflits entre deux transactions qui se chevauchent sont détectés, pas une modification basée sur une lecture périmée faite lors d'une requête HTTP précédente.

**Décision.**
1. **Correctif (fait)** : la fermeture en masse des contrats incrémente `version`. Sans cela, une modification du coût lancée avant la suppression du client pouvait rouvrir le contrat (`end_date` remis à `null`). Test : `ContractClosureConcurrencyAcceptanceTest`.
2. **Différée** : protection entre deux requêtes HTTP par `ETag` / `If-Match`. Entre deux requêtes HTTP, on garde « le dernier gagne ».

**Pourquoi différer l'étape 2.**
- Il y a au plus une dizaine de conseillers, qui gèrent chacun leur portefeuille. Deux modifications concurrentes du même client ou du même contrat, entre la lecture et l'envoi d'un formulaire, sont rares.
- Le seul cas grave, rouvrir un contrat fermé, est couvert par l'étape 1. Le domaine rejette déjà toute modification d'un contrat expiré.
- Ce qu'on peut encore perdre se rattrape : un nom, un email ou un téléphone écrasé. Pour un coût, la dernière décision est en général celle qu'on veut garder.
- `docs/sujet.txt` n'exige rien sur la concurrence.
- Le coût serait élevé :
  - un breaking change de l'API, puisque `If-Match` devient obligatoire ;
  - la version à faire transiter dans les ports, les commandes, les handlers et les controllers ;
  - la reprise de tous les tests d'acceptance `PUT`/`PATCH`.

**Atténuation sans code.** Le front utilise `PATCH` plutôt que `PUT` : `PUT` réécrit aussi les champs que l'utilisateur n'a pas touchés, alors que `PATCH` n'écrit que les champs envoyés. Une perte n'est alors possible que si deux personnes modifient le même champ.

**Signaux pour reprendre l'étape 2.**
- Les clients modifient eux-mêmes leurs données (self-service).
- Des intégrations ou des imports automatiques écrivent en parallèle des conseillers.
- Le nombre d'utilisateurs augmente fortement, ou les portefeuilles deviennent partagés.
- Un premier incident réel de modification perdue.

**Pistes de conception pour la reprise.**
- `GET` unitaire renvoie `ETag: "<version>"`. L'ETag doit être fort : `If-Match` utilise la comparaison forte, un ETag `W/` ne correspondrait jamais.
- `PUT`/`PATCH` exigent `If-Match` :
  - `428 Precondition Required` si l'en-tête manque ;
  - `412 Precondition Failed` si la version a changé ;
  - `*` refusé, car il contournerait le verrou.
- La version est un détail de persistance : la garder hors des agrégats, par exemple dans une enveloppe `Versioned<T>` renvoyée par les ports. Elle doit être lue dans la même requête que les données.
- Exposer `ETag` dans la configuration CORS (`exposedHeaders`), sinon un navigateur ne peut pas le lire.
- L'ETag ne sélectionne pas une version : il empêche d'écrire sur la base d'une lecture périmée, et le client doit relire avant de réessayer.

**Conséquences.**
- Un conflit entre deux transactions qui se chevauchent reste détecté (`409 optimisticLockFailure`).
- Le test `UpdateClientAcceptanceTest` « last one wins » documente le comportement voulu.

**Statut.** Étape 1 acceptée (branche `bugfix/contract-closure-lost-update`). Étape 2 différée.

---

## ADR-012 — Identifiants UUID v7

**Contexte.** Les identifiants sont des UUID v4 aléatoires, générés par Hibernate (`GenerationType.UUID`). La valeur par défaut `gen_random_uuid()` des colonnes n'est utilisée que par les `INSERT` SQL manuels. Des UUID aléatoires dispersent les insertions dans les index B-tree.

**Décision.** Passer aux **UUID v7** (triés par date de création) :
- côté application : `@UuidGenerator(style = VERSION_7)` (Hibernate 7) ;
- côté base : valeur par défaut `uuidv7()` (natif PostgreSQL 18), pour que l'application et la base génèrent le même type d'identifiant.

**Conséquences.**
- Index plus compacts et insertions plus rapides à volume élevé.
- Les identifiants existants (v4) restent valides, aucune migration de données.
- La date de création devient lisible dans l'identifiant exposé par l'API : acceptable ici.

**Statut.** Acceptée.

---

## ADR-013 — Fuseau horaire : `LocalDateTime` en `Europe/Zurich`

**Contexte.** Les utilisateurs sont en Suisse ; les frontaliers (France, Allemagne, Italie, Autriche) sont dans le même fuseau. Le code manipule des `LocalDateTime`, stockés en `TIMESTAMPTZ` et interprétés dans le fuseau de la JVM. Or l'image Docker (Debian slim) ne fixe aucun fuseau, donc tourne en UTC : les heures saisies seraient décalées d'1 à 2 heures selon la saison.

**Décision.**
- Garder `LocalDateTime` (un seul fuseau métier).
- Fixer le fuseau **explicitement** : `Clock.system(ZoneId.of("Europe/Zurich"))` via une propriété de configuration, `hibernate.jdbc.time_zone=Europe/Zurich`, et `TZ=Europe/Zurich` dans l'image.
- `birthDate` reste un `LocalDate` (date civile).
- Si l'application s'ouvre à des utilisateurs d'autres fuseaux : passage à `Instant` dans le domaine, `OffsetDateTime` dans l'API, UTC en base.

**Conséquences.**
- Comportement identique en local, en CI et en production.
- Limite connue : lors du passage à l'heure d'hiver, l'heure de 02:00 à 03:00 existe deux fois et une `LocalDateTime` à cette heure est ambiguë. Acceptable pour des contrats d'assurance.

**Statut.** Proposée (prioritaire).

---

## ADR-014 — Stratégie de tests

**Décision.**
- **Tests unitaires** (Surefire, `mvn test`) : domaine et handlers, avec repositories mockés et `Clock.fixed`.
- **Tests d'acceptance** (Failsafe, `*AcceptanceTest`, `mvn verify`) : API REST de bout en bout avec RestAssured, sur un vrai PostgreSQL via **Testcontainers**.
- **Tests de concurrence** : deux transactions réelles (`TransactionTemplate` + `REQUIRES_NEW`) pour reproduire les courses.
- **ArchUnit** pour les règles de dépendance (ADR-001).
- **JaCoCo** avec seuil de couverture vérifié en CI.
- Un test de non-régression sur un bug doit **échouer sans le correctif** ; on le vérifie avant de livrer.

**Conséquences.**
- Le conteneur Testcontainers est réutilisé (`withReuse(true)`) pour la rapidité en local. Mais en passant d'une branche à l'autre, sa base peut contenir des migrations que la branche ne connaît pas, et Flyway ignore sans rien dire une migration « future ». Il faut alors supprimer le conteneur (TODO : base par branche, ou pas de réutilisation en CI).

**Statut.** Acceptée.

---

## ADR-015 — Image Docker et exécution

**Décision.**
- Build multi-étapes :
  1. dépendances Maven en cache ;
  2. build avec Azul Zulu JDK 27, extraction des couches Spring Boot, puis JRE minimal construit par `jdeps` + `jlink` (`jdk.crypto.ec` ajouté explicitement) ;
  3. exécution sur `debian:trixie-slim`.
- Exécution sous un utilisateur non root, healthcheck sur `/actuator/health/liveness`, options JVM adaptées aux conteneurs (`MaxRAMPercentage=75`), profil `prod`.
- `../docker-compose.yml` : PostgreSQL 18 avec healthcheck et volume persistant ; l'application dans le profil compose `full`.
- CI GitHub Actions : JDK 27, build, tests unitaires + couverture, puis job d'intégration Testcontainers.

**Conséquences.**
- Image réduite et surface d'attaque limitée.
- Le fuseau n'est pas fixé dans l'image (ADR-013).

**Statut.** Acceptée.
