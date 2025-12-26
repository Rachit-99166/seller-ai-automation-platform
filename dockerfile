# Use JDK 21 (Alpine Linux for small size)
FROM eclipse-temurin:21-jdk-alpine

# Copy the built jar file
COPY target/*.jar app.jar

# Run the jar file
ENTRYPOINT ["java", "-jar", "/app.jar"]