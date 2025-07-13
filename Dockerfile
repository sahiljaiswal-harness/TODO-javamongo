# Use Maven for build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy source code and pom.xml into container
COPY . .

# Build the project and resolve dependencies
RUN mvn clean package -DskipTests

# -----------------------------
# Use a lightweight Java runtime for running the app
FROM eclipse-temurin:17-jre

# Set working directory for runtime container
WORKDIR /app

# Copy built jar from build container
COPY --from=build /app/target/todo-javamongo-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar

# Expose port your app runs on
EXPOSE 8000

# Set environment variable for MongoDB URI (to be overridden at runtime)
ENV MONGO_URI=""

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
