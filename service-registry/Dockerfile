FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY service-registry/pom.xml service-registry/
RUN mvn -f service-registry/pom.xml dependency:go-offline -B

COPY service-registry/src service-registry/src
RUN mvn -f service-registry/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/service-registry/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]