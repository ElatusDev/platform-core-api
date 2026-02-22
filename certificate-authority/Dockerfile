FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Dependency caching layer
COPY pom.xml ./
COPY certificate-authority/pom.xml ./certificate-authority/
RUN apk add --no-cache maven && \
    mvn -f certificate-authority/pom.xml -pl certificate-authority -am dependency:go-offline -q || true

# Build
COPY certificate-authority/src ./certificate-authority/src
RUN mvn -f certificate-authority/pom.xml -pl certificate-authority -am clean package -DskipTests -q

# Runtime — non-root user with curl/openssl for healthcheck
FROM eclipse-temurin:21-jre-alpine
RUN apk update && apk add --no-cache bash curl openssl && \
    addgroup -S cagroup && adduser -S causer -G cagroup

WORKDIR /app
COPY --from=build /workspace/certificate-authority/target/certificate-authority-*.jar \
    /app/ca-service.jar

RUN mkdir -p /certs && chown causer:cagroup /certs
USER causer

EXPOSE 8081 8082
ENTRYPOINT ["java", "-jar", "/app/ca-service.jar"]
