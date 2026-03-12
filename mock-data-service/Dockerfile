# =============================================================================
# Dockerfile — mock-data-service
# Build context: platform-core-api/  (set by docker-compose context: .)
#
# Dependency chain (mvn -pl mock-data-service -am):
#   utilities → infra-common → multi-tenant-data → notification-system
#   → security → user-management → course-management → billing
#   → tenant-management → pos-system → lead-management → mock-data-service
# =============================================================================

# ── Build stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:24-jdk-alpine AS build
WORKDIR /app

# Layer 1: POMs only (rarely change → good cache hit rate)
COPY pom.xml /app/
COPY billing/pom.xml               billing/
COPY course-management/pom.xml     course-management/
COPY infra-common/pom.xml          infra-common/
COPY mock-data-service/pom.xml      mock-data-service/
COPY multi-tenant-data/pom.xml     multi-tenant-data/
COPY notification-system/pom.xml   notification-system/
COPY pos-system/pom.xml            pos-system/
COPY security/pom.xml              security/
COPY tenant-management/pom.xml     tenant-management/
COPY user-management/pom.xml       user-management/
COPY utilities/pom.xml             utilities/
# Placeholder POMs for reactor completeness (no src needed)
COPY application/pom.xml           application/
COPY audit-service/pom.xml          audit-service/
COPY certificate-authority/pom.xml certificate-authority/
COPY etl-service/pom.xml            etl-service/
COPY lead-management/pom.xml       lead-management/

RUN apk add --no-cache maven
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
RUN mvn dependency:go-offline -pl mock-data-service -am -DskipTests -q || true

# Layer 2: Source trees — ONLY modules in the mock-data-service dependency chain
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
COPY mock-data-service/src       mock-data-service/src

RUN mvn clean install -pl mock-data-service -am -DskipTests -DskipITs -q

# ── Runtime stage ────────────────────────────────────────────────────────────
FROM eclipse-temurin:24-jre-alpine
RUN apk update && apk add --no-cache bash

WORKDIR /app
COPY --from=build /app/mock-data-service/target/*.jar /app.jar
COPY mock-data-service/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

EXPOSE 8180

ENTRYPOINT ["/app/docker-entrypoint.sh"]
