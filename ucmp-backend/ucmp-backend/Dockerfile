FROM maven:3.9.3-eclipse-temurin-21 AS builder
WORKDIR /app

COPY ucmp-backend/ucmp-backend/pom.xml .
COPY ucmp-backend/ucmp-backend/src ./src

RUN mvn clean package -DskipTests

RUN mvn -B dependency:resolve

FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]
