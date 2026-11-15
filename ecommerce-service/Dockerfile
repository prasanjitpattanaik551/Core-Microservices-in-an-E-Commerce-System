FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY ecommerce-service/pom.xml ecommerce-service/
RUN mvn -f ecommerce-service/pom.xml dependency:go-offline -B

COPY ecommerce-service/src ecommerce-service/src
RUN mvn -f ecommerce-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/ecommerce-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]