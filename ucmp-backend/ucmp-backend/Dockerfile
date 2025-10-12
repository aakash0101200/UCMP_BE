# Stage 1: Build with Maven + Java 21
FROM maven:3.9.3-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn dependency:resolve
RUN mvn clean package -DskipTests

# Stage 2: Run on Java 21
FROM openjdk:21-jdk-slim
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]
