# syntax=docker/dockerfile:1
# Build from the repository root (kafka-common is a shared module):
#   docker build -f api-gateway/Dockerfile -t api-gateway .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl api-gateway -am package -DskipTests

FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /build/api-gateway/target/*.jar app.jar
USER app
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
