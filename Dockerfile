ARG BASE_JDK_IMAGE=azul-zulu:27-jdk
ARG BUILD_WORKDIR=/workspace/app
ARG EXTRACT_DIR=target/extracted
ARG JRE_HOME=/opt/java/openjdk
ARG APP_HOME=/app
ARG APP_USER=appuser

# Stage 1: Download dependencies
# Using Azul Zulu JDK - stable, performant, optimized for containers
FROM ${BASE_JDK_IMAGE} AS deps
ARG BUILD_WORKDIR
WORKDIR ${BUILD_WORKDIR}

# Copy Maven wrapper and config files
COPY --chmod=755 mvnw ./mvnw
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies for caching layer
RUN ./mvnw -B dependency:resolve-plugins dependency:go-offline && \
    rm -rf /root/.m2/repository/com/mk/contract-service

# Stage 2: Build application and extract Spring Boot layers
FROM ${BASE_JDK_IMAGE} AS build
ARG BUILD_WORKDIR
ARG EXTRACT_DIR
ARG JRE_HOME
WORKDIR ${BUILD_WORKDIR}

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        binutils && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean

# Copy pre-downloaded dependencies from stage 1
COPY --from=deps /root/.m2 /root/.m2
COPY --from=deps ${BUILD_WORKDIR}/mvnw ./mvnw
COPY --from=deps ${BUILD_WORKDIR}/.mvn .mvn
COPY pom.xml .
COPY src src

# Build the Spring Boot jar, then use jarmode=tools to expose the jar's built-in
# extraction command instead of starting the application.
RUN ./mvnw -B package -DskipTests && \
    java -Djarmode=tools -jar target/*.jar extract \
        --launcher --force --layers dependencies --destination ${EXTRACT_DIR} && \
    java -Djarmode=tools -jar target/*.jar extract \
        --launcher --force --layers spring-boot-loader --destination ${EXTRACT_DIR} && \
    java -Djarmode=tools -jar target/*.jar extract \
        --launcher --force --layers snapshot-dependencies --destination ${EXTRACT_DIR} && \
    java -Djarmode=tools -jar target/*.jar extract \
        --launcher --force --layers application --destination ${EXTRACT_DIR} && \
    : "Analyze compiled classes to determine the minimal set of JDK modules required at runtime" && \
    jdeps \
        --ignore-missing-deps \
        --recursive \
        --multi-release 27 \
        --print-module-deps \
        --class-path "${EXTRACT_DIR}/dependencies/BOOT-INF/lib/*:${EXTRACT_DIR}/snapshot-dependencies/BOOT-INF/lib/*" \
        ${EXTRACT_DIR}/application/BOOT-INF/classes > target/jre-deps.info && \
    : "Build a custom minimal JRE for the application" && \
    : "Explicitly keep jdk.crypto.ec for modern HTTPS/TLS certificates; jdeps may not infer it reliably" && \
    jlink \
        --add-modules "$(cat target/jre-deps.info)",jdk.crypto.ec \
        --strip-debug \
        --no-man-pages \
        --no-header-files \
        --compress=zip-9 \
        --output ${JRE_HOME}

# Stage 3: Runtime image - custom minimal JRE on standard Debian slim
FROM debian:bookworm-slim

ARG BUILD_WORKDIR
ARG EXTRACT_DIR
ARG JRE_HOME
ARG APP_HOME
ARG APP_USER

ENV JAVA_HOME=${JRE_HOME} \
    PATH=${JRE_HOME}/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8

WORKDIR ${APP_HOME}

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
    groupadd --system ${APP_USER} && \
    useradd --system \
        --gid ${APP_USER} \
        --home-dir ${APP_HOME} \
        --create-home \
        --shell /usr/sbin/nologin \
        ${APP_USER} && \
    mkdir -p ${APP_HOME}/logs && \
    chown -R ${APP_USER}:${APP_USER} ${APP_HOME} && \
    chmod 755 ${APP_HOME} && \
    chmod 755 ${APP_HOME}/logs && \
    mkdir -p /tmp && \
    chmod 1777 /tmp

COPY --from=build ${JRE_HOME} ${JRE_HOME}

# Copy Spring Boot layers in order of least to most frequently changing
# This optimizes Docker layer caching for faster builds
COPY --from=build --chown=${APP_USER}:${APP_USER} \
    ${BUILD_WORKDIR}/${EXTRACT_DIR}/dependencies/ ./
COPY --from=build --chown=${APP_USER}:${APP_USER} \
    ${BUILD_WORKDIR}/${EXTRACT_DIR}/spring-boot-loader/ ./
COPY --from=build --chown=${APP_USER}:${APP_USER} \
    ${BUILD_WORKDIR}/${EXTRACT_DIR}/snapshot-dependencies/ ./
COPY --from=build --chown=${APP_USER}:${APP_USER} \
    ${BUILD_WORKDIR}/${EXTRACT_DIR}/application/ ./

# Define volatile volume for temporary data (can be overridden)
VOLUME /tmp

# Health check for orchestrators (Kubernetes, Docker Swarm)
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
    CMD curl --fail --silent --max-time 2 http://127.0.0.1:8080/actuator/health/liveness || exit 1

# Switch to non-root user for security
USER ${APP_USER}

# JVM optimization flags for containerized environments
ENTRYPOINT ["java", \
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

