# POC : Migration Value Object Email vers Clean Architecture

## 🎯 Objectif
Migrer **uniquement** le Value Object `Email` pour valider l'approche Clean Architecture.

**Durée estimée :** 1-2 heures  
**Risque :** Très faible  
**Réversible :** Oui (Git)

---

## 📋 Checklist

- [ ] Backup du code actuel (Git commit)
- [ ] Créer `EmailValue` (domain pur)
- [ ] Créer `EmailEmbeddable` (infrastructure)
- [ ] Adapter `Client` pour utiliser `EmailValue`
- [ ] Adapter `ClientJpaEntity` pour utiliser `EmailEmbeddable`
- [ ] Mapper entre les deux dans le repository
- [ ] Vérifier que les tests passent
- [ ] Mesurer les impacts

---

## 🔄 Étape par Étape

### Étape 1 : Commit de sauvegarde

```bash
git add .
git commit -m "feat: before Email value object refactoring (POC)"
git checkout -b poc/clean-architecture-email
```

---

### Étape 2 : Créer EmailValue (domain pur)

**Fichier :** `src/main/java/com/mk/contractservice/domain/valueobject/EmailValue.java`

```java
package com.mk.contractservice.domain.valueobject;

import java.util.Locale;
import java.util.Objects;

/**
 * Pure domain value object representing an email address.
 * No framework dependencies - can be used in any context.
 */
public final class EmailValue {

    private final String value;

    private EmailValue(final String value) {
        this.value = value;
    }

    public static EmailValue of(final String rawValue) {
        final String normalizedValue = normalize(rawValue);
        validate(normalizedValue, rawValue);
        return new EmailValue(normalizedValue);
    }

    private static String normalize(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or blank");
        }
        return rawValue.trim().toLowerCase(Locale.ROOT);
    }

    private static void validate(final String normalizedValue, final String rawValue) {
        if (normalizedValue.length() > 254) {
            throw new IllegalArgumentException("Email too long (max 254 characters per RFC 5321)");
        }

        if (!normalizedValue.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Invalid email format: " + rawValue);
        }
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof EmailValue other)) return false;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
```

**✅ Vérification :** Aucun import de `jakarta.*` ou `org.springframework.*`

---

### Étape 3 : Créer EmailEmbeddable (infrastructure)

**Fichier :** `src/main/java/com/mk/contractservice/infrastructure/persistence/embeddable/EmailEmbeddable.java`

```java
package com.mk.contractservice.infrastructure.persistence.embeddable;

import com.mk.contractservice.domain.valueobject.EmailValue;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * JPA embeddable for EmailValue.
 * Lives in infrastructure - contains JPA annotations.
 */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailEmbeddable {

    @Column(name = "email", nullable = false, length = 254)
    private String value;

    public EmailEmbeddable(final EmailValue emailValue) {
        this.value = emailValue.value();
    }

    public EmailValue toDomain() {
        return EmailValue.of(this.value);
    }

    // Package-private getter for JPA
    String getValue() {
        return value;
    }
}
```

---

### Étape 4 : Adapter Client (domain)

**Avant :**
```java
@Embedded
@NotNull
@Valid
private Email email;
```

**Après :**
```java
// Plus d'annotation JPA dans le domain
private EmailValue email;

protected Client(final ClientName name, final EmailValue email, final PhoneNumber phone) {
    // ...
    this.email = email;
}

public EmailValue getEmail() {
    return email;
}

public void updateCommonFields(final ClientName name, final EmailValue email, final PhoneNumber phone) {
    // ...
    this.email = email;
}
```

---

### Étape 5 : Créer ClientJpaEntity (infrastructure)

**Nouveau fichier :** `src/main/java/com/mk/contractservice/infrastructure/persistence/entity/ClientJpaEntity.java`

```java
package com.mk.contractservice.infrastructure.persistence.entity;

import com.mk.contractservice.infrastructure.persistence.embeddable.EmailEmbeddable;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "client", schema = "contracts")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class ClientJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Embedded
    private EmailEmbeddable email;

    @Column(name = "phone", nullable = false)
    private String phone;

    // Package-private getters/setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public EmailEmbeddable getEmail() { return email; }
    public void setEmail(EmailEmbeddable email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
```

---

### Étape 6 : Créer PersonJpaEntity

```java
package com.mk.contractservice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "person", schema = "contracts")
@DiscriminatorValue("PERSON")
@PrimaryKeyJoinColumn(name = "id")
public class PersonJpaEntity extends ClientJpaEntity {

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
}
```

---

### Étape 7 : Créer le Mapper

**Fichier :** `src/main/java/com/mk/contractservice/infrastructure/persistence/mapper/ClientMapper.java`

```java
package com.mk.contractservice.infrastructure.persistence.mapper;

import com.mk.contractservice.domain.client.Client;
import com.mk.contractservice.domain.client.Person;
import com.mk.contractservice.domain.valueobject.*;
import com.mk.contractservice.infrastructure.persistence.embeddable.EmailEmbeddable;
import com.mk.contractservice.infrastructure.persistence.entity.*;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

    public Person toDomain(PersonJpaEntity entity) {
        return new Person(
            ClientName.of(entity.getName()),
            entity.getEmail().toDomain(),  // EmailEmbeddable → EmailValue
            PhoneNumber.of(entity.getPhone()),
            PersonBirthDate.of(entity.getBirthDate())
        );
    }

    public PersonJpaEntity toEntity(Person domain, UUID id) {
        PersonJpaEntity entity = new PersonJpaEntity();
        entity.setId(id);
        updateEntity(entity, domain);
        return entity;
    }

    public void updateEntity(PersonJpaEntity entity, Person domain) {
        entity.setName(domain.getName().value());
        entity.setEmail(new EmailEmbeddable(domain.getEmail()));  // EmailValue → EmailEmbeddable
        entity.setPhone(domain.getPhone().value());
        entity.setBirthDate(domain.getBirthDate().value());
    }
}
```

---

### Étape 8 : Adapter le Repository

**Avant :**
```java
@Override
public Client save(final Client c) {
    return jpa.save(c);
}
```

**Après :**
```java
@Override
public Client save(final Client c) {
    if (c instanceof Person person) {
        PersonJpaEntity entity;
        
        // Check si update ou create
        UUID id = extractId(person);  // À implémenter
        
        if (id != null && jpa.existsById(id)) {
            entity = (PersonJpaEntity) jpa.findById(id).orElseThrow();
            mapper.updateEntity(entity, person);
        } else {
            entity = mapper.toEntity(person, UUID.randomUUID());
        }
        
        PersonJpaEntity saved = jpa.save(entity);
        return mapper.toDomain(saved);
    }
    
    // Handle Company similarly
    throw new UnsupportedOperationException("Company mapping not implemented yet");
}
```

---

### Étape 9 : Adapter les tests

**Test Email pur (déjà OK) :**
```java
@Test
void shouldCreateEmailWithValidFormat() {
    EmailValue email = EmailValue.of("test@example.com");
    assertThat(email.value()).isEqualTo("test@example.com");
}
```

**Test Person avec EmailValue :**
```java
@Test
void shouldCreatePersonWithEmailValue() {
    EmailValue email = EmailValue.of("john@example.com");
    Person person = new Person(
        ClientName.of("John"),
        email,  // EmailValue au lieu de Email
        PhoneNumber.of("+33123456789"),
        PersonBirthDate.of(LocalDate.of(1990, 1, 1))
    );
    
    assertThat(person.getEmail()).isEqualTo(email);
}
```

---

## 🧪 Tests de Validation

### 1. Tests unitaires domain

```bash
mvn test -Dtest=EmailValueTest
mvn test -Dtest=PersonTest
mvn test -Dtest=ClientTest
```

**Résultat attendu :** ✅ Tous verts, exécution < 1s

### 2. Tests d'intégration

```bash
mvn test -Dtest=ClientApplicationServiceTest
```

**Résultat attendu :** ✅ Mapping fonctionne, sauvegarde OK

### 3. Vérification BDD

```sql
-- Vérifier que la colonne email existe toujours
SELECT * FROM contracts.client LIMIT 1;

-- La structure de la table ne change PAS
```

---

## 📊 Métriques à Mesurer

### Avant

```
Fichiers modifiés     : 1 (Email.java)
Lignes de code        : ~60 lignes
Annotations JPA       : 2 (@Embeddable, @Column)
Imports jakarta.*     : 2
Tests exécution       : ~2s (Spring context)
```

### Après

```
Fichiers créés        : 3 (EmailValue, EmailEmbeddable, ClientMapper)
Lignes de code        : ~150 lignes (+150%)
Annotations JPA       : 1 (dans EmailEmbeddable uniquement)
Imports jakarta.*     : 0 dans le domain
Tests exécution       : ~0.1s (POJO pur)
```

---

## ✅ Critères de Succès

- [ ] EmailValue n'a AUCUN import jakarta.*
- [ ] Tests domain s'exécutent en < 1s
- [ ] Tests d'intégration passent
- [ ] Schéma BDD inchangé
- [ ] Aucune régression fonctionnelle
- [ ] Code compile sans warning
- [ ] Coverage maintenu (>80%)

---

## 🎯 Décision Go/No-Go

### ✅ GO si :

1. ✅ Tous les tests passent
2. ✅ Performance acceptable (+50 lignes tolérables)
3. ✅ Équipe comprend le pattern
4. ✅ Pas de bug introduit

**→ Continuer avec Person entière (étape suivante)**

### ❌ NO-GO si :

1. ❌ Trop complexe à comprendre
2. ❌ Bugs difficiles à résoudre
3. ❌ Performance dégradée (mapping trop lent)
4. ❌ Équipe refuse le changement

**→ Revenir en arrière :**
```bash
git checkout main
git branch -D poc/clean-architecture-email
```

---

## 📈 Prochaines Étapes (si GO)

1. ✅ EmailValue migré
2. 🎯 Migrer PhoneNumber (1h)
3. 🎯 Migrer ClientName (1h)
4. 🎯 Migrer tous les Value Objects (1 jour)
5. 🎯 Migrer Person complète (2 jours)
6. 🎯 Décision finale : continuer ou arrêter

---

## 🔄 Rollback (si besoin)

```bash
# Option 1 : Revenir au commit précédent
git checkout main
git branch -D poc/clean-architecture-email

# Option 2 : Garder le code pour référence future
git checkout main
git merge --no-commit poc/clean-architecture-email
git reset --hard HEAD
```

---

## 📝 Notes pour l'équipe

### Avantages observés :
- [ ] Domain pur (notez vos impressions)
- [ ] Tests plus rapides ? (mesurez le temps)
- [ ] Plus facile à comprendre ?
- [ ] ...

### Inconvénients observés :
- [ ] Plus de fichiers
- [ ] Mapping complexe ?
- [ ] ...

### Questions ouvertes :
- Comment gérer les IDs ?
- Faut-il mapper Company aussi ?
- ...

---

**Date de création POC :** 2025-10-31  
**Créateur :** GitHub Copilot  
**Durée estimée :** 1-2h  
**Statut :** 🟡 À valider

