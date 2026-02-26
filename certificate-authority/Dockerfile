FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Layer 1: Copy ALL module POMs so the Maven reactor can resolve the parent POM
# and BOM-managed dependencies without hitting a remote repo for internal artifacts.
COPY pom.xml /app/
COPY application/pom.xml           application/
COPY audit-system/pom.xml          audit-system/
COPY billing/pom.xml               billing/
COPY certificate-authority/pom.xml certificate-authority/
COPY course-management/pom.xml     course-management/
COPY etl-system/pom.xml            etl-system/
COPY infra-common/pom.xml          infra-common/
COPY mock-data-system/pom.xml      mock-data-system/
COPY multi-tenant-data/pom.xml     multi-tenant-data/
COPY notification-system/pom.xml   notification-system/
COPY pos-system/pom.xml            pos-system/
COPY security/pom.xml              security/
COPY tenant-management/pom.xml     tenant-management/
COPY user-management/pom.xml       user-management/
COPY utilities/pom.xml             utilities/

RUN apk add --no-cache maven
# Pre-fetch dependencies with reactor awareness (-am installs root parent into .m2)
RUN mvn dependency:go-offline -pl certificate-authority -am -DskipTests -q || true

# Layer 2: Source — CA only (no sibling module dependencies)
COPY certificate-authority/src certificate-authority/src

RUN mvn clean install -pl certificate-authority -am -DskipTests -DskipITs -q

# Runtime — non-root user with curl/openssl for healthcheck
FROM eclipse-temurin:21-jre-alpine
RUN apk update && apk add --no-cache bash curl openssl && \
    addgroup -S cagroup && adduser -S causer -G cagroup

WORKDIR /app
COPY --from=build /app/certificate-authority/target/certificate-authority-*.jar \
    /app/ca-service.jar

RUN mkdir -p /certs && chown causer:cagroup /certs
USER causer

EXPOSE 8081 8082
ENTRYPOINT ["java", "-jar", "/app/ca-service.jar"]
