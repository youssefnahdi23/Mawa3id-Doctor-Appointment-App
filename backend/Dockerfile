# syntax=docker/dockerfile:1

# ---- Build stage: compile and package the Spring Boot executable jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Resolve dependencies first so this layer is cached unless pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Package the app. Tests run in CI (mvn verify); the image build just packages.
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Runtime stage: slim JRE with just the jar ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# curl is used by the container HEALTHCHECK below.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --no-create-home --uid 10001 appuser

COPY --from=build /build/target/mawa3id-backend-*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar

USER appuser
EXPOSE 8080

# The actuator health endpoint is public (see config/SecurityConfig).
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
