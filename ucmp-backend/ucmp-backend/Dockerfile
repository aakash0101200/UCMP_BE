# Build stage using Eclipse Temurin (Official successor to OpenJDK)
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Match your specific project folder structure
COPY ucmp-backend/ucmp-backend/pom.xml .
COPY ucmp-backend/ucmp-backend/src ./src

RUN mvn clean package -DskipTests -B

# Runtime stage using a stable, smaller JRE image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]