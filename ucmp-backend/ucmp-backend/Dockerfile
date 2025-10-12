FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

RUN mvn -B dependency:resolve

FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]
