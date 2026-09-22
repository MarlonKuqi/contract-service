# Stage 1: Download dependencies
# Using Azul Zulu JDK - stable, performant, optimized for containers
FROM azul-zulu:27-jdk AS deps
WORKDIR /workspace/app

# Copy Maven wrapper and config files
COPY --chmod=755 mvnw ./mvnw
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies for caching layer
RUN ./mvnw -B dependency:resolve-plugins dependency:go-offline && \
    rm -rf /root/.m2/repository/com/mk/contract-service

# Stage 2: Build application and extract Spring Boot layers
FROM azul-zulu:27-jdk AS build
WORKDIR /workspace/app

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        binutils && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean

# Copy pre-downloaded dependencies from stage 1
COPY --from=deps /root/.m2 /root/.m2
COPY --from=deps /workspace/app/mvnw ./mvnw
COPY --from=deps /workspace/app/.mvn .mvn
COPY pom.xml .
COPY src src

# Build application and extract layers
RUN ./mvnw -B package -DskipTests && \
    java -Djarmode=tools -jar target/*.jar extract \
        --launcher --force --layers dependencies --destination target/extracted && \
    java -Djarmode=tools -jar target/*.jar extract \
        --launcher --force --layers spring-boot-loader --destination target/extracted && \
    java -Djarmode=tools -jar target/*.jar extract \
        --launcher --force --layers snapshot-dependencies --destination target/extracted && \
    java -Djarmode=tools -jar target/*.jar extract \
        --launcher --force --layers application --destination target/extracted && \
    jdeps \
        --ignore-missing-deps \
        --recursive \
        --multi-release 27 \
        --print-module-deps \
        --class-path "target/extracted/dependencies/BOOT-INF/lib/*:target/extracted/snapshot-dependencies/BOOT-INF/lib/*" \
        target/extracted/application/BOOT-INF/classes > target/jre-deps.info && \
    jlink \
        --add-modules "$(cat target/jre-deps.info)",jdk.crypto.ec \
        --strip-debug \
        --no-man-pages \
        --no-header-files \
        --compress=zip-9 \
        --output /opt/java/openjdk

# Stage 3: Runtime image - custom minimal JRE on standard Debian slim
FROM debian:bookworm-slim

ENV JAVA_HOME=/opt/java/openjdk \
    PATH=/opt/java/openjdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8

WORKDIR /app

# Install only essential packages
# - ca-certificates: for HTTPS connections and SSL verification
# - curl: for healthchecks
# - creates a non-root user and runtime directories with restrictive permissions
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        ca-certificates \
        curl && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean && \
    groupadd --system appuser && \
    useradd --system \
        --gid appuser \
        --home-dir /app \
        --create-home \
        --shell /usr/sbin/nologin \
        appuser && \
    mkdir -p /app/logs && \
    chown -R appuser:appuser /app && \
    chmod 755 /app && \
    chmod 755 /app/logs && \
    mkdir -p /tmp && \
    chmod 1777 /tmp

COPY --from=build /opt/java/openjdk /opt/java/openjdk

# Copy Spring Boot layers in order of least to most frequently changing
# This optimizes Docker layer caching for faster builds
COPY --from=build --chown=appuser:appuser \
    /workspace/app/target/extracted/dependencies/ ./
COPY --from=build --chown=appuser:appuser \
    /workspace/app/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=appuser:appuser \
    /workspace/app/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=appuser:appuser \
    /workspace/app/target/extracted/application/ ./

# Define volatile volume for temporary data (can be overridden)
VOLUME /tmp

# Health check for orchestrators (Kubernetes, Docker Swarm)
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
    CMD curl --fail --silent --max-time 2 http://127.0.0.1:8080/actuator/health/liveness || exit 1

# Switch to non-root user for security
USER appuser

# JVM optimization flags for containerized environments
ENTRYPOINT ["/opt/java/openjdk/bin/java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-XX:+TieredCompilation", \
    "-XX:TieredStopAtLevel=1", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/tmp/heap_dump.bin", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "org.springframework.boot.loader.launch.JarLauncher"]

