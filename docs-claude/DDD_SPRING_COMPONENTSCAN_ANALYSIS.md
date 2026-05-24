# 🏛️ DDD & Hexagonal Architecture - Spring ComponentScan Strategy

**Date**: 15 Novembre 2025  
**Source**: [Beyond xScratch - Binding Domain to Spring Context](https://beyondxscratch.com/2019/07/28/domaindrivendesign-hexagonalarchitecture-tips-tricks-binding-the-domain-to-the-spring-context-with-componentscan/)  
**Projet**: contract-service  
**Contexte**: Analyse de notre utilisation actuelle de `@Service` dans la couche Domain

---

## 📋 Résumé de l'Article

### Problématique Centrale

L'article aborde le **dilemme architectural** entre :
- **Pureté DDD/Hexagonal** : Domain complètement isolé du framework (pas d'annotations Spring)
- **Pragmatisme** : Utiliser `@Service` dans le domain pour simplifier la configuration Spring

### Position de l'Auteur

L'auteur **défend l'utilisation de `@Component` dans le Domain Layer** avec ces arguments :

1. **Séparation conceptuelle ≠ Séparation physique de modules**
   - Le Domain peut rester conceptuellement isolé même avec annotations Spring
   - Annotations comme `@Component`, `@Service` sont de simples **metadata** (inversion de dépendance)

2. **Pragmatisme vs Purisme**
   - Configuration manuelle de beans pour chaque Domain Service = boilerplate inutile
   - Spring ne pollue pas vraiment le domain (pas de logique métier couplée)

3. **ComponentScan stratégique**
   - Scan uniquement les packages nécessaires
   - Éviter les scan trop larges qui mélangent les couches

---

## 🎯 Stratégie Proposée par l'Article

### Configuration Spring Recommandée

```java
@Configuration
@ComponentScan(basePackages = {
    "com.example.domain",           // ✅ Domain Services
    "com.example.application",      // ✅ Application Services
    "com.example.infrastructure"    // ✅ Infrastructure
})
public class ApplicationConfiguration {
    // Pas besoin de @Bean pour chaque Domain Service
}
```

### Pattern Domain Service avec @Component

```java
// Domain Layer - Acceptable selon l'article
@Component  // ou @Service (stéréotype Spring)
public class ClientDomainService {
    
    private final ClientRepository clientRepository;
    
    // Constructor injection - IoC naturel
    public ClientDomainService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }
    
    // Pure domain logic
    public void ensureEmailIsUnique(Email email) {
        if (clientRepository.existsByEmail(email.value())) {
            throw new ClientAlreadyExistsException(...);
        }
    }
}
```

### Arguments POUR cette approche

✅ **Simplicité**
- Pas de configuration @Bean manuelle pour chaque service
- Auto-wiring naturel de Spring

✅ **Inversion de Dépendance respectée**
- Domain dépend d'interfaces (`ClientRepository`), pas d'implémentation
- `@Component` n'est qu'une metadata, pas de la logique

✅ **Testabilité préservée**
- Domain Services testables sans Spring (constructor injection)
- Tests unitaires avec mocks simples

✅ **Maintenance réduite**
- Ajout de nouveau Domain Service = pas de config supplémentaire
- Moins de code boilerplate

---

## 🔍 Application à Notre Projet

### Notre Situation Actuelle

```java
// domain/client/ClientService.java
@Service  // ❌ Identifié comme "violation" dans l'audit
public class ClientService {
    private final ClientRepository clientRepository;
    
    public ClientService(final ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }
    
    public Person createPerson(...) {
        ensureEmailIsUnique(email);
        return Person.builder()...build();
    }
    
    public void ensureEmailIsUnique(Email email) {
        if (clientRepository.existsByEmail(email.value())) {
            throw new ClientAlreadyExistsException(...);
        }
    }
}
```

### Analyse selon les Critères de l'Article

| Critère | Notre Code | Conforme Article |
|---------|------------|------------------|
| **Annotation Spring dans Domain** | ✅ `@Service` | ✅ Acceptable |
| **Dépendances Domain → Infrastructure** | ✅ Interface uniquement | ✅ Correct |
| **Logique métier pure** | ✅ Aucune logique Spring | ✅ Correct |
| **Testabilité sans Spring** | ✅ Constructor injection | ✅ Correct |
| **ComponentScan ciblé** | ⚠️ À vérifier | ⚠️ Amélioration possible |

**Verdict** : Notre utilisation de `@Service` dans Domain est **ACCEPTABLE** selon les principes de l'article.

---

## ✅ Ce Qui Fonctionne Déjà

### 1. Inversion de Dépendance Respectée

```java
// Domain Layer
public interface ClientRepository {  // ✅ Interface dans Domain
    Optional<Client> findById(UUID id);
    boolean existsByEmail(String email);
}

// Infrastructure Layer
@Repository
public class JpaClientRepository implements ClientRepository {
    // Implémentation JPA
}
```

**Analyse** : ✅ Domain ne dépend PAS de l'infrastructure, uniquement d'abstractions.

### 2. Tests Unitaires Purs

```java
// Test sans Spring Context
class ClientServiceTest {
    
    @Test
    void shouldThrowWhenEmailExists() {
        // Mock du repository (pas de Spring)
        ClientRepository mockRepo = mock(ClientRepository.class);
        when(mockRepo.existsByEmail("test@example.com")).thenReturn(true);
        
        ClientService service = new ClientService(mockRepo);
        
        assertThrows(ClientAlreadyExistsException.class, 
            () -> service.ensureEmailIsUnique(Email.of("test@example.com")));
    }
}
```

**Analyse** : ✅ Testable sans Spring, `@Service` n'impacte pas la testabilité.

### 3. Aucune Logique Spring dans Domain

Notre `ClientService` ne contient :
- ❌ Pas de `@Transactional`
- ❌ Pas de `@Cacheable`
- ❌ Pas de `@Async`
- ✅ Uniquement logique métier pure

**Analyse** : ✅ Respect de la séparation des préoccupations.

---

## ⚠️ Limitations & Points d'Attention

### 1. Dépendance Conceptuelle à Spring

**Problème** :
```java
import org.springframework.stereotype.Service;  // Dépendance Spring
```

**Impact** :
- Migration vers un autre framework nécessite modification du Domain
- Vendor lock-in léger (mais acceptable selon l'article)

**Mitigation** :
- Spring est un standard de facto en entreprise
- Migration improbable dans 99% des cas
- Trade-off pragmatisme vs pureté académique

### 2. Confusion sur la Responsabilité des Couches

**Risque** :
Si `@Service` est autorisé dans Domain, risque de glisser vers :
```java
@Service
public class ClientService {
    
    @Transactional  // ❌ MAUVAIS - responsabilité Application Layer
    public Person createPerson(...) {
        // ...
    }
}
```

**Mitigation** :
- **Règle stricte** : Annotations Spring dans Domain = `@Component`/`@Service` UNIQUEMENT
- Interdire : `@Transactional`, `@Cacheable`, `@Async`, etc.
- Code review + documentation claire

### 3. Tests d'Intégration Spring

**Observation** :
Nos tests domain utilisent parfois le contexte Spring :
```java
@SpringBootTest
class ClientServiceIntegrationTest {
    @Autowired
    private ClientService clientService;  // Injection Spring
}
```

**Analyse** :
- ✅ Tests d'intégration : acceptable
- ⚠️ Tests unitaires domain : devraient rester purs (new ClientService(mockRepo))

---

## 🎯 Recommandations pour Notre Projet

### Option A : Garder `@Service` (Approche Article)

**Action** : **AUCUNE MODIFICATION**

**Justification** :
- ✅ Conforme aux principes de l'article
- ✅ Pragmatique et maintenable
- ✅ Inversion de dépendance respectée
- ✅ Testabilité préservée

**Documentation nécessaire** :
```java
/**
 * Domain Service gérant la logique métier des clients.
 * <p>
 * Note architecturale : Cette classe utilise @Service pour l'intégration Spring,
 * mais reste un pur Domain Service (aucune logique infrastructure).
 * Voir : docs-claude/DDD_SPRING_COMPONENTSCAN_ANALYSIS.md
 */
@Service
public class ClientService {
    // ...
}
```

### Option B : Configuration @Bean Manuelle (Pureté Hexagonale)

**Action** : Supprimer `@Service`, créer configuration

```java
// Domain - Pur Java
public class ClientService {
    private final ClientRepository clientRepository;
    // ...
}

// Infrastructure/Config
@Configuration
public class DomainConfiguration {
    
    @Bean
    public ClientService clientService(ClientRepository clientRepository) {
        return new ClientService(clientRepository);
    }
    
    @Bean
    public ContractService contractService(ContractRepository contractRepository) {
        return new ContractService(contractRepository);
    }
}
```

**Avantages** :
- ✅ Pureté architecturale maximale
- ✅ Domain 100% framework-agnostic

**Inconvénients** :
- ❌ Boilerplate pour chaque nouveau Domain Service
- ❌ Maintenance accrue
- ❌ Gain théorique pour risque pratique minimal

---

## 📊 Matrice de Décision

| Critère | Option A (@Service) | Option B (@Bean) |
|---------|---------------------|------------------|
| **Simplicité** | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| **Pureté DDD** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Maintenabilité** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Testabilité** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Temps de dev** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Vendor lock-in** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

---

## 🔧 Améliorations Possibles (Indépendantes du Choix)

### 1. ComponentScan Ciblé

**Vérifier notre configuration actuelle** :
```java
@SpringBootApplication  // Scan automatique du package base
public class ContractServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContractServiceApplication.class, args);
    }
}
```

**Amélioration possible** :
```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.mk.contractservice.domain",        // Domain Services
    "com.mk.contractservice.application",   // Application Services
    "com.mk.contractservice.infrastructure",// Infrastructure
    "com.mk.contractservice.web"            // Controllers
})
public class ContractServiceApplication {
    // Configuration explicite des couches scannées
}
```

**Avantage** : Clarté architecturale, contrôle précis.

### 2. Tests Séparés (Unitaires vs Intégration)

**Structure recommandée** :
```
test/
├── unit/                           # Tests purs (pas de Spring)
│   └── domain/
│       ├── ClientServiceTest.java  // new ClientService(mock)
│       └── ContractServiceTest.java
└── integration/                    # Tests avec Spring
    ├── ClientLifecycleIT.java      // @SpringBootTest
    └── ContractLifecycleIT.java
```

**Implémentation** :
```java
// Tests unitaires - Pas de Spring
class ClientServiceUnitTest {
    private ClientRepository mockRepo;
    private ClientService service;
    
    @BeforeEach
    void setUp() {
        mockRepo = mock(ClientRepository.class);
        service = new ClientService(mockRepo);  // Construction manuelle
    }
    
    @Test
    void shouldValidateEmailUniqueness() {
        // Test pur sans @SpringBootTest
    }
}
```

### 3. Documentation Architecturale

**Créer un ADR (Architecture Decision Record)** :
```markdown
# ADR-003: Utilisation de @Service dans Domain Layer

## Statut
Accepté

## Contexte
Domain Services nécessitent injection de dépendances (Repositories).
Deux approches possibles : @Service ou @Bean manuel.

## Décision
Utiliser @Service dans Domain Layer pour les Domain Services.

## Conséquences
- Positives : Simplicité, auto-wiring, maintenance réduite
- Négatives : Dépendance légère à Spring (metadata uniquement)
- Mitigation : Interdire autres annotations Spring dans Domain
```

---

## 📝 Conclusion & Recommandation Finale

### Verdict sur Notre Code Actuel

**Notre utilisation de `@Service` dans Domain est CORRECTE** selon les principes de l'article.

**Justification** :
1. ✅ Inversion de dépendance respectée (interfaces)
2. ✅ Logique métier pure (pas de @Transactional, @Cacheable)
3. ✅ Testabilité sans Spring préservée
4. ✅ Pragmatisme vs purisme académique

### Mise à Jour de l'Audit

**Point #5 de l'audit initial** : 
- ~~Problème : @Service dans Domain~~
- **Correction** : **ACCEPTABLE** avec documentation

**Nouveau statut** :
```
5. Services Domain avec annotation @Service
   Sévérité: 🟢 ACCEPTABLE (avec bonnes pratiques)
   Action: Documenter le choix architectural
   Effort: 15 min (ADR + commentaires)
```

### Actions Recommandées

**Priorité HAUTE** :
1. ✅ Documenter le choix dans un ADR
2. ✅ Ajouter commentaires explicatifs dans `ClientService` et `ContractService`
3. ✅ Établir règle : uniquement `@Service`/`@Component` autorisés dans Domain

**Priorité BASSE** :
4. ⚠️ Vérifier ComponentScan (probablement déjà correct)
5. ⚠️ Séparer tests unitaires purs vs tests d'intégration Spring

**NON RECOMMANDÉ** :
- ❌ Refactorer vers @Bean manuel (gain théorique minimal, effort important)

---

## 🔗 Références

- [Article source](https://beyondxscratch.com/2019/07/28/domaindrivendesign-hexagonalarchitecture-tips-tricks-binding-the-domain-to-the-spring-context-with-componentscan/)
- [Domain-Driven Design - Eric Evans](https://www.domainlanguage.com/ddd/)
- [Hexagonal Architecture - Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Spring Framework Annotations](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html)

---

**Auteur** : GitHub Copilot  
**Révision** : Basée sur analyse de l'article + contexte projet contract-service

