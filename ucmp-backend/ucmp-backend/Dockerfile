# Stage 1: Build with Maven on Java 21
FROM maven:3.9.3-eclipse-temurin-21-jdk AS build
WORKDIR /app

# Copy project and build
COPY pom.xml .
COPY src ./src
RUN mvn -B dependency:resolve
RUN mvn -B clean package -DskipTests

# Stage 2: Run on Java 21
FROM eclipse-temurin:21-jdk AS runtime
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]
