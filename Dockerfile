# ---- Build Stage ----
FROM gradle:8.10-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# ---- Run Stage ----
FROM openjdk:21-jdk-slim
WORKDIR /app

# Copy the generated JAR from build stage
COPY --from=build /app/build/libs/s3manager-1.0.0.jar app.jar

# Expose port 8080
EXPOSE 8080

# Start the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
