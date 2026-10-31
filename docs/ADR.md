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
- **Une seule définition d'une valeur valide : le Value Object.** Les DTO ne valident que la forme du contrat HTTP (présence, type, longueur maximale) et ne redéfinissent pas les règles de format. `@Email` a été retiré : ses règles différaient de `ClientEmail` (`a@b` passait le DTO puis échouait dans le domaine, `.a@x.ch` échouait dans le DTO avec un autre code d'erreur). Toute adresse au format invalide renvoie maintenant `EMAIL_INVALID`.
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
3. **Réessai côté serveur (fait : `PUT`/`PATCH` et suppression d'un client, coût d'un contrat)** : les handlers sont annotés `@Retryable` sur `OptimisticLockingFailureException` (3 nouveaux essais, `delay` 20 ms, `jitter` 10 ms), activé par `@EnableResilientMethods` (`ResilienceConfig`). Chaque essai ouvre une nouvelle transaction : il relit l'agrégat et revérifie la version attendue et les règles métier.

**Conception de l'étape 2.**
- La version est un détail de persistance, pas une donnée métier : le domaine ne décide rien à partir d'elle (contrairement à `lastModified`, ADR-005). Elle reste hors des agrégats et hors des types renvoyés par les ports d'écriture : `getById` et `save` renvoient `Client` et `Contract`.
- **Lecture (CQRS)** : le `GET` passe par un read model, `ClientQueryPort.getDetails` (`PersonDetails` / `CompanyDetails`) et `ContractQueryPort.getDetails` (`ContractDetails`). Un read model n'est pas un agrégat : il peut porter la version, lue dans la même requête que les données. Les read models ne portent que des types simples. Le champ `active` de `ContractDetails` est calculé par l'adaptateur à l'instant fourni par le use case, en reconstituant `ContractPeriod` et en appelant `isEffectiveAt` : la règle de l'ADR-004 n'est pas dupliquée.
- **Écriture** : la commande porte la version attendue (`expectedVersion`) ; le handler appelle `ensureVersion(id, expectedVersion)` sur le port juste après le chargement, sur le modèle de `ensureExists`. Dans la même transaction, l'adaptateur relit l'entité depuis le contexte de persistance, donc la version comparée est celle des données chargées. Ordre des erreurs : `404` (ressource absente), puis `412` (précondition), puis validation du contenu et règles métier (`409 contractExpired`, `422`). Une précondition est évaluée avant de traiter le contenu (RFC 9110 §13.2.2).
- **Quand exiger une version attendue** : seulement quand la commande traduit la décision d'un utilisateur prise sur un état qu'il a lu dans une requête précédente (`PUT`/`PATCH`). Pas pour une création, ni pour une opération dont le résultat ne dépend pas de l'état lu (suppression d'un client), ni pour un traitement qui lit et écrit dans la même transaction (fermeture en masse, futurs jobs) : `@Version` suffit. Un import qui met à jour des données existantes demandera une règle métier, le système source n'ayant pas d'ETag.
- Le controller convertit l'en-tête (`EntityTags.requiredVersion`) et renvoie `428` sans appeler le use case quand il manque. La validation Bean Validation du corps a lieu avant : un corps invalide sans `If-Match` donne `422`.
- Une modification concurrente qui se glisse entre la vérification de la version et le `flush` lève `OptimisticLockingFailureException`. Le réessai relit l'agrégat, voit la nouvelle version et répond `412`. Il n'y a pas de modification perdue, et pas de `409 optimisticLockFailure` sur les endpoints protégés par `If-Match`.
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

**Effet sur l'étape 3.** Avant `If-Match`, le réessai appliquait « le dernier gagne » : le coût réessayé écrasait le coût concurrent, le `PATCH` d'un client gardait aussi la modification concurrente d'un autre champ, et le `PUT` n'était pas réessayé parce qu'il aurait écrasé sans bruit les champs non touchés. Avec `If-Match`, le réessai ne peut plus écraser une modification concurrente : il aboutit à `412`. Il ne sert plus qu'à transformer un conflit détecté au commit en la bonne réponse, d'où son ajout sur le `PUT`. Pour la suppression d'un client, sans `If-Match`, il garde son rôle d'origine : rejouer la suppression.

**Contraintes du réessai.**
- Il doit envelopper la transaction : un réessai à l'intérieur relirait l'entité périmée du contexte de persistance. Spring place l'intercepteur `@Retryable` avant les intercepteurs existants (`beforeExistingAdvisors`), ce qui permet de garder `@Retryable` et `@Transactional` sur le même handler. `ContractCostConflictAcceptanceTest` et `ClientConflictAcceptanceTest` vérifient cet ordre : ils injectent une écriture concurrente entre la lecture et l'écriture, et attendent deux lectures.
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
