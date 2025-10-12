# Stage 1: Build the JAR
FROM openjdk:17-jdk-slim AS build
WORKDIR /app

# Copy Maven wrapper & pom
COPY mvnw pom.xml ./
COPY .mvn/ .mvn

# Download dependencies
RUN ./mvnw dependency:resolve

# Copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the app
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

# Expose your application port
EXPOSE 8081

# Start the applicatio
ENTRYPOINT ["java","-jar","app.jar"]
