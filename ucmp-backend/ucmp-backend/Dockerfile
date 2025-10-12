# Build stage using community OpenJDK 21 Maven image
FROM smetecnologia/maven:3.9.6-openjdk-21 AS builder
WORKDIR /app

COPY ucmp-backend/ucmp-backend/pom.xml .
COPY ucmp-backend/ucmp-backend/src ./src

RUN mvn clean package -DskipTests -B
# Runtime stage using official OpenJDK 21 image
FROM openjdk:21-jdk-slim
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
