# Stage 1: Build the JAR
FROM openjdk:17-jdk-slim AS build
WORKDIR /app

# Copy entire project to include mvnw and .mvn
COPY . .

# Grant execute permission on mvnw
RUN chmod +x mvnw

# Download dependencies and build
RUN ./mvnw dependency:resolve
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the app
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8081

ENTRYPOINT ["java","-jar","app.jar"]
