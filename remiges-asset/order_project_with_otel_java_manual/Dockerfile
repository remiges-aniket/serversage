# Multi-stage build for ServerSage with OpenTelemetry
FROM maven:3.9.4-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (for better caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create app directory
WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Download OpenTelemetry Java Agent
ARG OTEL_VERSION=2.17.1
RUN curl -L -f --retry 3 --retry-delay 5 -o opentelemetry-javaagent.jar \
    "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_VERSION}/opentelemetry-javaagent.jar" && \
    ls -la opentelemetry-javaagent.jar && \
    [ -s opentelemetry-javaagent.jar ] && \
    chmod 644 opentelemetry-javaagent.jar

# Copy the built JAR from builder stage
COPY --from=builder /app/target/serversage-*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8081

# Environment variables for OpenTelemetry
ENV OTEL_SERVICE_NAME=serversage
ENV OTEL_SERVICE_VERSION=1.0.0
ENV OTEL_RESOURCE_ATTRIBUTES=service.name=serversage,service.version=1.0.0,deployment.environment=docker
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
ENV OTEL_EXPORTER_OTLP_PROTOCOL=grpc
ENV OTEL_METRICS_EXPORTER=otlp
ENV OTEL_LOGS_EXPORTER=otlp
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_INSTRUMENTATION_JDBC_ENABLED=true
ENV OTEL_INSTRUMENTATION_SPRING_WEB_ENABLED=true
ENV OTEL_INSTRUMENTATION_SPRING_WEBMVC_ENABLED=true
ENV OTEL_INSTRUMENTATION_HIBERNATE_ENABLED=true
ENV OTEL_INSTRUMENTATION_JPA_ENABLED=true
ENV OTEL_INSTRUMENTATION_HTTP_CLIENT_ENABLED=true
ENV OTEL_INSTRUMENTATION_LOGBACK_APPENDER_ENABLED=true

# JVM options
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

# Start the application with OpenTelemetry agent
CMD java $JAVA_OPTS \
    -javaagent:opentelemetry-javaagent.jar \
    -jar app.jar
