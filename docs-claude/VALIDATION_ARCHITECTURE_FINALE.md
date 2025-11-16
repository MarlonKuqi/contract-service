# ✅ Architecture Finale - Validation avec super.checkInvariants()

## Décision architecturale

Après analyse, nous avons choisi **la version simple avec `super.checkInvariants()`** car elle est :
- ✅ Plus lisible et maintenable
- ✅ Pattern classique d'héritage Java
- ✅ Une seule méthode au lieu de trois
- ✅ Respecte les principes KISS (Keep It Simple, Stupid)

## Architecture implémentée

### Client (classe abstraite)

```java
protected Client(UUID id, ClientName name, Email email, PhoneNumber phone) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.phone = phone;
    // PAS d'appel à checkInvariants() ici !
}

protected void checkInvariants() {
    if (name == null) throw new IllegalArgumentException("...");
    if (email == null) throw new IllegalArgumentException("...");
    if (phone == null) throw new IllegalArgumentException("...");
}
```

### Person (classe concrète)

```java
private Person(UUID id, ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
    super(id, name, email, phone);  // 1. Affecte champs parents
    this.birthDate = birthDate;      // 2. Affecte champ spécifique
    checkInvariants();               // 3. Valide TOUT
}

@Override
protected void checkInvariants() {
    super.checkInvariants();  // Valide parent d'abord
    if (birthDate == null) throw new IllegalArgumentException("...");
}
```

### Company (classe concrète)

```java
private Company(UUID id, ClientName name, Email email, PhoneNumber phone, CompanyIdentifier companyIdentifier) {
    super(id, name, email, phone);
    this.companyIdentifier = companyIdentifier;
    checkInvariants();
}

@Override
protected void checkInvariants() {
    super.checkInvariants();
    if (companyIdentifier == null) throw new IllegalArgumentException("...");
}
```

## Flow de validation

```
Person.of(name, email, phone, birthDate)
  ↓
builder().name(...).build()
  ↓
new Person(null, name, email, phone, birthDate)
  ↓
super(null, name, email, phone)  → Affecte id, name, email, phone (pas de validation)
  ↓
this.birthDate = birthDate  → Affecte birthDate
  ↓
checkInvariants()  → Person.checkInvariants()
  ↓
super.checkInvariants()  → Client.checkInvariants() (valide name, email, phone)
  ↓
if (birthDate == null) throw...  → Valide birthDate
  ↓
✅ Instance Person complètement validée
```

## Pourquoi pas l'approche avec validateAll() ?

L'approche alternative avec 3 méthodes :
```java
// Client
private void checkClientInvariants() { ... }
protected void checkInvariants() { }  // vide par défaut
protected final void validateAll() {
    checkClientInvariants();
    checkInvariants();
}
```

**Inconvénients** :
- ❌ 3 méthodes au lieu de 1
- ❌ Une méthode vide par défaut (confusion)
- ❌ Moins intuitif : pourquoi `validateAll()` ET `checkInvariants()` ?
- ❌ Plus de complexité pour le même résultat
- ❌ Viole le principe KISS

## Règles à respecter

### ✅ À faire
1. **N'appelez PAS `checkInvariants()` dans le constructeur parent** (Client)
2. **Appelez `checkInvariants()` dans les constructeurs finaux** (Person, Company)
3. **Appelez toujours `super.checkInvariants()` en premier** dans les sous-classes
4. **Affectez tous les champs AVANT d'appeler `checkInvariants()`**

### ❌ À éviter
1. Ne pas appeler `checkInvariants()` dans Client → sinon appelé trop tôt (champs enfants pas affectés)
2. Ne pas oublier `super.checkInvariants()` dans les sous-classes → sinon champs parents non validés
3. Ne pas créer plusieurs méthodes de validation → complexité inutile

## Garanties DDD

✅ **"Always Valid"** : Impossible de créer une instance invalide  
✅ **Encapsulation** : Builder privé, factory methods publiques  
✅ **Immutabilité** : Champs final, méthode `withCommonFields()` crée nouvelle instance  
✅ **Validation centralisée** : Une seule méthode `checkInvariants()` par classe  
✅ **Lisibilité** : Pattern classique d'héritage Java  

## Documentation

Cette décision est documentée dans :
- ✅ `CLAUDE.md` section "Domain Entities Validation"
- ✅ Ce document `VALIDATION_ARCHITECTURE_FINALE.md`

Date : 2025-01-16  
Décision : **Version simple avec `super.checkInvariants()`**  
Statut : ✅ **DÉFINITIF**

