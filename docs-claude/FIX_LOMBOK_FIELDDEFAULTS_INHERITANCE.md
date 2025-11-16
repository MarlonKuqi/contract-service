# 🔧 Fix - Lombok @FieldDefaults et Héritage

## Problème rencontré

Lors de la compilation :
```
error: phone has private access in com.mk.contractservice.domain.client.Client
```

## Cause

L'annotation `@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)` sur les classes `Person` et `Company` rendait **TOUS** les champs privés, y compris ceux hérités de `Client`.

Lombok ne pouvait plus accéder aux champs de la classe parente via le builder.

### Code problématique

```java
// Client.java
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public abstract sealed class Client {
    UUID id;           // Rendu private par @FieldDefaults
    ClientName name;   // Rendu private par @FieldDefaults
    Email email;       // Rendu private par @FieldDefaults
    PhoneNumber phone; // Rendu private par @FieldDefaults
}

// Person.java
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public final class Person extends Client {
    PersonBirthDate birthDate;
    
    @Builder(toBuilder = true)
    private Person(UUID id, ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
        super(id, name, email, phone);  // ❌ Lombok ne peut pas accéder aux champs privés de Client
        this.birthDate = birthDate;
    }
}
```

## Solution

**Supprimer `@FieldDefaults`** et déclarer explicitement les champs comme `private final`.

### Code corrigé

```java
// Client.java ✅
@Getter
public abstract sealed class Client permits Person, Company {
    private final UUID id;           // ✅ Explicitement private final
    private final ClientName name;   // ✅ Explicitement private final
    private final Email email;       // ✅ Explicitement private final
    private final PhoneNumber phone; // ✅ Explicitement private final
    
    protected Client(UUID id, ClientName name, Email email, PhoneNumber phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
}

// Person.java ✅
@Getter
public final class Person extends Client {
    private final PersonBirthDate birthDate;  // ✅ Explicitement private final
    
    @Builder(toBuilder = true)
    private Person(UUID id, ClientName name, Email email, PhoneNumber phone, PersonBirthDate birthDate) {
        super(id, name, email, phone);  // ✅ Fonctionne maintenant
        this.birthDate = birthDate;
        checkInvariants();
    }
}
```

## Pourquoi ça marche maintenant ?

1. Les champs de `Client` sont `private final` → Encapsulés correctement
2. Lombok `@Getter` génère les getters publics
3. Le builder de `Person` peut accéder aux getters de `Client`
4. Pas de conflit avec `@FieldDefaults`

## Avantages de la solution

✅ **Plus explicite** : On voit directement que les champs sont `private final`  
✅ **Plus clair** : Pas de "magie" Lombok cachée  
✅ **Compatible** : Fonctionne avec l'héritage et `@Builder`  
✅ **Maintenable** : Code standard Java

## Alternative non retenue

On aurait pu garder `@FieldDefaults` et utiliser `AccessLevel.PROTECTED` :

```java
// ❌ Moins bon : Expose les champs aux sous-classes
@FieldDefaults(makeFinal = true, level = AccessLevel.PROTECTED)
public abstract sealed class Client {
    UUID id;  // protected final
}
```

**Problème** : Les champs seraient `protected` au lieu de `private`, ce qui viole l'encapsulation.

## Conclusion

**Bonne pratique** : Avec l'héritage et Lombok `@Builder`, déclarez explicitement les champs comme `private final` au lieu d'utiliser `@FieldDefaults`.

```java
// ✅ BON
private final UUID id;

// ❌ ÉVITER avec héritage + @Builder
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
UUID id;
```

**Date** : 2025-01-16  
**Statut** : ✅ RÉSOLU

