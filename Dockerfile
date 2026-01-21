# Stage 1: Build frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build backend
FROM eclipse-temurin:21-jdk-alpine AS backend-builder
WORKDIR /app
COPY backend/gradlew .
COPY backend/gradle gradle
COPY backend/build.gradle.kts .
COPY backend/settings.gradle.kts .
COPY backend/src src
# Copy built frontend from stage 1
COPY --from=frontend-builder /frontend/dist src/main/resources/static
RUN chmod +x ./gradlew && ./gradlew build -x test

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/build/libs/*.jar app.jar

# Set environment variables for database connection
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/feature-flag?currentSchema=public
ENV SPRING_DATASOURCE_USERNAME=feature-flag
ENV SPRING_DATASOURCE_PASSWORD=feature-flag123

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
