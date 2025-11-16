# ❌ @SuperBuilder NE FONCTIONNE PAS - Décision finale

## Problème découvert

`@SuperBuilder` de Lombok **IGNORE LA VALIDATION** du constructeur.

### Tests qui échouent

```
[ERROR] shouldRejectNullName -- Expecting code to raise a throwable
[ERROR] shouldRejectNullEmail -- Expecting code to raise a throwable  
[ERROR] shouldRejectNullPhone -- Expecting code to raise a throwable
[ERROR] shouldRejectNullCompanyIdentifier -- Expecting code to raise a throwable
[ERROR] shouldRejectNullPersonBirthDateInConstructor -- Expecting code to raise a throwable
```

**Tous les tests de validation échouent** car `@SuperBuilder` ne call pas le constructeur avec validation.

## Pourquoi @SuperBuilder échoue

### Le code

```java
@SuperBuilder(toBuilder = true)
public final class Person extends Client {
    
    protected Person(...) {
        super(...);  // Validation dans Client
        this.birthDate = birthDate;
        checkInvariants();  // Validation dans Person
    }
}
```

### Le problème

Lombok génère un builder qui **bypass le constructeur** et initialise les champs directement via réflexion.

**Résultat** : La validation n'est JAMAIS appelée.

```java
Person person = Person.builder()
    .name(null)  // ❌ Devrait lancer une exception
    .build();    // ✅ Construit l'objet SANS validation !
```

## Solutions tentées

### ❌ Tentative 1 : Surcharger PersonBuilder.build()

```java
public static class PersonBuilder extends PersonBuilderImpl {
    @Override
    public Person build() {
        validate();  // ❌ Ne peut pas accéder aux champs du builder parent
        return super.build();
    }
}
```

**Problème** : Impossible d'accéder aux champs (`name`, `email`, etc.) du builder généré par Lombok.

### ❌ Tentative 2 : Constructeur protected

```java
protected Person(...) {  // protected au lieu de private
    super(...);
    checkInvariants();
}
```

**Problème** : `@SuperBuilder` ne passe pas par le constructeur, il utilise la réflexion.

### ❌ Tentative 3 : @Builder.Default

```java
@Builder.Default
private ClientName name = validateName();  // ❌ Ne fonctionne pas
```

**Problème** : Ne permet pas de validation inter-champs.

## ✅ SOLUTION FINALE : Builder manuel

La SEULE solution viable est le **builder manuel**.

### Code final

#### Person.java

```java
@Getter
public final class Person extends Client {

    private final PersonBirthDate birthDate;

    private Person(UUID id, ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
        super(id, name, email, phone);  // ← Validation appelée ✅
        this.birthDate = birthDate;
        checkInvariants();  // ← Validation appelée ✅
    }

    // Builder manuel
    public static PersonBuilder builder() {
        return new PersonBuilder();
    }

    public PersonBuilder toBuilder() {
        return new PersonBuilder()
                .id(this.getId())
                .name(this.getName())
                .email(this.getEmail())
                .phone(this.getPhone())
                .birthDate(this.birthDate);
    }

    public static class PersonBuilder {
        private UUID id;
        private ClientName name;
        private Email email;
        private PhoneNumber phone;
        private PersonBirthDate birthDate;

        public PersonBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public PersonBuilder name(ClientName name) {
            this.name = name;
            return this;
        }

        public PersonBuilder email(Email email) {
            this.email = email;
            return this;
        }

        public PersonBuilder phone(PhoneNumber phone) {
            this.phone = phone;
            return this;
        }

        public PersonBuilder birthDate(PersonBirthDate birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        public Person build() {
            return new Person(id, name, email, phone, birthDate);  // ← Appelle le constructeur ✅
        }
    }
}
```

## Comparaison finale

| Aspect | @SuperBuilder | Builder manuel |
|--------|---------------|----------------|
| **Lignes de code** | 1 annotation | ~50 lignes |
| **Validation** | ❌ IGNORÉE | ✅ GARANTIE |
| **toBuilder()** | ✅ Gratuit | ⚠️ Manuel mais simple |
| **Héritage** | ✅ Géré | ⚠️ Chaque classe a son builder |
| **Fiabilité** | ❌ Ne fonctionne pas | ✅ Fonctionne toujours |
| **DDD** | ❌ Viole "Always Valid" | ✅ Respecte "Always Valid" |

## Verdict final

**@SuperBuilder est INCOMPATIBLE avec la validation DDD.**

Pour respecter le principe DDD **"Always Valid"**, nous DEVONS utiliser un **builder manuel**.

### Trade-off accepté

✅ **+50 lignes de code par classe**  
✅ **Validation garantie à 100%**  
✅ **Principe DDD "Always Valid" respecté**  
✅ **Tests de validation passent**  

vs  

❌ **1 annotation Lombok**  
❌ **Validation ignorée**  
❌ **Principe DDD violé**  
❌ **Objets invalides possibles**  

## Conclusion

**Le builder manuel est la SEULE solution viable.**

Lombok `@SuperBuilder` est **inadapté** pour un domain model DDD qui exige une validation stricte.

**Citation DDD** :

> "An entity should never be in an invalid state."  
> — Eric Evans

Avec `@SuperBuilder`, cette règle est **VIOLÉE**.

**Décision** : Builder manuel obligatoire.

**Date** : 2025-01-16  
**Statut** : ✅ DÉCISION DÉFINITIVE

