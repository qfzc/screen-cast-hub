# syntax=docker/dockerfile:1.4
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies with cache mount (persisted across builds)
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B -Djavacpp.platform=linux-x86_64

# Copy source and build with cache mount
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B -Djavacpp.platform=linux-x86_64

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Create non-root user for security
RUN groupadd -r screencast && useradd -r -g screencast screencast

# Copy jar from builder
COPY --from=builder /build/target/*.jar app.jar

# Create directories for logs and data
RUN mkdir -p /app/logs /app/data && chown -R screencast:screencast /app

USER screencast

# Expose port
EXPOSE 8080

# JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
