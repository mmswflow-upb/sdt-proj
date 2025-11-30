## Build the reservation-service jar using Maven in a multi-stage build
FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app

# Copy Maven descriptor and source code
COPY pom.xml .
COPY src ./src

# Package the application (skip tests for faster build). The jar will be placed in target/
RUN mvn -q -B package -DskipTests

## Run stage: use a slim JRE to run the built jar
# FROM eclipse-temurin:21-jre-slim
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port the Spring Boot application listens on
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]