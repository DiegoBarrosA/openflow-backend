# Multi-stage build for Spring Boot application
FROM docker.io/library/maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Run tests before building - tests must pass for image to be created
RUN mvn clean package

FROM docker.io/library/eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create wallet directory
RUN mkdir -p /app/wallet

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

# Copy wallet files (if building wallet into image - NOT recommended for production)
# COPY wallet/* /app/wallet/

EXPOSE 8080
# Use JAVA_OPTS if provided, otherwise use default
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]

