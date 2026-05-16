# Couplage avec vs sans Domain Events : Comparaison Concrète

## 🎯 TA QUESTION : "Avec Domain Events, on serait moins couplé à Contract ?"

**✅ OUI, EXACTEMENT !**

Laisse-moi te montrer la différence concrète.

---

## 📊 Comparaison : AVEC vs SANS Domain Events

### ❌ SANS Domain Events (Couplage fort)

```java
// application/client/ClientApplicationService.java
@Service
public class ClientApplicationService {
    
    private final ClientRepository clientRepo;
    private final ContractApplicationService contractService;  // ← DÉPENDANCE DIRECTE
    
    public ClientApplicationService(
            ClientRepository clientRepo,
            ContractApplicationService contractService) {  // ← Injecté obligatoirement
        this.clientRepo = clientRepo;
        this.contractService = contractService;
    }
    
    @Transactional
    public void deleteClient(UUID id) {
        // Client connaît Contract et doit l'appeler directement
        contractService.closeActiveContractsByClientId(id);  // ← COUPLAGE FORT
        clientRepo.deleteById(id);
    }
}
```

**Problèmes du couplage fort :**

1. **Dépendance compile-time**
   ```
   ClientApplicationService
          ↓ dépend de
   ContractApplicationService
   ```
   - ❌ Client ne peut PAS être compilé/déployé sans Contract
   - ❌ Impossible de tester Client sans mocker Contract

2. **Modification en cascade**
   ```java
   // Si ContractApplicationService change sa signature :
   void closeActiveContractsByClientId(UUID clientId, String reason);  // ← Ajout paramètre
   
   // Il FAUT modifier ClientApplicationService :
   contractService.closeActiveContractsByClientId(id, "CLIENT_DELETED");  // ← Obligé
   ```

3. **Impossibilité d'extension**
   ```java
   // Si demain on veut aussi :
   // - Fermer les polices d'assurance
   // - Archiver les documents
   // - Notifier les agents
   
   @Transactional
   public void deleteClient(UUID id) {
       contractService.closeActiveContractsByClientId(id);      // ← Ligne 1
       policyService.cancelPoliciesByClientId(id);              // ← Ligne 2
       documentService.archiveDocumentsByClientId(id);          // ← Ligne 3
       notificationService.notifyAgentsOfClientDeletion(id);    // ← Ligne 4
       clientRepo.deleteById(id);
   }
   ```
   - ❌ ClientApplicationService devient un **God Service** (connaît tout le monde)
   - ❌ Violation du principe de responsabilité unique
   - ❌ Chaque ajout nécessite modifier ClientApplicationService

4. **Test complexe**
   ```java
   @Test
   void testDeleteClient() {
       // Il faut mocker TOUS les services dépendants
       ContractApplicationService contractMock = mock(ContractApplicationService.class);
       PolicyService policyMock = mock(PolicyService.class);
       DocumentService documentMock = mock(DocumentService.class);
       NotificationService notificationMock = mock(NotificationService.class);
       
       ClientApplicationService service = new ClientApplicationService(
           clientRepo, contractMock, policyMock, documentMock, notificationMock
       );
       
       service.deleteClient(clientId);
       
       // Vérifier tous les appels
       verify(contractMock).closeActiveContractsByClientId(clientId);
       verify(policyMock).cancelPoliciesByClientId(clientId);
       verify(documentMock).archiveDocumentsByClientId(clientId);
       verify(notificationMock).notifyAgentsOfClientDeletion(clientId);
   }
   ```

---

### ✅ AVEC Domain Events (Couplage faible)

```java
// application/client/ClientApplicationService.java
@Service
public class ClientApplicationService {
    
    private final ClientRepository clientRepo;
    private final ApplicationEventPublisher eventPublisher;  // ← Dépendance générique Spring
    
    public ClientApplicationService(
            ClientRepository clientRepo,
            ApplicationEventPublisher eventPublisher) {  // ← Pas de Contract ici !
        this.clientRepo = clientRepo;
        this.eventPublisher = eventPublisher;
    }
    
    @Transactional
    public void deleteClient(UUID id) {
        clientRepo.deleteById(id);
        
        // Client ne connaît PAS Contract, il publie juste un fait
        eventPublisher.publishEvent(new ClientDeletedEvent(id));  // ← DÉCOUPLAGE
    }
}
```

```java
// domain/client/event/ClientDeletedEvent.java
package com.mk.contractservice.domain.client.event;

// Simple DTO d'événement
public record ClientDeletedEvent(UUID clientId) {}
```

```java
// application/contract/listener/ClientEventListener.java
@Component
public class ClientEventListener {
    
    private final ContractApplicationService contractService;
    
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onClientDeleted(ClientDeletedEvent event) {
        // Contract réagit à l'événement, mais Client ne le sait pas
        contractService.closeActiveContractsByClientId(event.clientId());
    }
}
```

**Avantages du découplage faible :**

1. **Zéro dépendance compile-time**
   ```
   ClientApplicationService
          ↓ dépend de
   ApplicationEventPublisher (Spring - générique)
   
   ClientApplicationService  ❌ NE DÉPEND PAS  ContractApplicationService
   ```
   - ✅ Client peut être compilé/déployé sans Contract
   - ✅ Client ne connaît RIEN de Contract

2. **Pas de modification en cascade**
   ```java
   // Si ContractApplicationService change sa signature :
   void closeActiveContractsByClientId(UUID clientId, String reason);
   
   // ClientApplicationService reste INCHANGÉ :
   eventPublisher.publishEvent(new ClientDeletedEvent(id));  // ← Identique
   
   // Seul le listener change :
   @EventListener
   public void onClientDeleted(ClientDeletedEvent event) {
       contractService.closeActiveContractsByClientId(
           event.clientId(), 
           "CLIENT_DELETED"  // ← Changement isolé ici
       );
   }
   ```

3. **Extension facile (Open/Closed Principle)**
   ```java
   // ClientApplicationService reste IDENTIQUE
   @Transactional
   public void deleteClient(UUID id) {
       clientRepo.deleteById(id);
       eventPublisher.publishEvent(new ClientDeletedEvent(id));  // ← Pas de changement
   }
   
   // On ajoute juste de NOUVEAUX listeners :
   
   // Listener 1 : Contract
   @EventListener
   public void onClientDeleted_CloseContracts(ClientDeletedEvent event) {
       contractService.closeActiveContractsByClientId(event.clientId());
   }
   
   // Listener 2 : Policy (NOUVEAU)
   @EventListener
   public void onClientDeleted_CancelPolicies(ClientDeletedEvent event) {
       policyService.cancelPoliciesByClientId(event.clientId());
   }
   
   // Listener 3 : Document (NOUVEAU)
   @EventListener
   public void onClientDeleted_ArchiveDocuments(ClientDeletedEvent event) {
       documentService.archiveDocumentsByClientId(event.clientId());
   }
   
   // Listener 4 : Notification (NOUVEAU)
   @EventListener
   public void onClientDeleted_NotifyAgents(ClientDeletedEvent event) {
       notificationService.notifyAgentsOfClientDeletion(event.clientId());
   }
   ```
   - ✅ ClientApplicationService **jamais modifié**
   - ✅ Chaque listener est **indépendant**
   - ✅ On peut ajouter/supprimer listeners **sans impact**

4. **Tests isolés**
   ```java
   @Test
   void testDeleteClient() {
       // Mock UNIQUEMENT ApplicationEventPublisher (générique)
       ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
       
       ClientApplicationService service = new ClientApplicationService(
           clientRepo, 
           eventPublisher  // ← UN SEUL mock
       );
       
       service.deleteClient(clientId);
       
       // Vérifier l'événement publié (pas les effets de bord)
       verify(eventPublisher).publishEvent(argThat(event -> 
           event instanceof ClientDeletedEvent 
           && ((ClientDeletedEvent) event).clientId().equals(clientId)
       ));
   }
   ```

---

## 🎯 Visualisation du Couplage

### SANS Events (Couplage en étoile)

```
                    ClientApplicationService
                            │
                ┌───────────┼───────────┬───────────┐
                ↓           ↓           ↓           ↓
         ContractService  PolicyService  DocumentService  NotificationService
```

**Problème :** ClientApplicationService connaît TOUT LE MONDE

### AVEC Events (Couplage par médiateur)

```
         ClientApplicationService
                    ↓
            (publie événement)
                    ↓
         ApplicationEventPublisher (Spring)
                    │
        ┌───────────┼───────────┬───────────┐
        ↓           ↓           ↓           ↓
   ContractListener  PolicyListener  DocumentListener  NotificationListener
        ↓           ↓           ↓           ↓
   ContractService  PolicyService  DocumentService  NotificationService
```

**Avantage :** ClientApplicationService connaît UNIQUEMENT ApplicationEventPublisher (abstraction Spring)

---

## 📊 Tableau Comparatif : Couplage

| Critère | SANS Events (Couplage fort) | AVEC Events (Couplage faible) |
|---------|----------------------------|-------------------------------|
| **Dépendance Client → Contract** | ✅ Directe (compile-time) | ❌ Aucune (runtime) |
| **Client connaît Contract ?** | ✅ OUI | ❌ NON (juste l'événement) |
| **Modification Contract impacte Client ?** | ✅ OUI | ❌ NON |
| **Test Client isolé ?** | ❌ NON (mock Contract) | ✅ OUI (mock EventPublisher) |
| **Ajout nouveau listener** | ❌ Modifier Client | ✅ Juste créer listener |
| **Compréhension flux** | ✅ Facile (appel direct) | ⚠️ Moins évident (indirection) |
| **Déploiement indépendant** | ❌ NON | ✅ Possible (avec message broker) |

---

## 🎓 Analogie du Monde Réel

### SANS Events = Appel téléphonique direct

```
👤 Client Service : "Hey Contract Service, j'ai supprimé le client 123, ferme ses contrats."
📞 [appel direct]
👤 Contract Service : "OK, je m'en occupe."
```

**Problème :**
- Client doit **connaître le numéro** de Contract
- Client doit **savoir que** Contract existe
- Si Contract n'est pas disponible, Client est **bloqué**

### AVEC Events = Annonce publique

```
👤 Client Service : "Annonce : Le client 123 a été supprimé." [publie sur haut-parleur]
📢 [événement diffusé]
👤 Contract Service : "J'ai entendu, je ferme les contrats."
👤 Policy Service : "J'ai entendu, j'annule les polices."
👤 Document Service : "J'ai entendu, j'archive les docs."
```

**Avantage :**
- Client ne **connaît personne** spécifiquement
- Client ne **sait pas qui écoute**
- Chacun réagit **indépendamment**
- On peut ajouter des **auditeurs** sans prévenir Client

---

## 💡 En Théorie ET en Pratique

### En THÉORIE : Découplage complet

```java
// Client Aggregate peut vivre sans Contract
package com.mk.contractservice.domain.client;

// Aucun import de com.mk.contractservice.domain.contract
// Aucun import de com.mk.contractservice.application.contract
```

### En PRATIQUE : Dépendance runtime indirecte

```java
// Client publie un événement
eventPublisher.publishEvent(new ClientDeletedEvent(id));

// Contract écoute l'événement
@EventListener
public void onClientDeleted(ClientDeletedEvent event) {
    // Il y a bien une "dépendance" mais au runtime, pas au compile-time
}
```

**Nuance importante :**
- ❌ **Pas de couplage compile-time** (Client ne connaît pas Contract)
- ✅ **Couplage runtime indirect** (via l'événement)
- ✅ **Couplage sémantique** (règle métier : suppression Client → fermeture Contract)

**Mais c'est un "bon couplage" :**
- Client et Contract **restent dans le même BC** (Insurance Management)
- Ils **partagent le même langage ubiquitaire**
- La règle métier **existe vraiment** ("pas de contrats orphelins")

---

## 🎯 Réponse à ta question

### "Avec Domain Events, on serait moins couplé à Contract ?"

**✅ OUI, moins couplé techniquement :**
- Client ne dépend **PAS** de ContractApplicationService
- Client ne connaît **PAS** Contract
- Test de Client **isolé** (pas besoin de mock Contract)
- Modification Contract **sans impact** sur Client

**⚠️ MAIS toujours couplé sémantiquement :**
- La règle métier **existe** : "suppression Client → fermeture Contract"
- C'est une **invariant du domaine** (Insurance Management BC)
- C'est **normal et souhaitable** (c'est le métier)

**Conclusion :**
- **Couplage technique** : ❌ NON (découplé)
- **Couplage sémantique** : ✅ OUI (mais c'est le métier)

**C'est exactement ce qu'on veut !** 🎯

---

## 🚀 Recommandation

Pour ton projet, **utilise Domain Events** pour découpler Client et Contract :

**Pourquoi ?**
1. ✅ Meilleure testabilité
2. ✅ Extension facile (ajout listeners)
3. ✅ Architecture mature (portfolio pro)
4. ✅ Prépare future séparation (si besoin)
5. ✅ Respect principes SOLID (Open/Closed, Single Responsibility)

**Effort :** 1-2 heures de dev

**Gain :** Architecture senior-level 🏆

