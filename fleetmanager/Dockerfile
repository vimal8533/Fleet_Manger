# Stage 1: Build the Maven application with Java 17
FROM maven:3.9.6-eclipse-temurin-17-jammy AS builder

WORKDIR /app

# Copy pom.xml first to take advantage of Docker layer caching
COPY pom.xml .

# Pre-fetch project dependencies
RUN mvn dependency:go-offline -B || true

# Copy source code and compile production jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Java 17 JRE runtime container
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Create a dedicated non-root application user for security
RUN groupadd -r fleetgroup && useradd -r -g fleetgroup fleetuser

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set container ownership
RUN chown -R fleetuser:fleetgroup /app

USER fleetuser

# Render standard default port
ENV PORT=10000
EXPOSE 10000

# Launch Spring Boot with optimized random number generation
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
