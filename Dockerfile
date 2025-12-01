# ========================================
# STAGE 1: Build the application JAR with layers
# ========================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application
COPY src ./src
RUN mvn clean package -DskipTests

# Extract Spring Boot layers for optimal caching
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ========================================
# STAGE 2: Create custom JRE with jlink (minimal modules)
# ========================================
FROM eclipse-temurin:21-jdk-alpine AS jre-builder

RUN jlink \
    --add-modules java.base,java.logging,java.naming,java.sql,java.management,java.xml,java.instrument,jdk.unsupported,jdk.crypto.ec \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --compress=2 \
    --output /custom-jre

# ========================================
# STAGE 3: Runtime image
# ========================================
FROM alpine:3.22.2
WORKDIR /app

# Create non-root user
RUN addgroup -S appuser && \
    adduser -S appuser -G appuser && \
    mkdir -p /app/logs && \
    chown -R appuser:appuser /app

COPY --from=jre-builder /custom-jre /opt/jre

# Copy Spring Boot layers (optimized caching)
COPY --from=build --chown=appuser:appuser /app/target/extracted/dependencies/ ./
COPY --from=build --chown=appuser:appuser /app/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=appuser:appuser /app/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=appuser:appuser /app/target/extracted/application/ ./

ENV PATH="/opt/jre/bin:${PATH}"

# Switch to non-root user
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+TieredCompilation", \
    "-XX:TieredStopAtLevel=1", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]

