# =============================================================================
# Dockerfile — platform-core-api (application module)
# Build context: platform-core-api/
#
# Dependency chain (mvn -pl application -am):
#   utilities → infra-common → multi-tenant-data → notification-system
#   → security → user-management → course-management → billing
#   → tenant-management → pos-system → lead-management → application
# =============================================================================

# ── Build stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:24-jdk-alpine AS build
WORKDIR /app

# Layer 1: POMs only (rarely change → good cache hit rate)
# Copy all module POMs so the reactor can resolve inter-module dependencies.
# This is cheap (small files) and avoids reactor resolution failures.
COPY pom.xml /app/
COPY application/pom.xml           application/
COPY billing/pom.xml               billing/
COPY course-management/pom.xml     course-management/
COPY infra-common/pom.xml          infra-common/
COPY multi-tenant-data/pom.xml     multi-tenant-data/
COPY notification-system/pom.xml   notification-system/
COPY security/pom.xml              security/
COPY tenant-management/pom.xml     tenant-management/
COPY user-management/pom.xml       user-management/
COPY utilities/pom.xml             utilities/
# Placeholder POMs for reactor completeness (no src needed)
COPY audit-service/pom.xml          audit-service/
COPY certificate-authority/pom.xml certificate-authority/
COPY etl-service/pom.xml            etl-service/
COPY lead-management/pom.xml       lead-management/
COPY mock-data-service/pom.xml      mock-data-service/
COPY pos-system/pom.xml            pos-system/
COPY task-service/pom.xml          task-service/

RUN apk add --no-cache maven
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
RUN mvn dependency:go-offline -pl application -am -DskipTests -q || true

# Layer 2: Source trees — ONLY modules in the application dependency chain
COPY utilities/src              utilities/src
COPY infra-common/src           infra-common/src
COPY multi-tenant-data/src      multi-tenant-data/src
COPY notification-system/src    notification-system/src
COPY security/src               security/src
COPY user-management/src        user-management/src
COPY course-management/src      course-management/src
COPY billing/src                billing/src
COPY tenant-management/src      tenant-management/src
COPY pos-system/src             pos-system/src
COPY lead-management/src        lead-management/src
COPY task-service/src           task-service/src
COPY application/src            application/src

RUN mvn clean install -pl application -am -DskipTests -DskipITs -q

# ── Runtime stage ────────────────────────────────────────────────────────────
# keytool ships with the JRE (needed for JWT keystore generation in entrypoint)
FROM eclipse-temurin:24-jre-alpine
RUN apk update && apk add --no-cache bash

WORKDIR /app
COPY --from=build /app/application/target/*.jar /app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/app/docker-entrypoint.sh"]
