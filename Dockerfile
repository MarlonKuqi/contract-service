FROM eclipse-temurin:21-jdk AS deps
WORKDIR /workspace/app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B || true

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace/app
COPY --from=deps /root/.m2 /root/.m2
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x mvnw
RUN ./mvnw package -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM eclipse-temurin:21-jre
VOLUME /tmp

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Create logs directory
RUN mkdir -p /logs && chown -R appuser:appuser /logs

# Copy application files
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

# Set ownership to non-root user
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

ENTRYPOINT ["java","-cp","app:app/lib/*","com.mk.contractservice.ContractServiceApplication"]

