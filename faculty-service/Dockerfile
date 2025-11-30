## Build the faculty-service JAR using a multi-stage build.  The first stage
## compiles the Maven project and produces a runnable Spring Boot jar.  The
## second stage runs that jar on a slim JRE.  Using multi-stage builds keeps
## the final image small and consistent with the other services.

FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app

# Copy Maven metadata and source into the build context
COPY pom.xml .
COPY src ./src

# Build the project, skipping tests to reduce build time.  The Spring Boot
# Maven plugin will produce a jar under target/.
RUN mvn -q -B package -DskipTests

# FROM eclipse-temurin:21-jre-slim
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the packaged application from the build stage.  The wildcard will match
# the single jar produced by the previous Maven build.
COPY --from=build /app/target/*.jar app.jar

# Expose the port on which the faculty-service listens.  docker-compose maps
# this to 8083 on the host.
EXPOSE 8080

# Define the default command to run the Spring Boot application.
ENTRYPOINT ["java","-jar","/app/app.jar"]