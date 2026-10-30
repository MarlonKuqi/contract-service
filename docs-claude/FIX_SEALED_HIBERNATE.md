# ✅ Fix : Sealed Classes et Hibernate

## 🐛 Problème Rencontré

```
java.lang.IncompatibleClassChangeError: 
class ClientJpaEntity$HibernateProxy cannot inherit from sealed class ClientJpaEntity
```

**Cause** : Hibernate ne peut pas créer de **proxies dynamiques** pour les classes `sealed`, ce qui est nécessaire pour le lazy loading.

---

## 🔧 Solution Appliquée

### 1. Entités JPA : **NON sealed**

```java
// AVANT ❌
public abstract sealed class ClientJpaEntity permits PersonJpaEntity, CompanyJpaEntity {
    // Hibernate ne peut pas créer de proxy !
}

// APRÈS ✅
public abstract class ClientJpaEntity {
    // Hibernate peut créer des proxies pour le lazy loading
}
```

**Fichier modifié** : `ClientJpaEntity.java`

---

### 2. Domaine : **RESTE sealed** ✅

```java
// INCHANGÉ - C'est parfait comme ça !
public abstract sealed class Client permits Person, Company {
    // Le domaine reste pur et expressif avec sealed
}
```

**Aucun changement nécessaire** dans le domaine.

---

### 3. Assembler : Ajout de `default` case

Puisque `ClientJpaEntity` n'est plus `sealed`, le switch n'est plus exhaustif :

```java
// AVANT ❌ (ne compile plus)
public Client toDomain(ClientJpaEntity entity) {
    return switch (entity) {
        case PersonJpaEntity p -> ...
        case CompanyJpaEntity c -> ...
        // ❌ Pas de default = erreur de compilation
    };
}

// APRÈS ✅
public Client toDomain(ClientJpaEntity entity) {
    return switch (entity) {
        case PersonJpaEntity p -> ...
        case CompanyJpaEntity c -> ...
        default -> throw new IllegalArgumentException(...); // ✅ Obligatoire
    };
}
```

**Note** : Le switch `toJpaEntity(Client domain)` reste **sans default** car `Client` du domaine est toujours `sealed`.

**Fichier modifié** : `ClientAssembler.java`

---

## 📋 Résumé

| Couche | Class | Sealed ? | Raison |
|--------|-------|----------|--------|
| **Domain** | `Client` | ✅ **OUI** | Expression du modèle métier, sûreté du typage |
| **Domain** | `Person` | ✅ **final** | Leaf class |
| **Domain** | `Company` | ✅ **final** | Leaf class |
| **Infrastructure** | `ClientJpaEntity` | ❌ **NON** | Hibernate a besoin de créer des proxies |
| **Infrastructure** | `PersonJpaEntity` | ✅ **final** | Leaf class (pas de proxy nécessaire) |
| **Infrastructure** | `CompanyJpaEntity` | ✅ **final** | Leaf class (pas de proxy nécessaire) |

---

## ✅ Avantages de cette Architecture

1. **Domaine expressif** : `sealed class Client` exprime clairement qu'il n'y a que Person et Company
2. **Switch exhaustif dans le domaine** : Pas de `default` case nécessaire
3. **Hibernate compatible** : Les entités JPA peuvent être proxifiées
4. **ACL propre** : L'Assembler gère la conversion et protège le domaine

---

## 🚀 Tests Maintenant

Les tests d'intégration devraient maintenant passer ! 🎉

```bash
mvn clean verify
```

**Hibernate peut maintenant** :
- ✅ Créer des proxies pour `ClientJpaEntity`
- ✅ Faire du lazy loading
- ✅ Gérer l'héritage JOINED
- ✅ Démarrer le contexte Spring

---

## 🎯 Leçon Apprise

**`sealed` est excellent pour le domaine DDD**, mais **incompatible avec Hibernate** qui a besoin de créer des proxies dynamiques.

**Solution** : Séparer domaine et infrastructure
- **Domaine** = `sealed` pour l'expressivité
- **Infrastructure** = `abstract` (non sealed) pour Hibernate
- **Assembler** = Protège le domaine des contraintes techniques

