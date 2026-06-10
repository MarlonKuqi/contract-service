# Stage 1: Download dependencies
FROM eclipse-temurin:21-jdk-alpine AS deps
WORKDIR /workspace/app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B || true

# Stage 2: Build application with layered JAR
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app
COPY --from=deps /root/.m2 /root/.m2
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x mvnw
RUN ./mvnw package -DskipTests
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# Stage 3: Runtime image
FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp

# Create user and directories in one layer to reduce image size
RUN addgroup -S appuser && adduser -S appuser -G appuser

WORKDIR /app

RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# Copy Spring Boot layers in order of least to most frequently changing
# This optimizes Docker layer caching
COPY --from=build --chown=appuser:appuser /workspace/app/target/extracted/dependencies/ ./
COPY --from=build --chown=appuser:appuser /workspace/app/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=appuser:appuser /workspace/app/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=appuser:appuser /workspace/app/target/extracted/application/ ./

# Switch to non-root user
USER appuser

# JVM optimization flags for containerized environments
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+TieredCompilation", \
    "-XX:TieredStopAtLevel=1", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]

