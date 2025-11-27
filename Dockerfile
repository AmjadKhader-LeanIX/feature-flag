FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Copy source code
COPY src src

# Make gradlew executable
RUN chmod +x ./gradlew

# Build the application
RUN ./gradlew build -x test

# Expose the application port
EXPOSE 8080

# Set environment variables for database connection
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/feature-flag?currentSchema=public
ENV SPRING_DATASOURCE_USERNAME=feature-flag
ENV SPRING_DATASOURCE_PASSWORD=feature-flag123

# Run the application

CMD ["java", "-jar", "build/libs/feature-flag-1.0.0.jar"]
