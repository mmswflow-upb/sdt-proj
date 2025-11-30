## Build the scheduling-service JAR using Maven in a multi-stage build.  The first stage
## uses the official Maven image to compile the project.  The final stage uses a
## lightweight JRE to run the packaged Spring Boot application.  This pattern
## mirrors the reservation-service Dockerfile so each service can be built and
## deployed independently.

FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the Maven descriptor and source code into the build stage
COPY pom.xml .
COPY src ./src

# Package the application. We skip tests for faster container builds since the
# integration tests will be run separately during CI/CD. The resulting jar is
# placed under target/ by the Spring Boot Maven plugin.
RUN mvn -q -B package -DskipTests

# FROM eclipse-temurin:21-jre-slim
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from the previous stage. The wildcard picks up the only jar
# present in the target directory.
COPY --from=build /app/target/*.jar app.jar

# Expose the default port for the scheduling-service. In docker-compose we map
# this to the host port 8082.
EXPOSE 8080

# Start the Spring Boot application
ENTRYPOINT ["java","-jar","/app/app.jar"]