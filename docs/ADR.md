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
- [ADR-016 — Mutation testing (PIT)](#adr-016--mutation-testing-pit)

**Sécurité**
- [ADR-017 — Authentification des conseillers par Keycloak](#adr-017--authentification-des-conseillers-par-keycloak)

**API**
- [ADR-018 — Pagination par offset, bornes portées par le use case](#adr-018--pagination-par-offset-bornes-portées-par-le-use-case)
- [ADR-019 — Export des contrats actifs : pagination par curseur sur l'identifiant](#adr-019--export-des-contrats-actifs--pagination-par-curseur-sur-lidentifiant)
- [ADR-020 — Stratégie de merge entre branches majeures et mineures](#adr-020--stratégie-de-merge-entre-branches-majeures-et-mineures)
- [ADR-021 — Import en masse de contrats : synchrone, tout ou rien](#adr-021--import-en-masse-de-contrats--synchrone-tout-ou-rien)
- [ADR-023 — Analyse statique : Error Prone et NullAway](#adr-023--analyse-statique--error-prone-et-nullaway)
- [ADR-022 — Import de gros volumes : fichier CSV traité en asynchrone, partiel](#adr-022--import-de-gros-volumes--fichier-csv-traité-en-asynchrone-partiel)

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
- **Accesseurs fluent** (`lombok.accessors.fluent = true` dans `lombok.config` à la racine) : `contract.id()`, `email.value()`, comme les records des DTO et des read models. Une seule convention de lecture dans le code applicatif. Les entités JPA gardent les accesseurs JavaBean (`lombok.config` de `infrastructure/persistence` : `fluent = false`) : Hibernate reconnaît `getId()` comme accesseur d'identifiant et le sert sur un proxy lazy sans l'initialiser, ce qu'il ne fait pas avec `id()`.
- **Entrées contrôlées** :
  - `of(...)` valide et normalise une donnée venant de l'extérieur ;
  - `reconstituteFromDatabase(...)` reconstruit sans re-valider une donnée déjà fiable ;
  - les factories (`PersonFactory`, `CompanyFactory`, `ContractFactory`) exposent `createFromCommand(...)` et `buildFromDatabase(...)`.
- **Mutations par méthodes d'intention** qui renvoient une nouvelle instance : `changeName`, `changeEmail`, `changePhone`, `changeCoreFields`, `changeCost`. `birthDate` et `companyIdentifier` n'ont pas de méthode de modification.
- **États exclusifs : une interface scellée, une variante par état.** Un objet qui est soit l'un, soit l'autre n'a pas de champs nullables à interpréter : `ContractImportTermination` est `Ended` ou `Failed(reason)`, `ContractImportJobLine` est `Imported(contractId)` ou `Rejected(reason, message)`. **Les variantes sont des records, par exception à la règle des Value Objects.** Elles ne normalisent rien et n'ont pas d'entrée « donnée fiable » distincte : leur constructeur compact valide chaque champ obligatoire, donc une instance créée hors des factories de l'interface reste valide. En échange, les lecteurs les déconstruisent dans un `switch` exhaustif (`case Failed(LocalDateTime _, ContractImportFailureReason reason)`). Cette exception ne vaut que pour une variante de type scellé sans normalisation. La persistance garde ses colonnes nullables : le mapper est le seul endroit qui traduit l'une en l'autre.
- **Méthodes de comparaison métier** : `hasEmail`, `hasPhone`, `hasName`, `ContractCost.isSameAmountAs` (insensible à l'échelle : `100.0` = `100.00`). Une modification à valeur identique est un no-op : la règle vit dans l'agrégat, la méthode `change*` renvoie `this`. Les handlers ne sauvegardent que si l'objet renvoyé diffère de l'objet chargé (`!updated.equals(loaded)`, égalité de valeur via `@EqualsAndHashCode`). Pas de comparaison de références : Error Prone (`ReferenceEquality`) la signale sur un type qui définit `equals`.
- **Domain services réservés aux vraies règles métier** : un domain service porte une règle qu'aucun agrégat ne peut vérifier seul (`CompanyIdentifierUniqueness` : l'unicité demande de connaître les autres sociétés). « Charger ou lever 404 » et « vérifier l'existence » sont de l'accès aux données : ce sont des méthodes `default` du port (`ClientRepository.getById`, `ensureExists`), qui gardent la règle « introuvable → `ClientNotFoundException` » dans le domaine sans la dupliquer dans chaque adapter. `ClientService`, `ClientValidationService` et `ContractService`, qui ne portaient aucune règle, ont été supprimés.
- Les entités JPA ne sortent jamais de l'infrastructure ; l'API n'expose que des DTO.
- **Une seule définition d'une valeur valide : le Value Object.** Les DTO ne valident que la syntaxe du contrat HTTP (présence, type, longueur maximale, nombre de chiffres) et ne redéfinissent ni les règles de format ni les règles métier. `@Email` a été retiré : ses règles différaient de `ClientEmail` (`a@b` passait le DTO puis échouait dans le domaine, `.a@x.ch` échouait dans le DTO avec un autre code d'erreur). Toute adresse au format invalide renvoie maintenant `EMAIL_INVALID`. Même raisonnement pour le `@Pattern` du téléphone (`PHONE_INVALID`), `@Positive` sur le coût (`invalidContractCost`) et `@PastOrPresent` sur la date de naissance (`BIRTH_DATE_INVALID`) ; ce dernier lisait de plus l'horloge système au lieu de la `Clock` (ADR-003). Un `PATCH` applique les mêmes contraintes de syntaxe qu'un `PUT`, et un champ obligatoire envoyé à `null` y est refusé (`422 validationError`) au lieu d'être ignoré.
- **Chaque point d'entrée a sa propre validation de syntaxe.** Elle dépend du format reçu : un point d'entrée JSON peut réutiliser un DTO JSON existant (l'import JSON de contrats, ADR-021, réutilise `ContractCreationRequest`), alors qu'un import par fichier a son propre validateur (ADR-022). Tous convergent vers les mêmes Value Objects, qui restent la dernière barrière.
- **Le modèle d'entrée d'un use case se valide lui-même.** Une `Query` ou une `Command` ne peut pas exister dans un état invalide pour son cas d'usage : son constructeur vérifie les règles applicatives qui ne relèvent d'aucun Value Object (par exemple les bornes de pagination, ADR-018). Le use case ne dépend pas d'un adaptateur pour se protéger : un batch ou un consommateur de messages se heurterait aux mêmes règles. L'adaptateur ne fait que convertir (types, présence, valeurs par défaut) et ne duplique pas ces règles. Ce ne sont pas des règles métier : elles restent dans `features`, pas dans `domain`.
- **Format de `ClientEmail`** : partie locale *dot-atom* RFC 5322 (64 caractères au plus, sans point en tête, en fin ni doublé, apostrophe et `+` acceptés), domaine à un ou plusieurs labels (63 caractères au plus, sans tiret en bordure), TLD alphabétique ou punycode, 254 caractères au total. Les formes entre guillemets, les adresses IP et les domaines IDN en Unicode sont refusés (le punycode est accepté). Pas de liste de TLD, qui vieillit mal. Le but est d'attraper les fautes de frappe sans jamais refuser une adresse réelle ; seul l'envoi d'un email prouverait qu'elle existe. Le message d'erreur ne reprend pas l'adresse (donnée personnelle, exposée dans le `detail` et les logs).

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
- Le domaine est déterministe et testable sans base de données (`Clock.fixed` dans les tests unitaires, `MutableInstantSource` dans les tests d'acceptance qui font avancer le temps).
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
- L'unicité de `company_identifier` est **insensible à la casse** : deux entités légales ne se distinguent pas par la casse de leur identifiant. `CompanyIdentifier` normalise la valeur en majuscules (`Locale.ROOT`), et la vérification d'unicité compare `upper(company_identifier)`.
- Pas de migration des données : les identifiants enregistrés avant la normalisation restent tels quels et sont renvoyés tels quels par l'API. La comparaison `upper(...)` les couvre quand même.
- Le sujet ne demande qu'une validation de format ; aucune unicité n'était exigée.

**Conséquences.**
- L'API ne renvoie plus `409 emailAlreadyExists` / `phoneAlreadyExists`. Changement de comportement compatible (une requête valide hier l'est toujours), à signaler dans les release notes ; pas de version majeure.
- Une détection de doublons non bloquante (avertissement au conseiller) reste possible plus tard.
- La contrainte `UNIQUE` de la base reste sensible à la casse. Elle ne protège donc pas contre deux créations simultanées qui ne diffèrent que par la casse, ni contre un doublon de casse déjà présent en base. Les doublons déjà présents ne sont pas corrigés automatiquement. Avec une dizaine de conseillers, ce risque est jugé acceptable.
- La vérification `upper(...)` n'utilise pas l'index unique : parcours séquentiel de `company`, négligeable à ce volume.

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
- Test d'acceptance `ContractTotalOverTimeAcceptanceTest` : il avance une `MutableInstantSource` au-delà de la date de fin d'un contrat et vérifie que celui-ci sort de la somme.

**Statut.** Acceptée (merge #71).

---

## ADR-011 — Optimistic locking

**Contexte.** `@Version` existe sur `client` et `contract` (migration `V2.0.0.0`), et un conflit est traduit en `409 optimisticLockFailure` par `BaseControllerAdvice`. Mais `save()` recharge l'entité avant d'y recopier les valeurs du domaine : sans autre mécanisme, seuls les conflits entre deux transactions qui se chevauchent sont détectés, pas une modification basée sur une lecture périmée faite lors d'une requête HTTP précédente.

**Décision.**
1. **Correctif (fait)** : la fermeture en masse des contrats incrémente `version`. Sans cela, une modification du coût lancée avant la suppression du client pouvait rouvrir le contrat (`end_date` remis à `null`). Test : `ContractClosureConcurrencyAcceptanceTest`.
2. **Protection entre deux requêtes HTTP par `ETag` / `If-Match` (faite, après avoir été différée)** :
   - seul le `GET` unitaire d'un client ou d'un contrat renvoie un ETag fort `"<version>"` ;
   - `PUT`/`PATCH` d'un client et `PATCH` du coût d'un contrat exigent `If-Match` :
     - `428 preconditionRequired` si l'en-tête manque ou vaut `*` (le joker contournerait le verrou) ;
     - `412 preconditionFailed` si la version a changé depuis la lecture, si l'ETag est faible (`If-Match` utilise la comparaison forte) ou s'il ne contient pas exactement un ETag ;
   - les réponses de création et de modification ne portent pas d'ETag : le client relit la ressource avant une nouvelle modification. Pour le `PUT`, c'est de toute façon imposé par RFC 9110 §9.3.4, qui interdit un validateur dans la réponse quand la représentation stockée diffère de celle reçue (on normalise l'email et le téléphone) ;
   - la suppression d'un client n'exige pas `If-Match` : la rejouer après une modification concurrente donne le résultat voulu ;
   - un `GET` avec `If-None-Match` égal à l'ETag courant renvoie `304` (comportement standard de Spring MVC dès que la réponse porte un ETag).
3. **Réessai côté serveur (fait : suppression d'un client uniquement)** : le handler est annoté `@Retryable` sur `OptimisticLockingFailureException` (3 nouveaux essais, `delay` 20 ms, `jitter` 10 ms), activé par `@EnableResilientMethods` (`ResilienceConfig`). Chaque essai ouvre une nouvelle transaction : il relit l'agrégat et revérifie les règles métier. Les endpoints protégés par `If-Match` ne sont pas réessayés.

**Conception de l'étape 2.**
- La version est un détail de persistance, pas une donnée métier : le domaine ne décide rien à partir d'elle (contrairement à `lastModified`, ADR-005). Elle reste hors des agrégats et hors des types renvoyés par les ports d'écriture : `getById` et `save` renvoient `Client` et `Contract`.
- **Lecture (CQRS)** : le `GET` passe par un read model, `ClientQueryPort.getDetails` (`PersonDetails` / `CompanyDetails`) et `ContractQueryPort.getDetails` (`ContractDetails`). Un read model n'est pas un agrégat : il peut porter la version, lue dans la même requête que les données. Les read models ne portent que des types simples. Le champ `active` de `ContractDetails` est calculé par l'adaptateur à l'instant fourni par le use case, en reconstituant `ContractPeriod` et en appelant `isEffectiveAt` : la règle de l'ADR-004 n'est pas dupliquée.
- **Écriture** : la commande porte la version attendue (`expectedVersion`) ; le handler appelle `ensureVersion(id, expectedVersion)` sur le port juste après le chargement, sur le modèle de `ensureExists`. Dans la même transaction, l'adaptateur relit l'entité depuis le contexte de persistance, donc la version comparée est celle des données chargées. Ordre des erreurs : `422 validationError` (syntaxe du DTO, ADR-002), `428` (`If-Match` absent), `404` (ressource absente), `412` (précondition), puis règles du domaine (`409 contractExpired`, `422`). Une précondition est évaluée avant d'appliquer le contenu (RFC 9110 §13.2.2).
- **Quand exiger une version attendue** : seulement quand la commande traduit la décision d'un utilisateur prise sur un état qu'il a lu dans une requête précédente (`PUT`/`PATCH`). Pas pour une création, ni pour une opération dont le résultat ne dépend pas de l'état lu (suppression d'un client), ni pour un traitement qui lit et écrit dans la même transaction (fermeture en masse, futurs jobs) : `@Version` suffit. Un import qui met à jour des données existantes demandera une règle métier, le système source n'ayant pas d'ETag.
- Le controller convertit l'en-tête (`EntityTags.requiredVersion`) et renvoie `428` sans appeler le use case quand il manque. La validation Bean Validation du corps a lieu avant : un corps invalide sans `If-Match` donne `422`.
- Une modification concurrente qui se glisse entre la vérification de la version et le `flush` lève `OptimisticLockingFailureException`. `BaseControllerAdvice` répond `412` quand la requête porte `If-Match`, `409 optimisticLockFailure` sinon. Il n'y a pas de modification perdue, et pas de `409` sur les endpoints protégés par `If-Match`.
- `ETag`, `Location` et `Content-Range` sont exposés en CORS (`exposedHeaders`), `If-Match` et `If-None-Match` autorisés (`allowedHeaders`).
- Limite assumée : `If-Match` avec une liste de plusieurs ETags donne `412` même si l'un d'eux correspond. Un client n'envoie que l'ETag qu'il a reçu.

**Première conception, abandonnée.** Les ports d'écriture renvoyaient une enveloppe `Versioned<Client>` / `Versioned<Contract>` depuis `getById` et `save`, et les réponses `POST`/`PATCH` portaient le nouvel ETag. Abandonnée avant merge : une préoccupation HTTP traversait les ports d'écriture et tous les handlers, `save` devait faire un `saveAndFlush` pour connaître la nouvelle version, et chaque nouvelle méta-donnée technique aurait de nouveau changé ces signatures. Séparer lecture et écriture garde le modèle d'écriture inchangé, au prix d'un `GET` avant chaque modification.

**Pourquoi l'étape 2 avait été différée (17/10).**
- Il y a au plus une dizaine de conseillers, qui gèrent chacun leur portefeuille. Deux modifications concurrentes du même client ou du même contrat, entre la lecture et l'envoi d'un formulaire, sont rares.
- Le seul cas grave, rouvrir un contrat fermé, est couvert par l'étape 1.
- Ce qu'on pouvait perdre se rattrape : un nom, un email ou un téléphone écrasé.
- `docs/sujet.txt` n'exige rien sur la concurrence.
- Le coût était élevé : breaking change, version à faire transiter dans les ports, les commandes, les handlers et les controllers, reprise des tests d'acceptance `PUT`/`PATCH`.

**Pourquoi la reprendre.** Les arguments ci-dessus restent vrais à l'échelle actuelle. L'étape 2 est reprise comme exercice, et parce qu'elle prépare les sujets prévus ensuite (`PROGRESS.md`, « Pour aller plus loin ») : l'import en masse fait écrire des automates en parallèle des conseillers, l'un des signaux qui justifiaient la reprise, et le handler connaît la version attendue, qu'il pourra transmettre aux événements d'une outbox.

**Effet sur l'étape 3.** Avant `If-Match`, le réessai appliquait « le dernier gagne » : le coût réessayé écrasait le coût concurrent, le `PATCH` d'un client gardait aussi la modification concurrente d'un autre champ, et le `PUT` n'était pas réessayé parce qu'il aurait écrasé sans bruit les champs non touchés. Avec `If-Match`, un réessai ne peut plus réussir : la version attendue vient de l'en-tête et la version en base ne fait qu'augmenter, il aboutit donc toujours à `412` (ou `404` si la ressource a été supprimée). Il a d'abord été gardé pour traduire le conflit en `412`, puis retiré au profit de l'advice : `maxRetries`, le délai et le jitter laissaient croire qu'insister pouvait aider, pour une seule différence, une suppression concurrente répond `412` au lieu de `404` (le `GET` suivant donne `404`). Pour la suppression d'un client, sans `If-Match`, il garde son rôle d'origine : rejouer la suppression.

**Contraintes du réessai.**
- Il doit envelopper la transaction : un réessai à l'intérieur relirait l'entité périmée du contexte de persistance. Spring place l'intercepteur `@Retryable` avant les intercepteurs existants (`beforeExistingAdvisors`), ce qui permet de garder `@Retryable` et `@Transactional` sur le même handler. `ClientConflictAcceptanceTest` vérifie cet ordre : il injecte une écriture concurrente pendant la suppression et attend deux essais.
- Sans `@EnableResilientMethods`, `@Retryable` est ignoré.
- Un cas d'usage réessayable ne doit pas être appelé depuis une transaction existante : il la rejoindrait (`REQUIRED`) et le réessai serait sans effet.
- Ses effets de bord doivent pouvoir être rejoués. Aujourd'hui, il n'écrit qu'en base ; une publication de message devra se faire après le commit.

**Conséquences.**
- Breaking change de l'API : un client qui modifie sans `If-Match` reçoit `428`. Les collections (`api-collections/`) capturent l'ETag et l'envoient automatiquement.
- Une modification basée sur une lecture périmée, entre deux requêtes HTTP ou entre deux transactions qui se chevauchent, donne `412`. Un contrat fermé par la suppression de son client depuis la lecture donne aussi `412` (sa version a été incrémentée par l'étape 1), et il reste fermé.
- `409 optimisticLockFailure` ne subsiste que pour la suppression d'un client, après épuisement des essais.
- Tests : `OptimisticLockingAcceptanceTest` (contrat HTTP), `ClientConflictAcceptanceTest` et `ContractCostConflictAcceptanceTest` (conflits entre transactions), et les tests des handlers.

**Statut.** Étapes 1 et 3 acceptées. Étape 2 acceptée (branche `feat/etag-if-match`).

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
- Une seule propriété porte le fuseau métier : `app.time-zone=Europe/Zurich`, liée et validée au démarrage par `TimeZoneProperties` (`@ConfigurationProperties`), injectée là où le fuseau est nécessaire.
- Horloge : l'instant et le fuseau sont séparés. `ClockConfiguration` expose une `InstantSource` (`InstantSource.system()`) et un `Clock` qui lui applique le fuseau métier (`instantSource.withZone(timeZoneProperties.timeZone())`). Le « maintenant » métier ne dépend plus du fuseau de la JVM. Les tests remplacent uniquement l'`InstantSource` : le fuseau reste celui de l'application.
- Persistance : `BusinessZoneDateTimeConverter` (`@Converter(autoApply = true)`) convertit chaque `LocalDateTime` en `OffsetDateTime` dans `app.time-zone` avant écriture, et inversement à la lecture. L'instant stocké en `TIMESTAMPTZ` ne dépend plus du fuseau de la JVM.
- `hibernate.jdbc.time_zone` est **écarté** : avec des `LocalDateTime`, Hibernate 7 passe toujours par `ZoneId.systemDefault()`. Le scénario de relecture des dates restait rouge avec ce réglage.
- Image Docker : `ENV TZ=Europe/Zurich`, pour ce qui dépend encore du fuseau de la JVM (`@CreationTimestamp`/`@UpdateTimestamp`, logs).
- `birthDate` reste un `LocalDate` (date civile).
- Si l'application s'ouvre à des utilisateurs d'autres fuseaux : passage à `Instant` dans le domaine, `OffsetDateTime` dans l'API, UTC en base.

**Conséquences.**
- Comportement identique en local, en CI et en production. Vérifié par deux scénarios métier, sur une suite d'acceptance exécutée avec une JVM en UTC (`-Duser.timezone=UTC`) : un contrat qui se termine à minuit heure suisse ne compte plus dans le total à 00:30 heure suisse (`ContractTotalOverTimeAcceptanceTest`) ; les dates saisies sont relues identiques après un redéploiement du serveur dans un autre fuseau (`ReadContractAcceptanceTest`).
- Un fuseau absent ou inconnu empêche le démarrage (`@Validated`, `@NotNull`, conversion en `ZoneId`).
- Limite connue : `created_at`/`last_modified` générés par Hibernate (`@CreationTimestamp`, `@UpdateTimestamp`) utilisent l'horloge de la JVM. Ils sont justes avec `TZ=Europe/Zurich` et décalés sur une JVM dans un autre fuseau. Ces champs sont techniques et ne sont pas exposés (ADR-005).
- Limite connue : lors du passage à l'heure d'hiver, l'heure de 02:00 à 03:00 existe deux fois et une `LocalDateTime` à cette heure est ambiguë. Acceptable pour des contrats d'assurance.

**Statut.** Acceptée.

---

## ADR-014 — Stratégie de tests

**Décision.**
- **Tests unitaires** (Surefire, `mvn test`) : domaine et handlers, avec repositories mockés et `Clock.fixed`.
- **Tests d'acceptance** (Failsafe, `*AcceptanceTest`, `mvn verify`) : API REST de bout en bout avec RestAssured, sur un vrai PostgreSQL via **Testcontainers**. Réservés aux fonctionnalités vues par un utilisateur de l'API. La JVM tourne en UTC (`-Duser.timezone=UTC`), comme un hôte sans fuseau configuré ; le temps se contrôle en remplaçant uniquement l'`InstantSource` par une `MutableInstantSource`, jamais le `Clock`.
- **Tests de concurrence** : deux transactions réelles (`TransactionTemplate` + `REQUIRES_NEW`) pour reproduire les courses.
- **ArchUnit** pour les règles de dépendance (ADR-001).
- **JaCoCo** en rapport de couverture, sans seuil : une ligne couverte n'est pas une ligne vérifiée. Le critère bloquant sur la qualité des tests est le score de mutation PIT (ADR-016).
- Un test de non-régression sur un bug doit **échouer sans le correctif** ; on le vérifie avant de livrer.

**Conséquences.**
- Le conteneur Testcontainers est réutilisé (`withReuse(true)`) pour la rapidité en local. Mais en passant d'une branche à l'autre, sa base peut contenir des migrations que la branche ne connaît pas, et Flyway ignore sans rien dire une migration « future ». Il faut alors supprimer le conteneur (TODO : base par branche, ou pas de réutilisation en CI).

**Statut.** Acceptée.

---

## ADR-015 — Image Docker et exécution

**Décision.**
- Build multi-étapes :
  1. dépendances Maven en cache ;
  2. build avec Azul Zulu JDK 27, extraction des couches Spring Boot, puis JRE minimal construit par `jdeps` + `jlink` (`jdk.crypto.ec`, `jdk.jfr` et `jdk.jcmd` ajoutés explicitement) ;
  3. exécution sur `debian:trixie-slim`.
- Exécution sous un utilisateur non root, healthcheck sur `/actuator/health/liveness`, options JVM adaptées aux conteneurs (`MaxRAMPercentage=75`), profil `prod`.
- `../docker-compose.yml` : PostgreSQL 18 avec healthcheck et volume persistant ; l'application dans le profil compose `full`.
- CI GitHub Actions : JDK 27, build, tests unitaires + couverture, puis job d'intégration Testcontainers.

**Conséquences.**
- Image réduite et surface d'attaque limitée.
- JFR, `jcmd` et `jps` restent disponibles pour le diagnostic en production. JFR enregistre en continu dès le démarrage, avec une rétention limitée à 6 h et 500 Mo : les données les plus anciennes sont supprimées, et un `JFR.dump` récupère l'historique sans redémarrage.
- Le fuseau est fixé dans l'image : `TZ=Europe/Zurich` (ADR-013).

**Statut.** Acceptée.

---

## ADR-016 — Mutation testing (PIT)

**Contexte.** JaCoCo mesure le code *exécuté* par les tests, pas le code *vérifié*. Les règles métier du domaine (bornes de dates exclusives, montants insensibles à l'échelle, no-op des `change*`, longueurs maximales) sont exactement celles qu'un test peut traverser sans les vérifier.

**Décision.**
- **PIT** (`pitest-maven` + `pitest-junit5-plugin`) dans un profil Maven `mutation` : `./mvnw test -Pmutation`.
- Périmètre : `domain` et `features` uniquement, avec les tests unitaires (Surefire). Les tests d'acceptance (Testcontainers) sont exclus : un démarrage PostgreSQL par mutant est trop lent pour un gain faible. `controllers` et `infrastructure` sont exclus : leurs mutants sont surtout du mapping ou de la configuration.
- Aucune classe du périmètre n'est exclue de la mutation : exclure du code pour améliorer le score masquerait ce que les tests unitaires ne vérifient pas.
- Mutateurs `DEFAULTS`. Deux seuils, le build du profil échoue en dessous :
  - `testStrengthThreshold = 95` : mutants tués parmi les mutants **couverts** par les tests unitaires. C'est le seuil qui garde les règles métier ;
  - `mutationThreshold = 90` : mutants tués parmi **tous** les mutants. Il tolère la plomberie couverte uniquement par l'acceptance (handlers de pure délégation), qui reste visible en `NO_COVERAGE` dans le rapport.
- On écrit des tests unitaires pour des comportements (agrégats, services de domaine, décisions d'orchestration des handlers), pas pour tuer des mutants. Un survivant sur une règle métier se tue par un test ; un mutant non couvert sur de la plomberie testée par l'acceptance ne justifie pas un test unitaire.
- Hors du `clean verify` standard (environ 1 minute de plus), exécuté dans un job CI dédié `mutation-tests` ; rapport dans `target/pit-reports/index.html`.
- Pas d'historique incrémental (`withHistory`) : avec PIT 1.30 il exige le plugin commercial Arcmutate.

**Conséquences.**
- Premier passage : 92 % de mutants tués. Les survivants ont révélé des trous réels : aucune vérification de la longueur maximale *acceptée* (nom 200, email 254), chemins nominaux de `CreateContract` et `CreateCompany` non testés en unitaire. Ils ont aussi révélé un « find or throw » écrit dans les handlers `GetContractById` et `PatchContractCost`, et des ports qui exposaient `findById`/`existsById` uniquement pour servir leurs propres méthodes par défaut `getById`/`ensureExists`. Les ports n'exposent plus que `getById` et `ensureExists`, implémentés par les adapters JPA qui lèvent l'exception du domaine.
- Deuxième passage : les survivants restants ont été traités.
  - `rawValue.isBlank()` dans `normalize` (`ClientName`, `ClientEmail`, `ClientPhoneNumber`, `CompanyIdentifier`) n'était pas un mutant équivalent : sans ce test, une valeur blanche reste rejetée avec la même exception, mais avec un message trompeur (« Client name too long » pour un nom vide, « Invalid email format » pour un email blanc). Le message est exposé dans le `detail` du `ProblemDetail`. Des tests vérifient désormais que le message mentionne la valeur blanche.
  - `ClientPhoneNumber.toString` n'était utilisé par aucun code de production : supprimé plutôt que testé.
- Score actuel : 99 % de mutants tués (191/193), test strength 100 %.
- Mutants non tués, acceptés : `NO_COVERAGE` sur `GetClientById.Handler` et `GetContractById.Handler` (« replaced return value with null »). Ces handlers délèguent à `getById` du port, qui lève l'exception métier si l'agrégat n'existe pas. Un test unitaire avec un port mocké ne vérifierait que le mock lui-même : il n'y a ni règle ni décision d'orchestration à vérifier. Le comportement (200 avec le bon agrégat, 404 sinon) est vérifié par les tests d'acceptance, que PIT exclut. Ces deux mutants sont donc attendus. Un nouveau `NO_COVERAGE` sur un handler qui orchestre (plusieurs ports, condition, appel à un service de domaine) doit, lui, être couvert par un test unitaire.

**Statut.** Acceptée.

---

## ADR-017 — Authentification des conseillers par Keycloak

**Contexte.** Le sujet décrit une API utilisée par des conseillers (« As a counselor, I want to create clients and contracts […] and manage them »). Aujourd'hui, n'importe qui peut appeler l'API, alors qu'elle expose des données personnelles (nom, email, téléphone, date de naissance) et des montants de contrats. La branche `feat/add-auth-keycloak` pose une première version :
- Resource server OAuth2 JWT (Spring Security), avec Keycloak 26 et sa base PostgreSQL dans `docker-compose.yml`.
- `KeycloakJwtConverter` transforme en `ROLE_*` les rôles du realm et les rôles de **tous** les clients Keycloak (`resource_access`).
- `@PreAuthorize` sur chaque contrôleur, via `ApplicationRoles` (dans `infrastructure.security`). Les écritures sont réservées à `ADMIN`, les lectures ouvertes à `ADMIN` et `COUNSELOR`.
- Dans les tests, `TestSecurityConfig` remplace la chaîne de sécurité par `permitAll`.

Cette version pose six problèmes :
1. **Les conseillers ne peuvent pas faire leur travail.** Réserver les écritures à `ADMIN` contredit le sujet : créer, modifier et supprimer des clients et des contrats est justement le travail du conseiller. Avec une dizaine de conseillers et aucun besoin d'administration identifié, un rôle `ADMIN` n'apporte rien.
2. **Les rôles sont mélangés entre clients Keycloak.** Les rôles de n'importe quel client Keycloak du realm deviennent des rôles applicatifs. Un rôle `admin` défini pour une autre application donnerait donc `ROLE_ADMIN` ici.
3. **L'audience n'est pas vérifiée.** Un jeton émis pour une autre application du même realm est accepté.
4. **Les règles sont dispersées.** Une annotation par endpoint : un endpoint ajouté sans annotation reste accessible à tout utilisateur authentifié. De plus, un refus levé par `@PreAuthorize` part en `AccessDeniedException` depuis l'appel du contrôleur : il passe par les `@ControllerAdvice`, où le handler générique `@ExceptionHandler(Exception.class)` de `BaseControllerAdvice` le transforme en `500`, sauf si on ajoute un handler dédié.
5. **Aucun test d'autorisation.** Avec `permitAll` en test, aucun test ne vérifie qui a le droit de faire quoi : une règle supprimée par erreur passerait inaperçue.
6. **Le reste n'est pas prêt.** Les dépendances `jjwt` ne sont pas utilisées (le resource server s'appuie sur Nimbus). Le realm n'est pas provisionné, il faut le créer à la main. Le CORS est ouvert à toutes les origines (`*`). Les erreurs 401/403 n'ont pas de corps `ProblemDetail`.

**Décision.**
- **Un seul rôle : `COUNSELOR`.** C'est un rôle du realm `contract-service`. Toutes les routes `/v2/**` l'exigent, en lecture comme en écriture. Pas de rôle `ADMIN` tant qu'aucun besoin réel n'apparaît : en ajouter un plus tard ne casse rien.

  | Endpoint | Accès |
  |---|---|
  | `POST /v2/clients` | `COUNSELOR` |
  | `GET`, `PUT`, `PATCH`, `DELETE /v2/clients/{id}` | `COUNSELOR` |
  | `POST /v2/contracts` | `COUNSELOR` |
  | `GET /v2/contracts?clientId=`, `GET /v2/contracts/{id}`, `GET /v2/contracts/total?clientId=` | `COUNSELOR` |
  | `PATCH /v2/contracts/{id}/cost` | `COUNSELOR` |
  | `GET /v2/contracts/export` (ADR-019) | `COUNSELOR` |
  | `POST /v2/contracts/imports` (ADR-021) | `COUNSELOR` |
  | `/actuator/health/**`, `/actuator/info` | public |
  | `/swagger-ui/**`, `/v3/api-docs/**` | public (la documentation ne contient aucune donnée) |
  | toute autre route | refusée (`denyAll`) |

- **Les règles sont dans la chaîne de filtres, en un seul endroit.** `SecurityConfig` les déclare par URL (`requestMatchers("/v2/**").hasRole("COUNSELOR")`, puis `anyRequest().denyAll()`). Les `@PreAuthorize`, `ApplicationRoles` et `@EnableMethodSecurity` sont retirés. Un nouvel endpoint sous `/v2` est protégé sans rien ajouter ; une route oubliée hors de `/v2` est refusée. Le refus a lieu dans le filtre, avant le `DispatcherServlet`, donc les `@ControllerAdvice` ne le voient jamais. `@PreAuthorize` reviendra le jour où des règles différeront d'un endpoint à l'autre (par exemple, un endpoint ouvert à un futur rôle `CLIENT`).
- **Jeton accepté.**
  - Signature vérifiée avec les clés publiées par Keycloak (`jwk-set-uri`).
  - Émetteur vérifié : `iss` doit être le realm (`issuer-uri`).
  - Audience vérifiée : `aud` doit contenir `contract-service` (`spring.security.oauth2.resourceserver.jwt.audiences`).
  - Rôles lus uniquement dans `realm_access.roles`. `resource_access` est ignoré.
- **Erreurs au format `ProblemDetail`**, comme le reste de l'API :
  - sans jeton, jeton expiré ou signature invalide : `401`, code `unauthenticated`, en-tête `WWW-Authenticate: Bearer` ;
  - jeton valide sans le rôle `COUNSELOR` : `403`, code `accessDenied`.

  Ces réponses sont produites par un `AuthenticationEntryPoint` et un `AccessDeniedHandler` dans `infrastructure.security`. Le `detail` ne révèle pas pourquoi le jeton a été refusé.
- **L'autorisation est vérifiée avant tout le reste** : `401`, puis `403`, puis validation (`422`), puis existence (`404`). Sans les droits, un appel n'apprend donc pas si une ressource existe.
- **Le domaine et les use cases ne savent rien de la sécurité.** Les rôles sont une règle d'accès à l'API, pas une règle métier. Aucun changement dans `domain` ni dans `features`.
- **Le realm est versionné.** Un export du realm `contract-service` (rôle `COUNSELOR`, client `contract-service`, utilisateurs de démo pour le développement uniquement) est importé au démarrage (`start-dev --import-realm`). Les mots de passe de démo ne servent qu'en local.
- **CORS** : origines autorisées lues dans une propriété (`app.cors.allowed-origins`), vide par défaut. Pas de `*`.
- **Dépendances** : `jjwt` est retiré, `spring-boot-starter-oauth2-resource-server` suffit.

**Tests.**
- `TestSecurityConfig` en `permitAll` est supprimé. Les tests d'acceptance passent par la vraie chaîne de sécurité.
- Pas de conteneur Keycloak en test. Un `JwtDecoder` de test vérifie des jetons signés par une clé RSA générée au démarrage des tests ; un helper émet des jetons avec l'émetteur, l'audience et les rôles voulus. Ce qui est testé, c'est notre configuration (règles, audience, rôles, erreurs), pas Keycloak.
- La spécification RestAssured par défaut envoie un jeton `COUNSELOR` : les scénarios existants ne changent pas.
- Une classe `SecurityAcceptanceTest` couvre :
  - sans jeton → `401 unauthenticated` ;
  - jeton expiré, mauvaise audience ou mauvais émetteur → `401` ;
  - jeton sans `COUNSELOR`, ou avec un rôle `counselor` venant de `resource_access` → `403 accessDenied` ;
  - avec `COUNSELOR` → `2xx` sur chaque endpoint `/v2` ;
  - `/actuator/health` sans jeton → `200` ;
  - route inconnue hors de `/v2` avec un jeton `COUNSELOR` → refusée ;
  - sans jeton et avec un corps invalide → `401`, pas `422`.

**Hors périmètre, évolutions prévues.**
- **Un client consulte ses propres contrats.** Le rôle seul ne suffit pas : il faut savoir à quel client correspond le compte. Cela demandera de relier le compte Keycloak au client (`sub` ↔ `client.id`, donc une migration), de transmettre l'identité de l'appelant dans la commande ou la requête du use case, et de vérifier dans le use case que le contrat lui appartient. Ce contrôle de propriété est une règle métier : il ira dans `features`/`domain`, pas dans la configuration de sécurité. Les règles par URL resteront valables pour les rôles.
- **Traçabilité** (quel conseiller a modifié quoi) : le `sub` du jeton est disponible, mais rien n'est stocké tant que le besoin n'est pas exprimé.
- **Rôle `ADMIN`** : seulement quand une action réservée apparaît (par exemple une purge RGPD).

**Conséquences.**
- Changement cassant pour les consommateurs de l'API : chaque appel `/v2` exige un jeton `Bearer`. À annoncer dans les release notes, avec la façon d'obtenir un jeton en local.
- Collections Bruno / Postman : authentification OAuth2 (password grant sur le realm local) au niveau de la collection.
- OpenAPI : `securityScheme` bearer JWT déclaré globalement.
- README : démarrer Keycloak, utilisateurs de démo, obtenir un jeton.
- La branche `feat/add-auth-keycloak` est à remettre sur `develop` et à aligner sur cette décision : retirer `ApplicationRoles` et les `@PreAuthorize`, limiter le converter aux rôles du realm, ajouter la vérification d'audience.

**Statut.** Acceptée.

---

## ADR-018 — Pagination par offset, bornes portées par le use case

**Contexte.** `GET /v2/contracts?clientId=` est paginé. La pagination passait par un `ValidatingPageableArgumentResolver` (sous-classe de `PageableHandlerMethodArgumentResolver`), enregistré par un `WebMvcConfigurer` avec `@EnableSpringDataWebSupport`. Il validait `page` et `size` à la main (`parseInt`, bornes lues dans `app.pagination`). Il transmettait aussi le paramètre `sort` du client à JPA sans contrôle :
- `?sort=lastModified,asc` triait sur un champ que l'API n'expose pas (ADR-005) ;
- `?sort=version` triait sur un champ technique ;
- `?sort=champInconnu` levait une `PropertyReferenceException`, transformée en `500`.

`VIA_DTO` ne servait à rien : le contrôleur renvoie déjà son propre DTO `PagedContractResponse`.

Une pagination par curseur a été écartée :
- le sujet n'exige pas de pagination ;
- la liste concerne les contrats actifs d'un seul client (quelques dizaines au plus), consultés par une dizaine de conseillers : le coût d'un `OFFSET` est négligeable ;
- la clé de tri `lastModified` change à chaque `changeCost` : un curseur sur une clé qui bouge ferait sauter des éléments, et demanderait un curseur composite `(lastModified, id)` ;
- `updatedSince` couvre déjà le besoin de synchronisation incrémentale.

**Décision.**
- **Les bornes et le tri appartiennent au use case**, pas au contrôleur (ADR-002 : le modèle d'entrée d'un use case se valide lui-même). Ils dépendent du cas d'usage (100 pour la liste, 1000 pour l'export, ADR-019), pas du canal :
  - `ListActiveContractsByClient.Query(clientId, updatedSince, page, size)` vérifie `page ≥ 0` et `1 ≤ size ≤ MAX_PAGE_SIZE` (100) via `PageBounds` (`features/shared`) ;
  - la `Query` construit le `PageRequest` avec le tri `lastModified` décroissant ; un appelant ne peut pas choisir le tri.
- Le contrôleur ne fait que convertir : `@RequestParam int page` (`0` par défaut) et `int size` (`20` par défaut), sans annotation de validation. Le paramètre `sort` est ignoré. La description OpenAPI reprend `ListActiveContractsByClient.MAX_PAGE_SIZE` : une seule source de vérité.
- Une borne non respectée lève une `InvalidPageRequestException` : `400`, code `invalidParameter`. C'est un paramètre de requête invalide, pas une règle métier violée : pas de `422`. Une valeur non numérique lève une `MethodArgumentTypeMismatchException` : `400`, code `invalidParameterType`, comme pour les autres paramètres.
- `PaginationConfig`, `ValidatingPageableArgumentResolver`, `PaginationProperties` (`app.pagination`) et `InvalidPaginationException` sont supprimés.

**Conséquences.**
- Tout appelant du use case (contrôleur, batch, consommateur de messages) se heurte aux mêmes bornes ; les tests unitaires de la `Query` les couvrent, les tests d'acceptation vérifient le `400`.
- OpenAPI n'expose plus `minimum`/`maximum` sur `page` et `size` : les bornes sont dans la description. `sort` n'est pas exposé.
- Une première version validait `page`/`size` par Bean Validation dans le contrôleur (`@PositiveOrZero`, `@Positive`, `@Max`, `HandlerMethodValidationException`) ; elle a été abandonnée parce que le use case dépendait alors de l'adaptateur pour se protéger.
- Changement de contrat (sans impact : la V2 n'est pas en production) : `sort` est ignoré, un `page`/`size` non numérique renvoie le code `invalidParameterType` au lieu de `invalidParameter`.
- Le domaine garde `Page`/`Pageable` (compromis de l'ADR-001) : le use case construit le `PageRequest`, le contrôleur ne manipule plus de `Pageable`.
- Un endpoint d'export de tous les contrats (gros volumes) pourrait justifier un curseur. Il fera l'objet d'une décision séparée (ADR-019).

**Statut.** Acceptée (branche).

---

## ADR-019 — Export des contrats actifs : pagination par curseur sur l'identifiant

**Contexte.** Exercice : un système tiers (entrepôt de données, archivage) doit récupérer tous les contrats actifs, tous clients confondus. Contrairement à la liste par client (ADR-018), le volume peut être grand, et donc :
- avec un `OFFSET`, PostgreSQL lit puis jette toutes les lignes qui précèdent la page, si bien que chaque page coûte plus cher que la précédente ;
- un `COUNT(*)` sur toute la table, à chaque page, ne sert à rien pour un export ;
- l'export dure longtemps, et les contrats changent pendant ce temps. Avec un `OFFSET`, une ligne qui change de rang entre deux pages est sautée ou exportée deux fois.

**Décision.**
- `GET /v2/contracts/export?cursor=&size=`, rôle `COUNSELOR` (couvert par `/v2/**`, ADR-017).
- **Périmètre** : les contrats actifs (ADR-004) de tous les clients, à l'instant de **chaque** requête. Aucun filtre tant que personne n'en a besoin.
- **Curseur sur `id` seul** (pagination *keyset*) : `WHERE <actif> AND id > :after ORDER BY id LIMIT size + 1`. L'`id` est unique et immuable, et l'index de la clé primaire sert à la fois au tri et au point de reprise. La clé `(lastModified, id)` est écartée : `lastModified` change à chaque `changeCost` (ADR-005). Une ligne modifiée pendant l'export passerait derrière le curseur et serait perdue, ou repasserait devant et serait exportée deux fois.
- **Curseur opaque** : le Base64 URL-safe des 16 octets de l'`id` du dernier contrat renvoyé. Le client le renvoie tel quel. Son format ne fait pas partie du contrat d'API : on pourra le changer (le signer, y ajouter une version) sans casser les clients. Un curseur qui ne se décode pas en UUID renvoie `400`, code `invalidCursor`.
- **Réponse** : `{ "items": [ContractResponse…], "nextCursor": "…" | null }`. On lit `size + 1` lignes : la ligne en trop indique qu'il existe une page suivante, sans `COUNT`. `nextCursor` vaut `null` sur la dernière page. La réponse n'expose pas `lastModified` (ADR-005).
- **Taille de page** : `1 ≤ size ≤ 1000` (`ExportActiveContracts.MAX_PAGE_SIZE`), vérifiée par `ExportActiveContracts.Query` via `PageBounds` comme dans l'ADR-018 (`400`, code `invalidParameter`). `100` par défaut, fixé par le contrôleur.
- **JSON paginé plutôt que streaming** (NDJSON, CSV). Un flux garde une transaction et une connexion ouvertes pendant tout l'export, et on ne peut pas le reprendre après une coupure. Avec un curseur, chaque page est une requête courte, et le client reprend au dernier curseur reçu.
- **Répartition des responsabilités** :
  - le controller (`ExportContractController`) encode et décode le curseur, qui est un détail du protocole HTTP ;
  - le use case (`ExportActiveContracts`) prend l'heure de la `Clock` (ADR-003) et décide s'il existe une page suivante ;
  - le port `ContractRepository.findActiveAfter(activeAt, afterId, limit)` renvoie une `List`, sans `Page` ni `Pageable` ;
  - l'adapter JPA utilise une `Specification` et `findBy(...).limit(...)`, ce qui évite la requête de comptage.

**Conséquences.**
- Coût constant par page, quelle que soit la profondeur de l'export. Aucune migration : l'index de la clé primaire suffit.
- Garanties vérifiées par `ExportContractsAcceptanceTest` :
  - chaque contrat actif est exporté une seule fois, et aucun contrat expiré ne l'est ;
  - un contrat dont le coût change pendant l'export n'est ni sauté ni répété ;
  - un contrat qui expire avant que sa page soit lue n'est pas exporté.
- L'export n'est **pas un instantané** : chaque page reflète l'état de la base au moment de la requête. Un contrat créé pendant l'export a un UUID v7 (ADR-012), plus grand que le curseur, et il apparaît donc dans une page suivante. Un contrat ancien en UUID v4 ne suit pas cet ordre : s'il est actif mais placé avant le curseur, il n'est pas exporté dans cette passe. Pour obtenir un instantané cohérent, il faudrait une transaction `REPEATABLE READ` sur tout l'export ou une table d'export dédiée : hors périmètre.
- L'ordre est celui de PostgreSQL sur le type `uuid` (octet par octet), et non celui de `UUID.compareTo` en Java (qui compare des `long` signés). Le tri et la comparaison au curseur se font tous les deux en base, donc ils restent cohérents. Il ne faut jamais trier ou filtrer ces identifiants en mémoire côté Java.
- Le filtre « actif » n'est pas couvert par l'index utilisé pour le tri. Si la plupart des contrats étaient expirés, chaque page parcourrait beaucoup de lignes inactives. Un index partiel ou `(end_date, id)` serait alors à mesurer : pas avant d'avoir des volumes réels.

**Statut.** Acceptée (branche `feat/contract-export`, exercice).

---

## ADR-020 — Stratégie de merge entre branches majeures et mineures

**Contexte.** La PR #65 (`develop` → `release/2.0.0`) a été mergée en *squash* : `c01ffd4` contient tout le code de `develop`, mais c'est un commit neuf, sans lien avec les 76 commits d'origine. Le back-merge `d0b79da` (« Merge branch 'release/2.0.0' into develop ») avait été fait depuis l'ancien sommet de la release (`5a060e9`), pas depuis `c01ffd4`. Pour git, la release contenait donc un commit inconnu de `develop`, et `develop` 76 commits absents de la release. Après le merge d'une nouvelle feature sur `develop`, `git rebase release/2.0.0` a rejoué ces 76 commits un par un sur une base qui contenait déjà leur résultat final : chacun entrait en conflit avec sa propre version définitive. Un merge classique aurait donné 13 fichiers en conflit, alors que le contenu des deux branches ne différait que par les commits récents de `develop`.

**Décision.**
- **Deux types de branches :**
  - **Branche majeure** : `master`, `develop` et `release/x.y.z`. Elle dure longtemps, elle est partagée, d'autres branches partent d'elle ou y reviennent, et son historique publié n'est jamais réécrit.
    - `master` : le code livré en production. Chaque release mergée y est taguée `vx.y.z`.
    - `develop` : la branche d'intégration de la prochaine version. Les features y arrivent.
    - `release/x.y.z` : la stabilisation d'une version, créée depuis `develop`. Elle ne reçoit que des corrections, puis elle est mergée dans `master` et dans `develop`.
  - **Branche mineure** : `feat/<topic>` et `bugfix/<topic>`. Elle dure peu de temps, elle porte un seul sujet, elle part d'une branche majeure et y revient par une seule PR, puis elle est supprimée. Personne ne repart de ses commits.
    - `feat/<topic>` : part de `develop` et revient dans `develop`.
    - `bugfix/<topic>` : part de la branche majeure à corriger (`develop`, `release/x.y.z` ou `master`) et revient dans cette même branche.
- **PR d'une branche mineure vers une branche majeure** : **squash obligatoire**. Le sujet devient un seul commit au format Conventional Commits, et les commits de travail intermédiaires disparaissent. Comme la branche mineure est supprimée ensuite, perdre le lien avec ses commits n'a aucune conséquence.
- **PR entre branches majeures** : **merge commit uniquement**, jamais de squash ni de rebase-merge. Le merge commit garde les commits d'origine comme ancêtres, et git sait qu'ils sont intégrés.
- **Un bugfix est squashé dans une seule branche majeure**, la plus ancienne qu'il concerne. Il rejoint ensuite les autres par les merges entre branches majeures, jamais par une deuxième PR du même bugfix :
  - un correctif de `release/x.y.z` arrive dans `master` et `develop` quand la release y est mergée ;
  - un correctif de `master` arrive dans `develop` par un back-merge `master` → `develop`.
- **Jamais de rebase d'une branche partagée.** Rebaser `develop` sur une release réécrit son historique publié et rejoue tous ses commits. On intègre une autre branche avec `git merge`.
- **Back-merge** (`release/*` ou `master` → `develop`) depuis le sommet final de la branche, une fois la PR de release mergée.
- **Rattrapage après un squash** : vérifier que la branche squashée n'apporte rien qui manque à la cible (`git diff`), puis `git merge -s ours <branche>`. Le commit de merge garde le contenu de la cible à l'identique (`git diff HEAD~1 HEAD` vide) et enregistre la branche comme intégrée. Appliqué sur `develop` pour `release/2.0.0`.
- **Avant un merge entre branches majeures**, le simuler avec `git merge-tree --write-tree <cible> <branche>` : il liste les conflits sans toucher à l'arbre de travail.

**Conséquences.**
- Les merges suivants entre `develop`, `release/*` et `master` ne rejouent plus l'historique déjà intégré.
- L'historique de `develop`, `release/*` et `master` garde des commits de merge : c'est le prix d'une ascendance que git sait suivre.
- `-s ours` ignore tout le contenu de la branche fusionnée. Il ne faut l'utiliser qu'après avoir vérifié qu'elle n'apporte aucun correctif propre, sinon ce correctif est perdu sans conflit ni avertissement.
- La règle repose sur la discipline. Un *ruleset* GitHub fixe les méthodes de merge autorisées par branche cible, pas par branche source : `master`, `release/*` et `develop` reçoivent à la fois des squash (branches mineures) et des merge commits (branches majeures), donc il ne peut pas imposer la bonne méthode. Il peut seulement interdire le rebase-merge sur les trois branches majeures. Le rappel figure dans la checklist du template de PR.

**Statut.** Acceptée.

---

## ADR-021 — Import en masse de contrats : synchrone, tout ou rien

**Contexte.** Exercice : ni `docs/sujet.txt` ni l'échelle actuelle (une dizaine de conseillers) n'exigent un import en masse. Il sert à s'entraîner sur la sémantique d'échec partiel et sur le JDBC batching. Il n'y a pas de besoin métier à invoquer.

Un contrat n'a pas de clé naturelle : deux contrats identiques pour un même client sont légitimes. Le serveur ne peut donc pas reconnaître une ligne déjà importée, et cette contrainte décide de la sémantique d'échec.

**Décision.**
- `POST /v2/contracts/imports`, rôle `COUNSELOR` (couvert par `/v2/**`, ADR-017). Corps JSON : `{ "contracts": [ { clientId, startDate, endDate, costAmount }, … ] }`.
- **Tout ou rien.** Toutes les lignes sont validées avant la première écriture, dans une seule transaction. Si une ligne est rejetée, rien n'est créé et la réponse liste tous les rejets : le conseiller corrige et renvoie l'import complet, sans risque de doublon.
- **Réponses :**
  - `201` : `{ "importedCount": n, "contractIds": [...] }`, les identifiants étant dans l'ordre des lignes. Pas de `Location` : aucune ressource d'import n'existe ;
  - `422 contractImportRejected` : propriété `rejections`, avec pour chaque problème `index` (à partir de 0), `clientId`, `code` et `message`. `index` est le seul repère d'une ligne, car un import peut contenir plusieurs contrats du même client ; `clientId` permet de regrouper les lignes d'un même client manquant. Les codes sont ceux de `POST /v2/contracts` (`invalidContractCost`, `invalidContractPeriod`, `clientNotFound`) ;
  - `422 invalidImportSize` : aucune ligne, ou plus de `ImportContracts.MAX_LINES` (1000) ;
  - `422 validationError` : champ manquant ou mal typé, nommé `contracts[n].champ`.
- **Syntaxe, puis règles.** Comme ailleurs (ADR-002), le DTO vérifie la syntaxe et s'arrête là : une erreur de syntaxe sur une ligne empêche d'évaluer les règles métier de toutes les autres. Une valeur mal typée interrompt le parsing JSON : seule la première est signalée.
- **Un seul rejet par ligne, le client d'abord.** Une ligne dont le client est inconnu est rejetée sans construire le contrat : le conseiller sait qu'il doit d'abord créer ce client, et le contenu de la ligne sera évalué au renvoi. L'ordre diffère de `CreateContract`, qui valide le contrat avant de chercher le client : à l'échelle d'un import, signaler d'abord le client manquant donne la correction la plus utile. De même, `ContractFactory` s'arrête à la première règle violée : une ligne dont la période et le coût sont invalides ne signale que la période. Une correction peut donc faire apparaître un nouveau rejet sur la même ligne. Cet ordre (client, période, coût) est porté par `ContractImportLineEvaluator` et vérifié par `ContractImportLineEvaluatorTest` : l'import par fichier (ADR-022) l'applique à l'identique, après son contrôle de format.
- **L'import ne crée pas les clients manquants.** La ligne ne porte que `clientId`, alors que créer un client demande son type, son nom, son email, son téléphone et sa date de naissance ou son identifiant d'entreprise, que seul le conseiller peut fournir. De plus, l'identifiant est généré par le serveur : après `POST /v2/clients`, le conseiller remplace `clientId` dans la ligne par l'identifiant renvoyé. Enfin, `clientNotFound` couvre aussi un client supprimé (ADR-009), qu'il ne faut pas recréer sans décision du conseiller.
- **« Always valid » conservé** : chaque contrat passe par `ContractFactory.createFromCommand`. `ContractImportLineEvaluator` (`features/shared`), partagé avec l'import par fichier (ADR-022), vérifie le client puis traduit les exceptions du domaine en rejets (`ContractImportRejectionReason`), sans ajouter de règle métier. Il porte une seule politique applicative : l'ordre des contrôles, le client d'abord (voir plus haut).
- **Codes de l'API hors du domaine.** `ContractImportRejectionReason` et `ContractImportFailureReason` ne portent que le motif. `ContractImportCodes` (`controllers/contract/shared`) les traduit en codes (`invalidContractCost`…), dans un `switch` exhaustif : un nouveau motif ne compile pas tant qu'il n'a pas de code. Les rejets de la réponse `422` sont un record (`ContractImportLineRejectionResponse`), documenté dans OpenAPI, et non une `Map`.
- **Taille** : la limite est portée par `ImportContracts.Command` (ADR-002). 1000 lignes suffisent largement à cette échelle et gardent une transaction courte (environ 30 ms d'écriture, mesuré ci-dessous).
- **Existence des clients sans N+1** : le port `ClientRepository.findExistingIds(Set<UUID>)` fait une seule requête `IN`. `@SQLRestriction` s'y applique : un client supprimé est absent du résultat, donc rejeté comme inconnu (ADR-009).
- **Une seule `now`** pour tout l'import (ADR-003) : date de début par défaut et `lastModified`.
- **Pas de version attendue** (ADR-011) : l'import ne crée que de nouveaux contrats.
- **Écriture** : `ContractRepository.saveAll(List<Contract>)` délègue à `save` pour chaque contrat : même règle « id nul → création, sinon fusion dans l'existant », sans la dupliquer ni recopier en silence un contrat existant. Chaque `save` fait un `persist` ; les `INSERT` sont différés et partent en lots au commit.

**JDBC batching : ce qui est retenu, et pourquoi.** `hibernate.jdbc.batch_size: 50` dans `application.yml` ; l'ADR-022
l'a depuis porté à 1000 et a activé `reWriteBatchedInserts`, et les mesures ci-dessous restent celles de cette première
valeur. C'est possible parce que l'UUID v7 est généré côté application (ADR-012) : avec un `IDENTITY`, Hibernate devrait
exécuter chaque `INSERT` pour lire l'identifiant, ce qui désactive le batching.

Mesures : base PostgreSQL 18 en conteneur local (Docker, poste Windows), un seul client, médiane de 5 exécutions après 2 exécutions de chauffe. Ce sont des ordres de grandeur : la latence réseau réelle accentuerait l'écart.

| Écriture | 1000 lignes | 10 000 lignes | Lots JDBC pour 1000 lignes |
|---|---|---|---|
| `persist`, sans batching | 296 ms | 2946 ms | 1000 exécutions |
| `persist`, `batch_size=50` | **27 ms** | **276 ms** | 20 lots de 50 |
| `persist`, `batch_size=50`, `flush`/`clear` tous les 50 | 27 ms | 277 ms | non compté |
| `saveAll` Spring Data (`merge`), `batch_size=50` | 31 ms | 266 ms | non compté |

- **Gain : un facteur 10 environ.** Le nombre de lots vient des traces `org.hibernate.orm.jdbc.batch` (20 fois `Executing JDBC batch (50 / 50)`).
- **`flush`/`clear` écarté.** Aucun gain mesuré, même à 10 000 lignes, et `clear` détacherait toute autre entité du contexte de persistance. À réévaluer seulement si la limite augmente nettement.
- **`order_inserts` écarté.** Il regroupe les `INSERT` de plusieurs tables ; l'import n'en écrit qu'une.
- **`@Version` sans valeur initiale.** Spring Data considère comme nouvelle une entité dont le `@Version` de type objet est `null`. Les entités initialisaient ce champ à `0L` (commit `c307374`, sans justification) : `save` passait donc par `merge`, qui renvoyait une copie et laissait l'entité passée en argument sans identifiant. Comme son identifiant était `null`, Hibernate n'exécutait aucun `SELECT`, et le surcoût restait dans le bruit de mesure (dernière ligne du tableau, mesurée avant ce changement). L'initialisation est retirée de `ClientJpaEntity` et de `ContractJpaEntity` : Hibernate pose lui-même la version à `0` au `persist`, et `save` comme `saveAll` font désormais un `persist` pour une nouvelle entité. Le `DEFAULT 0` de la base reste pour les `INSERT` SQL manuels. Les lignes `persist` du tableau décrivent donc le chemin actuel.
- **`reWriteBatchedInserts` de pgjdbc non retenu.** Il réécrit les lots en `INSERT` multi-valeurs ; non mesuré, et rien
  ne le justifie à ce volume. Activé ensuite pour les gros volumes (ADR-022), ce réglage s'applique aussi à cet
  endpoint.
- **Effet global.** `batch_size` s'applique à toutes les écritures. Elles n'envoient qu'une ligne par entité et par table : un lot d'une ligne. La détection des conflits `@Version` reste vérifiée par les tests de concurrence existants.

**Test technique, et sa justification.** `ContractBatchInsertAcceptanceTest` vérifie qu'un import de 1000 lignes prépare au plus une requête d'existence et une requête par lot (`Statistics.getPrepareStatementCount`). Un utilisateur de l'API ne peut pas observer cette garantie, et le réglage peut être perdu sans qu'aucun scénario métier échoue. Le test a échoué à 1001 requêtes avant l'ajout de `batch_size`. Limite : Hibernate réutilise le même `PreparedStatement` entre les lots (2 requêtes préparées en tout), donc cette statistique prouve l'absence d'une requête par ligne, sans compter les lots. Mesurer les lots demanderait `datasource-proxy`, une dépendance jugée inutile pour une seule assertion.

**Options écartées.**
- **Partiel dans une transaction** (lignes valides insérées, `207` ou `200` avec rapport). Si l'appelant renvoie tout le fichier au lieu des seules lignes rejetées, les lignes déjà acceptées sont créées une seconde fois. De plus, `207` vient de WebDAV et sert souvent mal à distinguer un succès d'un échec.
- **Commit par lots.** Une panne en cours d'import laisse un état partiel que l'appelant ne connaît pas : il faudrait une `Idempotency-Key` ou un identifiant de ligne. Cela n'aurait d'intérêt que pour de gros volumes.
- **Asynchrone** (`202 Accepted` + ressource d'import). Envisagé puis écarté : 1000 lignes tiennent en quelques dizaines
  de millisecondes, et il faudrait une table (migration), un worker et une reprise après crash. La piste la plus simple
  si le besoin apparaît : un `@Scheduled` qui traite chaque import dans une seule transaction avec
  `FOR UPDATE SKIP LOCKED`, contrats et statut étant commités ensemble. Repris pour les gros volumes par l'ADR-022, qui
  écarte la transaction unique.
- **CSV.** Plus proche d'un fichier métier, mais il ajoute parsing, encodage et séparateurs, sans rapport avec le sujet
  de l'exercice. Le sujet demande du JSON. Un import CSV aurait son propre validateur (ADR-002) et pourrait réutiliser
  le même use case. L'ADR-022 ajoute ce format pour l'import asynchrone, sans toucher à cet endpoint.

**Conséquences.**
- Aucune migration.
- Un import rejeté se renvoie sans risque, et un conseiller voit toutes les erreurs de règles métier en une seule réponse.
- Limite : un import **accepté** dont la réponse se perd (coupure réseau), puis renvoyé, crée des doublons. Le risque est le même que pour `POST /v2/contracts`, avec plus de lignes. Le traitement prévu est `Idempotency-Key` (`PROGRESS.md`).
- Limite : tout le corps est lu en mémoire et validé avant le contrôle du nombre de lignes. Acceptable pour des utilisateurs authentifiés, à une dizaine de conseillers. Une API ouverte demanderait une limite de taille du corps.
- Limite : un client supprimé entre le contrôle d'existence et le commit garde les contrats importés, actifs. Cette course existe déjà dans `CreateContract` ; un verrou sur les clients ne se justifie pas à cette échelle.
- `ContractControllerAdvice` nomme désormais le chemin complet d'un champ mal typé (`contracts[1].costAmount`). Pour les corps à un seul niveau, le message ne change pas.

**Statut.** Acceptée (exercice).

---

## ADR-022 — Import de gros volumes : fichier CSV traité en asynchrone, partiel

**Contexte.** Exercice, comme l'ADR-021 : aucun besoin métier ne l'exige. Il sert à s'entraîner sur le traitement
asynchrone, la reprise après panne et la concurrence. L'import synchrone de l'ADR-021 reste inchangé : l'appelant
choisit explicitement l'un ou l'autre endpoint, sans bascule automatique selon la taille.

**Décision.**

- **API**, rôle `COUNSELOR` (couvert par `/v2/**`, ADR-017) :
  - `POST /v2/contracts/import-jobs`, `multipart/form-data`, partie `file` : `202`, en-tête `Location` et
    `{ "jobId", "status" }` ;
  - `GET /v2/contracts/import-jobs/{jobId}` : statut, nom du fichier et compteurs ;
  - `GET /v2/contracts/import-jobs/{jobId}/rejections?page&size` : rejets paginés, triés par ligne (bornes de
    l'ADR-018).
  - `GET /v2/contracts/import-jobs?submittedBy&page&size` : jobs du plus récent au plus ancien, avec leur auteur ;
  - `POST /v2/contracts/import-jobs/{jobId}/cancellation` : annule le job et ferme ses contrats ;
  - `GET /v2/contracts/import-jobs/{jobId}/rejections/export` : rejets en CSV, avec leur ligne d'origine.
- **Un seul contrôleur.** `ContractImportJobController` porte les six endpoints, par exception à la règle « un
  contrôleur par endpoint » : ils forment une seule ressource et partagent ses erreurs.
- **Format CSV.** UTF-8, BOM toléré, séparateur `;` (celui d'Excel en Suisse et en France), en-tête obligatoire
  `clientId;startDate;endDate;costAmount`. Dates ISO, décimales avec un point, `startDate` et `endDate` vides autorisés.
  Pas de guillemets : aucune valeur ne contient le séparateur. Un simple `split` suffit donc, sans dépendance.
- **Contrôles à la soumission.** Fichier vide, en-tête incorrect, encodage invalide ou plus de `MAX_LINES` (500 000)
  lignes de données : `422 invalidImportFile`, rien n'est enregistré. Plus de 50 Mo : `413 importFileTooLarge`.
  `resolve-lazily` est activé pour que cette erreur passe par `ContractControllerAdvice`.
- **Partiel.** Les lignes valides créent leur contrat, les autres sont rapportées. Une ligne mal formée est rejetée avec
  `invalidLineFormat`. Les autres codes sont ceux de l'ADR-021.
- **Identifiant de ligne.** Chaque ligne est identifiée par `(job_id, line_number)`, clé primaire de
  `contract_import_job_line`, qui porte soit `contract_id`, soit le code et le message du rejet. `line_number` est le
  numéro physique dans le fichier (l'en-tête est la ligne 1), celui que le conseiller voit dans son tableur.
  L'identifiant du contrat reste un UUID v7 (ADR-012) : ni la table `contract` ni l'agrégat ne changent.
- **Stockage du fichier.** En `BYTEA`, dans `contract_import_job_file`, séparée de `contract_import_job`. Hibernate ne
  charge pas paresseusement un `byte[]` sans instrumenter le bytecode : dans une seule table, chaque lecture de statut
  et chaque lot rechargeraient jusqu'à 50 Mo. La base évite aussi de dépendre d'un disque local si plusieurs instances
  tournent.
- **Spring Batch.** Le traitement est un job Spring Batch à un seul step, en chunks de 2000 lignes
  (`ContractImportBatchConfiguration`, paramètre identifiant `jobId`). `FlatFileItemReader` lit le fichier et donne le
  numéro de ligne, le processor écarte les lignes vides, et le writer délègue à `ProcessContractImportBatch`. Spring
  Batch reste donc un adaptateur entrant, dans le package `batch` : le domaine et les règles ne le connaissent pas. Les
  tables `BATCH_*` sont créées par Flyway dans le schéma `contracts` (`initialize-schema: never`).
- **Worker.** Spring Batch ne choisit pas les jobs à lancer et ne détecte pas les crashs. Un `@Scheduled` réserve donc
  un job avec `FOR UPDATE SKIP LOCKED` : `PENDING`, ou `RUNNING` dont le bail a expiré. Chaque chunk renouvelle le bail.
- **Use cases.** Les douze use cases de cet import sont dans `features/contract/importjob`, comme une seule tranche
  verticale. `SubmitContractImportJob`, `GetContractImportJob`, `ListContractImportJobs`, `ListContractImportJobRejections`,
  `ExportContractImportJobRejections` et `CancelContractImportJob` sont appelés par le contrôleur ; les deux lectures de rejets passent par `ContractImportJobQueryPort` (ADR-011), le repository ne servant qu'à l'agrégat ; `ClaimContractImportJob`, `ProcessContractImportBatch`, `FinishContractImportJob`, `ReleaseContractImportJob`,
  `FailContractImportJob` et `LoadContractImportJobFile` sont appelés par le package `batch`. Aucune règle ne les lui réserve : rien n'empêche un
  autre adaptateur de les appeler, ce qui contournerait la position du reader et le bail. Ce sont tous des application
  services, appelés par des adaptateurs entrants différents. `ContractImportJob` n'accepte `recordBatch`, `finish`, `release` et
  `fail` que sur un job `RUNNING`.
- **Fin de job.** La décision appartient à une seule classe, `ContractImportJobOutcomeListener`, à la fin de l'exécution
  Spring Batch, et non plus au compteur de lignes dans `recordBatch` :
  - `COMPLETED` : `FinishContractImportJob`. Si `processedLines` n'atteint pas `totalLines`, le job échoue en
    `incompleteProcessing` au lieu de rester `RUNNING` ;
  - `STOPPED` ou erreur transitoire (`TransientDataAccessException`, `RecoverableDataAccessException`) :
    `ReleaseContractImportJob`, le job revient en `PENDING` et reprend après le dernier chunk commité ;
  - toute autre erreur : `FailContractImportJob`, en `unexpectedError`.
- **Reprise.** Chaque transaction de chunk commite ensemble les contrats, les lignes, les compteurs, le bail et la
  position du reader : un job repris continue exactement après le dernier chunk commité. Après un crash, l'exécution
  reste `STARTED` et Spring Batch refuse de la relancer (`JobExecutionAlreadyRunningException`) :
  `ContractImportJobRunner` appelle d'abord `JobOperator.recover`. Si la dernière exécution est déjà `COMPLETED` (crash
  avant le listener), le runner applique directement `FinishContractImportJob`. Après `MAX_ATTEMPTS` (5) exécutions en
  échec, le job échoue en `tooManyAttempts`. Une exécution `STOPPED` ne compte pas : elle ne vient que d'un arrêt de
  l'application, et un job doit survivre à plusieurs déploiements successifs. Le `retry` de Spring Batch n'est pas utilisé : il rejoue l'écriture dans la même
  transaction, inutilisable après une erreur PostgreSQL. La remise en file relance dans une transaction neuve.
- **Arrêt de l'application.** `ChunkOrientedStep` (Spring Batch 6.0.6) sort de sa boucle sur une interruption du thread
  sans lever d'erreur : avec la politique par défaut, le step finit `COMPLETED` avec des lignes non lues, et une reprise
  le saute. `ContractImportStepInterruptionPolicy` appelle donc `setTerminateOnly` à la limite d'un chunk, sur une
  interruption comme sur `ContextClosedEvent`. Le step finit `STOPPED`, reste relançable, et le job revient en `PENDING`
  sans attendre l'expiration du bail.
- **Worker remplacé.** Un worker bloqué au-delà du bail peut se réveiller après qu'un autre a repris le job. Son chunk
  échoue sur la clé primaire de `contract_import_job_line` et sur la version de son `StepExecution`. Le listener compare
  son exécution à la dernière de l'instance et ignore le résultat d'une exécution remplacée, sans migration.
- **Bail en `Instant`.** Exception à l'ADR-013 : le bail est une durée technique, pas une date métier. En
  `LocalDateTime`, il durait 65 minutes pendant l'heure répétée du passage à l'heure d'hiver.
- **Lignes qui ne doivent jamais faire échouer un chunk.** Un caractère NUL est rejeté en `invalidLineFormat`, car
  PostgreSQL le refuse dans un `TEXT`. `ContractPeriod` borne les années de 1 à 9999, car `TIMESTAMPTZ` a ses propres
  limites : la règle vaut pour toutes les créations, qui répondent `invalidContractPeriod`. Le reader désactive les
  commentaires (`.comments()`) : par défaut, il ignore les lignes commençant par `#`.
  Une règle du contrat ne fait pas non plus échouer un chunk : `InvalidContractCostException` et
  `InvalidContractPeriodException` héritent de `ContractValidationException`, scellée. `ContractImportLineEvaluator`
  n'intercepte qu'elle et la traduit en motif de rejet par un `switch` exhaustif : une nouvelle règle doit être
  ajoutée à `permits`, puis le `switch` ne compile plus tant qu'elle n'a pas de motif, ni `ContractImportCodes` tant
  qu'elle n'a pas de code. Un filet `DomainValidationException` générique a été écarté : il cacherait la règle oubliée
  derrière un code vague.
- **Client supprimé pendant le traitement.** Chaque lot relit ses clients avec `FOR SHARE`
  (`ClientRepository.lockExistingIds`, `findExistingIds` restant sans verrou). Pour que le verrou serve à quelque chose,
  `DeleteClient` doit écrire `deleted_at` **avant** de fermer les contrats : sans `flush`, Hibernate exécute la
  fermeture d'abord, car elle ne touche pas la table `client`, puis bloque sur le verrou au commit. Le lot pourrait
  alors insérer un contrat que la fermeture ne verrait jamais, d'où un contrat actif sur un client supprimé.
  `JpaClientRepository.delete` fait donc un `flush`. La course de l'import synchrone et de `CreateContract`, qui lisent
  sans verrou, reste celle décrite dans l'ADR-021.
- **Attente bornée : `lock_timeout` de 5 s.** Le verrou `FOR SHARE` n'est tenu que pendant un chunk (2000 lignes), pas
  pendant tout l'import : à environ 8000 lignes par seconde (500 000 lignes en une minute au plus), un chunk le garde
  quelques centaines de millisecondes. Un `PUT`, `PATCH` ou `DELETE` sur un client du chunk attend donc ce commit, sans
  échouer. Sans borne, un chunk bloqué (connexion lente, pause du GC) ferait attendre ces requêtes indéfiniment.
  `SET lock_timeout` est posé à l'ouverture de chaque connexion du pool (`connection-init-sql`, propriété
  `app.database.lock-timeout`). 5 s laissent une marge d'environ 20 fois la durée normale d'un chunk. Au-delà :
  - côté API, PostgreSQL annule l'instruction, Spring lève `CannotAcquireLockException` et l'API répond
    `409 resourceBusy` sans rien modifier : le conseiller renvoie la requête quelques secondes plus tard ;
  - côté import, l'exception est transitoire : le job retourne dans la file et reprend au dernier chunk commité,
    dans la limite de `ContractImportJobRunner.MAX_ATTEMPTS`.
  La borne s'applique à toute attente de verrou, migrations Flyway comprises : une migration qui attendrait plus de
  5 s un verrou tenu par l'application échoue au démarrage au lieu de bloquer le trafic derrière elle. Le verrou
  consultatif de Flyway n'est pas concerné, Flyway l'obtient par essais successifs. Si un jour une opération doit
  attendre plus longtemps, elle relève la borne pour sa transaction (`SET LOCAL lock_timeout`), sans changer la
  valeur globale. Défaut connu : `FOR SHARE` fait aussi attendre la modification d'un client, alors que seule sa
  suppression menace la règle ; un verrou consultatif par client ne bloquerait que la suppression, à revoir si
  l'attente gêne.
- **Pas de version attendue** (ADR-011) : le traitement ne dépend pas d'un état lu par l'utilisateur.
- **Auteur d'un job.** La soumission enregistre le `preferred_username` du jeton dans `submitted_by`, ou son `sub` s'il n'en a pas. L'annulation enregistre de même `cancelled_by`. Les jobs soumis avant cet ajout ont un auteur `null`. La liste montre les jobs de tous les conseillers, avec un filtre facultatif `submittedBy` : aucune règle de propriété, comme dans le reste de l'API (ADR-017).
- **Idempotence de la soumission.** Sans protection, une double soumission (double clic, nouvel essai après un timeout, réponse `202` perdue) importe deux fois tout le fichier. L'en-tête facultatif `Idempotency-Key` (255 caractères au plus) est unique par auteur, garanti par l'index unique partiel `(submitted_by, idempotency_key)`.
  - même clé et même fichier : `202` avec le job déjà créé et son `Location`, sans nouveau job ;
  - même clé et autre fichier : `422 idempotencyKeyReused`. Le fichier est comparé par son empreinte SHA-256 (`content_sha256`), calculée à chaque soumission et rétro-calculée par la migration pour les jobs existants ;
  - clé vide ou trop longue : `400 invalidIdempotencyKey` ;
  - deux soumissions simultanées avec la même clé : la seconde viole l'index unique et reçoit `409 dataIntegrityViolation`. Un nouvel essai renvoie alors le job créé par la première.
  - Sans clé, rien ne change. La rendre obligatoire (`428`, comme `If-Match`) casserait les appelants existants. Les clés n'expirent pas.
- **Annulation.** `POST /v2/contracts/import-jobs/{jobId}/cancellation` annule un job dans n'importe quel état. Un job `FAILED` n'est pas relancé : comme il est déjà importé en partie, le conseiller l'annule, puis soumet un fichier corrigé, sans doublon actif.
  - Les contrats du job encore actifs sont fermés à l'instant de l'annulation (`end_date = last_modified = cancelled_at`, `version` incrémentée comme toute mise à jour en masse, ADR-011). Ce sont des contrats annulés au sens de l'ADR-009 : ils sortent de la liste et des totaux, et restent lisibles. Ils sont retrouvés par `contract_import_job_line.contract_id`. Un contrat déjà fermé (expiré, client supprimé) n'est pas touché ; un contrat dont le coût a changé depuis l'import est fermé quand même.
  - Une seconde annulation ne change rien et renvoie `closedContracts = 0`. Pas de `If-Match` (ADR-011) : le résultat ne dépend pas d'un état lu par le conseiller.
  - Pendant un chunk : l'annulation verrouille la ligne du job (`SELECT ... FOR UPDATE`) avant de fermer les contrats. Un chunk déjà commité voit ses contrats fermés. Un chunk en cours ne peut plus rien commiter : s'il relit le job annulé, `recordBatch` lève `ContractImportJobCancelledException` ; s'il l'avait lu avant, sa mise à jour du job échoue sur `@Version`. Dans les deux cas sa transaction est annulée. `finish`, `release` et `fail` laissent un job annulé tel quel, et le listener reconnaît l'annulation. Spring Batch journalise quand même l'arrêt du step en erreur.
- **Export des rejets.** `GET /v2/contracts/import-jobs/{jobId}/rejections/export` renvoie un CSV `lineNumber;rejectionCode;rejectionMessage;line`, trié par ligne. La colonne `line` est relue dans le fichier stocké : aucune migration. Les colonnes de texte sont entre guillemets, car le message et la ligne d'origine contiennent des `;`. Le conseiller corrige la ligne et la recopie dans un nouveau fichier. L'export est construit en mémoire : quelques dizaines de Mo au pire, pour un fichier de 50 Mo entièrement rejeté. Acceptable à cette échelle ; au-delà, il faudrait le diffuser en flux.

**Performance : ce qui est retenu, et pourquoi.** `CHUNK_SIZE = 2000`, `hibernate.jdbc.batch_size: 1000` et
`reWriteBatchedInserts: true` (propriété de pgjdbc, via `spring.datasource.hikari.data-source-properties`).

Un chronométrage temporaire sur 50 000 lignes (chunks de 5000, `batch_size` 50) a montré que l'écriture des `INSERT`
prend 89 % du step : 6,1 s sur 6,8 s, pour 100 000 lignes écrites, soit un contrat et une ligne de rapport par ligne du
fichier. Le parsing, les règles du domaine, le verrou `FOR SHARE` et Spring Batch pèsent chacun 6 % au plus.

| Réglage                                                | Durée du step sur 50 000 lignes |
|--------------------------------------------------------|---------------------------------|
| Chunk 500, `batch_size` 50                             | 7,3 s                           |
| Chunk 5000, `batch_size` 50                            | 6,8 s                           |
| Chunk 2000, `batch_size` 1000                          | **5,8 s**                       |
| Chunk 2000, `batch_size` 1000, `reWriteBatchedInserts` | **4,6 s**                       |

Ces durées viennent d'un run chacune, sur une base Docker locale : ce sont des ordres de grandeur.

- **Le chunk décide du commit, `batch_size` de la taille des paquets envoyés.** La taille du chunk influence peu la
  durée mesurée, mais elle fixe la durée du verrou `FOR SHARE` et le travail perdu sur une erreur. `batch_size` doit
  rester inférieur ou égal au chunk : au-delà, il ne produit pas de paquets plus grands.
- **`INSERT` et verrous.** Un `INSERT` ne bloque pas les autres lectures et écritures de la table. Le seul verrou gênant
  est `FOR SHARE`, tenu jusqu'au commit du chunk : il fait attendre la suppression ou la modification d'un client de ce
  chunk, environ 200 ms d'après ces mesures. Un `batch_size` plus grand raccourcit cette attente.
- **`reWriteBatchedInserts`.** Le driver fusionne les `INSERT` d'un paquet en requêtes multi-valeurs. Il ne touche pas
  aux `UPDATE`, donc la détection des conflits `@Version` ne change pas. Il renvoie « succès, nombre inconnu » au lieu
  du nombre de lignes par `INSERT`, ce qui est sans effet ici, car un `INSERT` sans `ON CONFLICT` réussit ou lève une
  erreur. Une erreur de contrainte n'indique plus précisément la ligne fautive, ce qui ne gêne que le diagnostic : le
  chunk est annulé dans tous les cas.

**Options écartées.**

- **Clé composite sur `contract`, ou UUID v5 dérivé de `(jobId, ligne)`.** La première casse l'API et le curseur
  d'export, la seconde perd les UUID v7 (ADR-012), et aucune des deux ne stocke les rejets.
- **Colonnes `import_job_id` et `line_number` sur `contract`.** Elles imposent un détail d'import à chaque contrat, et
  les rejets restent à stocker ailleurs.
- **Boucle de lots écrite à la main.** Première version de cet ADR : `processed_lines` servait de position de reprise.
  Elle fonctionnait, sans dépendance ni tables `BATCH_*`, mais toute violation de contrainte y était lue comme une
  reprise par un autre worker, avec un risque de reprises sans fin. Spring Batch est retenu pour l'exercice et pour ces
  garanties, pas pour réduire le code : la réservation, le bail et le rapport de rejets restent nécessaires.
- **`@Async` seul.** Un redémarrage perdrait le job sans laisser de trace.
- **Tout-ou-rien en une transaction.** Une transaction de plusieurs minutes sur 500 000 lignes bloquerait les
  suppressions de clients pendant toute sa durée.

**Conséquences.**

- Migrations `V2.0.0.6` (trois tables du domaine) et `V2.0.0.7` (tables de Spring Batch, script fourni par
  `spring-batch-core`, préfixé par le schéma).
- Les fichiers restent en base : aucune purge n'est prévue. À traiter si cet import quittait le cadre de l'exercice.
- Un job s'exécute dans le thread du planificateur : une instance traite un job à la fois.
- Limite : un worker vivant dont un chunk dépasse le bail de 5 minutes voit son job repris par un autre worker. Son
  chunk en cours est perdu (annulé) et le worker remplaçant continue. `ContractImportJobRecoveryAcceptanceTest` couvre ce cas.
- Migration `V2.0.0.8` : `failure_reason` sur `contract_import_job`, exposé en `failureCode` par l'API.
- Migration `V2.0.0.9` : `submitted_by`, `idempotency_key`, `content_sha256` (calculée pour les jobs existants), `cancelled_at`, `cancelled_by`, statut `CANCELLED` et index unique partiel de l'idempotence.
- CORS : `Idempotency-Key` est autorisé, `Content-Disposition` exposé.
- La purge des fichiers, si elle arrive, devra les garder tant que les rejets peuvent être exportés.
- `contract_import_job_line.contract_id` référence `contract` : les tests suppriment les jobs avant les contrats.
- Le worker est désactivé dans le profil `test` (`app.contract-import.worker-enabled`) et activé seulement par les tests
  qui le visent : sinon chaque contexte Spring en cache aurait son propre worker, avec une horloge parfois modifiée.

**Statut.** Acceptée (exercice).

---

## ADR-023 — Analyse statique : Error Prone et NullAway

**Contexte.** Le code annote ses valeurs optionnelles avec JSpecify (`@Nullable`), mais rien ne le vérifiait à la compilation : l'IDE signale le déréférencement d'une valeur `@Nullable`, pas le passage de `null` à un paramètre non annoté. Le `@NullMarked` de `com/mk/contractservice/package-info.java` ne couvre que ce package : en JSpecify, il ne s'applique pas aux sous-packages. Le numéro ADR-022 est pris par l'import asynchrone (`feat/import-contract-jobs`).

**Décision.**
- **Error Prone** (`maven-compiler-plugin`, `.mvn/jvm.config` pour les `--add-exports` du compilateur) avec ses contrôles par défaut : les contrôles de niveau erreur cassent le build, les avertissements restent des avertissements (pas de `-Werror`). Le code généré (MapStruct, `generated-sources`) est exclu.
- **NullAway** en erreur sur `src/main`, désactivé sur `src/test` :
  - `AnnotatedPackages=com.mk.contractservice` : tout le code du projet est considéré non nul par défaut, même dans un package qui aurait oublié son `@NullMarked` ;
  - `JSpecifyMode=true` ;
  - entités JPA exclues (`@Entity`, `@MappedSuperclass`, `@Embeddable`) : Hibernate les remplit par réflexion.
- **Pas de comparaison de références sur les agrégats** (`ReferenceEquality`) : le no-op des mises à jour compare l'état (`updated.equals(loaded)`, `@EqualsAndHashCode` de Lombok, ADR-002).
- Les absences légitimes sont déclarées `@Nullable` plutôt que contournées : `detail` d'un `ProblemDetail` (facultatif en RFC 9457), `code` et `cause` de `DomainValidationException`, `clientId` d'un contrat tant que la colonne accepte `NULL`. Une `DomainValidationException` porte toujours un message (`getMessage()` non nul).
- Les réponses REST exigent un id : `Objects.requireNonNull(aggregate.id())` dans les mappers, un agrégat non persisté n'a rien à faire dans une réponse.
- **Un `package-info.java` annoté `@NullMarked` dans chaque package de `src/main`**, pour que l'IDE applique la même règle que NullAway pendant l'écriture : sans lui, IntelliJ ne signale pas un `null` passé à un paramètre non annoté. `ArchitectureTest.every_package_should_be_null_marked` fait échouer le build si un nouveau package l'oublie.
- **Formatage par Spotless** avec le profil Eclipse exporté d'IntelliJ (`config/formatter/eclipse-java-formatter.xml`), en `ratchetFrom` : seuls les fichiers modifiés par rapport à la branche cible sont vérifiés, pour ne pas reformater tout le projet d'un coup. Hors `verify` : `./mvnw spotless:apply` en local, `spotless:check` en première étape de la CI.

**Alternatives écartées.**
- **Checker Framework (Nullness Checker)** : plus complet, mais beaucoup plus lent et plus exigeant (annotations de généricité partout). NullAway est conçu pour tourner à chaque compilation.
- **Seulement l'IDE** : il ne vérifie ni la CI ni les contributions faites sans IntelliJ.
- **NullAway en `OnlyNullMarked` (sans `AnnotatedPackages`)** : il ne vérifierait que les packages annotés. Garder `AnnotatedPackages` protège le build même avant que le test ArchUnit ne signale un oubli.

**Conséquences.**
- Un `null` passé là où il n'est pas déclaré casse la compilation.
- Les inférences de génériques de NullAway restent limitées : `ContractPeriod.of` donne ses arguments de type explicitement à `ValueObjectUtils.validateAndCreate`.
- Error Prone se met à jour avec le JDK ; une nouvelle version de Java peut demander une nouvelle version d'Error Prone avant de compiler.
- IntelliJ compile avec les arguments du `pom.xml` : si sa compilation échoue sur `-Xplugin:ErrorProne`, déléguer le build à Maven (*Build Tools → Maven → Runner → Delegate IDE build/run actions to Maven*).

**Statut.** Acceptée.
