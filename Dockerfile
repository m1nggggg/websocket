# --- Stage 1: Build the application ---
# Use an official Maven image with a specific Java version to build the project.
# Using a specific version (e.g., 21) ensures consistent builds.
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml first to leverage Docker's layer caching.
# This step will only be re-run if the pom.xml changes.
COPY pom.xml .

# Download all dependencies
RUN mvn dependency:go-offline

# Copy the rest of your source code
COPY src ./src

# Package the application into a JAR file, skipping the tests for faster builds.
# The final JAR will be in the /app/target/ directory.
RUN mvn package -DskipTests

# --- Stage 2: Create the final, lightweight runtime image ---
# Use a JRE (Java Runtime Environment) image, which is smaller than a full JDK.
FROM eclipse-temurin:21-jre-jammy

# Set the working directory
WORKDIR /app

# Create a non-root user and group for better security
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser
USER appuser

# Copy only the built JAR file from the 'builder' stage into the final image.
# This keeps the final image small and free of source code or build tools.
COPY --from=builder /app/target/*.jar app.jar

# Expose the port that the Spring Boot application listens on
EXPOSE 10000

# The command to run when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]