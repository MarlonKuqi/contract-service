# Documentation Claude - Index

Ce dossier contient la documentation générée lors des sessions de développement avec Claude.

## 📁 Organisation

### 🔧 Configuration et Architecture

#### Pagination
- **[PAGINATION_ARCHITECTURE.md](PAGINATION_ARCHITECTURE.md)** - Architecture complète du système de pagination
- **[PAGINATION_FLOW_EXPLAINED.md](PAGINATION_FLOW_EXPLAINED.md)** - Explication détaillée du flux de validation de pagination
- **[PAGINATION_FIX_SUMMARY.md](PAGINATION_FIX_SUMMARY.md)** - Corrections appliquées aux tests de pagination

#### Tests et Contexte Spring
- **[SPRING_CONTEXT_FIX.md](SPRING_CONTEXT_FIX.md)** - Fix du contexte Spring qui se chargeait deux fois

#### Docker
- **[DOCKER_BUILD_GUIDE.md](DOCKER_BUILD_GUIDE.md)** - Guide rapide pour builder l'image Docker
- Voir aussi : **../docs/DOCKER_OPTIMIZATION.md** - Guide complet d'optimisation Docker

---

## 🎯 Guides rapides

### Comment builder l'image Docker ?
```bash
docker build -t contract-service:latest .
```
→ Voir [DOCKER_BUILD_GUIDE.md](DOCKER_BUILD_GUIDE.md)

### Comment fonctionne la validation de pagination ?
→ Voir [PAGINATION_FLOW_EXPLAINED.md](PAGINATION_FLOW_EXPLAINED.md)

### Pourquoi le contexte Spring se chargeait deux fois ?
→ Voir [SPRING_CONTEXT_FIX.md](SPRING_CONTEXT_FIX.md)

---

## 📚 Documentation principale du projet

Pour la documentation principale et les guides architecturaux, voir le dossier **../docs/** :
- `DDD_SERVICES_ARCHITECTURE.md` - Architecture DDD des services
- `DOCKER_OPTIMIZATION.md` - Optimisation Docker détaillée
- `MIGRATION_GUIDE_V2.md` - Guide de migration vers v2

---

## 📝 Notes

Cette documentation est générée automatiquement lors des sessions de développement et de refactoring.
Elle sert de référence pour les décisions techniques et les correctifs appliqués.

**Date de dernière mise à jour** : 2025-11-15

