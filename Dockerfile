# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy parent pom and all module poms for dependency resolution
COPY pom.xml /app/
COPY application/pom.xml application/
COPY audit-system/pom.xml audit-system/
COPY billing/pom.xml billing/
COPY certificate-authority/pom.xml certificate-authority/
COPY course-management/pom.xml course-management/
COPY etl-system/pom.xml etl-system/
COPY infra-common/pom.xml infra-common/
COPY mock-data-system/pom.xml mock-data-system/
COPY multi-tenant-data/pom.xml multi-tenant-data/
COPY notification-system/pom.xml notification-system/
COPY pos-system/pom.xml pos-system/
COPY security/pom.xml security/
COPY tenant-management/pom.xml tenant-management/
COPY user-management/pom.xml user-management/
COPY utilities/pom.xml utilities/

RUN apk add --no-cache maven
RUN mvn dependency:go-offline -DskipTests -q || true

# Copy all source trees
COPY application/src application/src
COPY audit-system/src audit-system/src
COPY billing/src billing/src
COPY course-management/src course-management/src
COPY etl-system/src etl-system/src
COPY infra-common/src infra-common/src
COPY mock-data-system/src mock-data-system/src
COPY multi-tenant-data/src multi-tenant-data/src
COPY notification-system/src notification-system/src
COPY pos-system/src pos-system/src
COPY security/src security/src
COPY tenant-management/src tenant-management/src
COPY user-management/src user-management/src
COPY utilities/src utilities/src

RUN mvn clean install -DskipTests -DskipITs -q

# Runtime stage — includes curl/jq/openssl for docker-entrypoint.sh
FROM eclipse-temurin:21-jre-alpine
RUN apk update && apk add --no-cache bash curl jq openssl

WORKDIR /app
COPY --from=build /app/application/target/*.jar /app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

RUN mkdir -p /app/certs
EXPOSE 8443

ENTRYPOINT ["/app/docker-entrypoint.sh"]
