FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Cache dependencies first
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# certs/ is a volume: the generated TLS keystore survives restarts
RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir /app/certs && chown spring:spring /app/certs
USER spring

COPY --from=build /app/target/*.jar app.jar
COPY docker-entrypoint.sh .

EXPOSE 8443

ENTRYPOINT ["sh", "docker-entrypoint.sh"]
