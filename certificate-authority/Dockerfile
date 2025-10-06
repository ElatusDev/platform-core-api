# ca-service/Dockerfile

# --- Stage 1: The Build Stage ---
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY pom.xml ./
COPY ca-service/pom.xml ./ca-service/pom.xml
COPY ca-service/src ./ca-service/src

RUN apk add --no-cache maven
RUN mvn -f ca-service/pom.xml dependency:go-offline -Pca-service
RUN mvn -f ca-service/pom.xml clean package -Pca-service -DskipTests=true

# --- Stage 2: The Final Runtime Stage ---
FROM eclipse-temurin:21-jre-alpine
RUN apk update && apk add --no-cache bash

WORKDIR /app
COPY --from=build /app/ca-service/target/ca-service-*.jar /app/ca-service.jar
EXPOSE 8081
CMD ["java", "-jar", "/app/ca-service.jar"]