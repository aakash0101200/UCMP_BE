# Stage 1: Build with Maven on Java 21
FROM eclipse-temurin:21-jdk-slim AS build

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy your Maven config and source
COPY pom.xml .
COPY src ./src

# Build the project
RUN mvn dependency:resolve
RUN mvn clean package -DskipTests

# Stage 2: Run on Java 21
FROM eclipse-temurin:21-jdk-slim
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java","-jar","app.jar"]
