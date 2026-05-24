# Guide de Build Docker - contract-service

## 🚀 Build rapide

### 1. Builder l'image Docker

```bash
# À la racine du projet
docker build -t contract-service:latest .
```

**Avec tag de version** :
```bash
docker build -t contract-service:2.0.0 .
```

### 2. Lancer le container

```bash
docker-compose up -d
```

Ou avec un container seul :
```bash
docker run -p 8080:8080 contract-service:latest
```

---

## 📦 Architecture de l'image

### Image optimisée actuelle
- **Base** : `eclipse-temurin:21-jre-alpine`
- **Taille** : ~405 MB
- **Optimisation** : Layered JARs activé

### Layers Docker (du plus stable au plus changeant)
1. **dependencies/** - Bibliothèques externes (Jackson, Hibernate, etc.)
2. **spring-boot-loader/** - Classes Spring Boot loader
3. **snapshot-dependencies/** - Dépendances SNAPSHOT
4. **application/** - Votre code source

**Avantage** : Seule la couche `application/` est reconstruite quand vous modifiez votre code.

---

## 🔧 Commandes utiles

### Build Maven avant Docker
```bash
mvnw clean package -DskipTests
```

### Build avec tests
```bash
mvnw clean package
docker build -t contract-service:latest .
```

### Voir les layers de l'image
```bash
docker history contract-service:latest
```

### Inspecter l'image
```bash
docker inspect contract-service:latest
```

### Supprimer l'ancienne image
```bash
docker rmi contract-service:latest
```

---

## 🐳 Docker Compose

### Démarrer tous les services
```bash
docker-compose up -d
```

### Voir les logs
```bash
docker-compose logs -f contract-service
```

### Arrêter
```bash
docker-compose down
```

### Rebuild et redémarrer
```bash
docker-compose up -d --build
```

---

## 📝 Configuration dans pom.xml

Le Layered JAR est activé dans le plugin Spring Boot :

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <layers>
            <enabled>true</enabled>
        </layers>
    </configuration>
</plugin>
```

---

## 📚 Documentation complète

Pour plus de détails sur l'optimisation Docker, voir :
- **docs/DOCKER_OPTIMIZATION.md** - Guide complet avec explications détaillées

---

## 🎯 Workflow complet de déploiement

```bash
# 1. Tests
mvnw clean test

# 2. Build JAR
mvnw package -DskipTests

# 3. Build image Docker
docker build -t contract-service:2.0.0 .

# 4. Tag pour registry (optionnel)
docker tag contract-service:2.0.0 registry.example.com/contract-service:2.0.0

# 5. Push (optionnel)
docker push registry.example.com/contract-service:2.0.0

# 6. Déployer
docker-compose up -d
```

---

## ⚠️ Troubleshooting

### Erreur "Cannot find main class"
→ Vérifiez que le JAR est bien créé : `ls -la target/*.jar`

### Image trop grosse
→ Utilisez Alpine : `eclipse-temurin:21-jre-alpine`

### Build lent
→ Vérifiez que les layers sont bien configurées (voir pom.xml)

### Container ne démarre pas
→ Vérifiez les logs : `docker logs <container-id>`

