# Documentation Claude - Index

Ce dossier contient la documentation générée lors des sessions de développement avec Claude.

---

## 🆕 AUDIT DDD COMPLET (2025-12-17)

### 🚀 START HERE

#### **[DDD_QUICK_START.md](DDD_QUICK_START.md)** ⚡⚡⚡
**LA PREMIÈRE CHOSE À LIRE (5 min)** :
- Dois-je implémenter les changements DDD ?
- Arbre de décision selon ton contexte
- Recommandations personnalisées
- **→ Commence absolument par ce document !**

#### **[DDD_AUDIT_INDEX.md](DDD_AUDIT_INDEX.md)** 📖
Index complet de l'audit DDD :
- Vue d'ensemble de tous les documents
- Guidance selon ton temps disponible
- Score global : **72/100** → **88/100**
- Plan d'action (9h de travail)

### 📊 Documents Essentiels

#### **[DDD_EXECUTIVE_SUMMARY.md](DDD_EXECUTIVE_SUMMARY.md)** ⚡
**Lecture rapide (5-10 min)** :
- Points forts et manques du projet
- Recommandations prioritaires
- Nom du projet : `insurance-policy-service`

#### **[DDD_COMPLETE_AUDIT.md](DDD_COMPLETE_AUDIT.md)** 🔍
**Analyse approfondie (30-45 min)** :
- Audit détaillé de chaque pattern DDD
- Exemples avant/après
- Justifications architecturales

### 🛠️ Documents Techniques

#### **[DDD_REFACTORING_GUIDE.md](DDD_REFACTORING_GUIDE.md)** 🔧
**Guide pratique (1-2h)** :
- Code complet Domain Events
- Code complet Factories
- Tests à ajouter

#### **[DDD_FOLDER_STRUCTURE_DECISION.md](DDD_FOLDER_STRUCTURE_DECISION.md)** 🗂️
**Organisation (10-15 min)** :
- Dossiers DDD vs techniques
- 3 approches comparées
- Recommandation : Hybride

#### **[DDD_ARCHITECTURE_DIAGRAM.md](DDD_ARCHITECTURE_DIAGRAM.md)** 🏗️
**Visualisation (15 min)** :
- Architecture avant/après
- Flux détaillé avec Domain Events
- Comparaison testabilité

---

## 📁 Organisation (Documents Historiques)

### 🔧 Configuration et Architecture

#### Pagination
- **[PAGINATION_ARCHITECTURE.md](../../docs-claude/PAGINATION_ARCHITECTURE.md)** - Architecture complète du système de pagination
- **[PAGINATION_FLOW_EXPLAINED.md](../../docs-claude/PAGINATION_FLOW_EXPLAINED.md)** - Explication détaillée du flux de validation de pagination
- **[PAGINATION_FIX_SUMMARY.md](../../docs-claude/PAGINATION_FIX_SUMMARY.md)** - Corrections appliquées aux tests de pagination

#### Tests et Contexte Spring
- **[SPRING_CONTEXT_FIX.md](../../docs-claude/SPRING_CONTEXT_FIX.md)** - Fix du contexte Spring qui se chargeait deux fois

#### Docker
- **[DOCKER_BUILD_GUIDE.md](../../docs-claude/DOCKER_BUILD_GUIDE.md)** - Guide rapide pour builder l'image Docker
- Voir aussi : **../docs/DOCKER_OPTIMIZATION.md** - Guide complet d'optimisation Docker

---

## 🎯 Guides rapides

### Comment builder l'image Docker ?
```bash
docker build -t contract-service:latest .
```
→ Voir [DOCKER_BUILD_GUIDE.md](../../docs-claude/DOCKER_BUILD_GUIDE.md)

### Comment fonctionne la validation de pagination ?
→ Voir [PAGINATION_FLOW_EXPLAINED.md](../../docs-claude/PAGINATION_FLOW_EXPLAINED.md)

### Pourquoi le contexte Spring se chargeait deux fois ?
→ Voir [SPRING_CONTEXT_FIX.md](../../docs-claude/SPRING_CONTEXT_FIX.md)

---

## 📚 Documentation principale du projet

Pour la documentation principale et les guides architecturaux, voir le dossier **../docs/** :
- `DDD_SERVICES_ARCHITECTURE.md` - Architecture DDD des services
- `DOCKER_OPTIMIZATION.md` - Optimisation Docker détaillée
- `MIGRATION_GUIDE_V2.md` - Guide de migration vers v2

**Date de dernière mise à jour** : 2025-12-17 (Audit DDD complet ajouté)

## 📝 Notes

Cette documentation est générée automatiquement lors des sessions de développement et de refactoring.
Elle sert de référence pour les décisions techniques et les correctifs appliqués.

**Date de dernière mise à jour** : 2025-11-15

