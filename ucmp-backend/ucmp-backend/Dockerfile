# Build Stage: Maven with Java 17 (latest stable available with JDK)
FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn -B dependency:resolve
RUN mvn -B clean package -DskipTests
# Run Stage: Java 17 runtime image
FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]
