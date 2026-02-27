# =============================================================================
# Dockerfile — mock-data-system
# Build context: platform-core-api/  (set by docker-compose context: .)
#
# Dependency chain (mvn -pl mock-data-system -am):
#   utilities → infra-common → multi-tenant-data → notification-system
#   → security → user-management → course-management → billing
#   → tenant-management → pos-system → mock-data-system
# =============================================================================

# ── Build stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Layer 1: POMs only (rarely change → good cache hit rate)
COPY pom.xml /app/
COPY billing/pom.xml               billing/
COPY course-management/pom.xml     course-management/
COPY infra-common/pom.xml          infra-common/
COPY mock-data-system/pom.xml      mock-data-system/
COPY multi-tenant-data/pom.xml     multi-tenant-data/
COPY notification-system/pom.xml   notification-system/
COPY pos-system/pom.xml            pos-system/
COPY security/pom.xml              security/
COPY tenant-management/pom.xml     tenant-management/
COPY user-management/pom.xml       user-management/
COPY utilities/pom.xml             utilities/
# Placeholder POMs for reactor completeness (no src needed)
COPY application/pom.xml           application/
COPY audit-system/pom.xml          audit-system/
COPY certificate-authority/pom.xml certificate-authority/
COPY etl-system/pom.xml            etl-system/

RUN apk add --no-cache maven
RUN mvn dependency:go-offline -pl mock-data-system -am -DskipTests -q || true

# Layer 2: Source trees — ONLY modules in the mock-data-system dependency chain
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
COPY mock-data-system/src       mock-data-system/src

RUN mvn clean install -pl mock-data-system -am -DskipTests -DskipITs -q

# ── Runtime stage ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
RUN apk update && apk add --no-cache bash

WORKDIR /app
COPY --from=build /app/mock-data-system/target/*.jar /app.jar
COPY mock-data-system/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

EXPOSE 8180

ENTRYPOINT ["/app/docker-entrypoint.sh"]
