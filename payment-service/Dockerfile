FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY payment-service/pom.xml payment-service/
RUN mvn -f payment-service/pom.xml dependency:go-offline -B

COPY payment-service/src payment-service/src
RUN mvn -f payment-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/payment-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]