# Stage 1: Build with Maven
FROM maven:3.9.3-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven config and source
COPY pom.xml .
COPY src ./src

# Download dependencies and build
RUN mvn dependency:resolve
RUN mvn clean package -DskipTests

# Stage 2: Run the app
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8081

ENTRYPOINT ["java","-jar","app.jar"]
