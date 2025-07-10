# 🐳 ServerSage Docker Deployment Guide

Complete guide for building and deploying ServerSage with OpenTelemetry using Docker.

## 🚀 Quick Start

### 1. Build Docker Image
```bash
# Build with OpenTelemetry Java Agent included
./docker-build.sh

# Or manually
docker build -t serversage:latest .
```

### 2. Run Complete Stack
```bash
# Start all services (recommended)
./docker-run.sh

# Or manually
docker-compose up -d
```

### 3. Test the Deployment
```bash
# Test Docker image
./test-docker-image.sh

# Test enhanced tracing
./test-enhanced-tracing.sh
```

## 🏗️ Docker Image Details

### Multi-Stage Build Process
```dockerfile
# Stage 1: Build application with Maven
FROM maven:3.9.4-eclipse-temurin-17 AS builder
# Downloads dependencies and builds JAR

# Stage 2: Runtime with OpenTelemetry
FROM eclipse-temurin:17-jre-alpine
# Downloads OpenTelemetry Java Agent v1.32.0
# Creates optimized runtime environment
```

### Image Features
- **Size**: ~303MB (optimized Alpine Linux)
- **Security**: Non-root user execution
- **Observability**: Pre-configured OpenTelemetry
- **Health Checks**: Built-in health monitoring
- **Performance**: Optimized JVM settings

### OpenTelemetry Configuration
```bash
# Pre-configured environment variables
OTEL_SERVICE_NAME=serversage
OTEL_SERVICE_VERSION=1.0.0
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_EXPORTER_OTLP_PROTOCOL=grpc
OTEL_METRICS_EXPORTER=otlp
OTEL_LOGS_EXPORTER=otlp
OTEL_TRACES_EXPORTER=otlp

# Instrumentation enabled
OTEL_INSTRUMENTATION_JDBC_ENABLED=true
OTEL_INSTRUMENTATION_SPRING_WEB_ENABLED=true
OTEL_INSTRUMENTATION_HIBERNATE_ENABLED=true
```

## 🔧 Deployment Options

### Option 1: Complete Stack (Recommended)
```bash
# Includes: App + Database + Observability Stack
docker-compose up -d

# Services started:
# - serversage-app (port 8081)
# - postgres (port 5432)
# - otel-collector (ports 4317/4318)
# - prometheus (port 9090)
# - tempo (port 3200)
# - loki (port 3100)
# - grafana (port 3000)
```

### Option 2: Standalone Application
```bash
# Run with external database
docker run -d \
  --name serversage \
  -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/serversage \
  -e SPRING_DATASOURCE_USERNAME=serversage \
  -e SPRING_DATASOURCE_PASSWORD=serversage123 \
  -e OTEL_EXPORTER_OTLP_ENDPOINT=http://host.docker.internal:4317 \
  serversage:latest
```

### Option 3: Development Mode
```bash
# Run with H2 in-memory database
docker run -d \
  --name serversage-dev \
  -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:h2:mem:devdb \
  -e SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver \
  -e SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect \
  serversage:latest
```

## 🌐 Service Access URLs

After running `docker-compose up -d`:

| Service | URL | Credentials |
|---------|-----|-------------|
| **ServerSage API** | http://localhost:8081 | - |
| **Swagger UI** | http://localhost:8081/swagger-ui.html | - |
| **Health Check** | http://localhost:8081/actuator/health | - |
| **Grafana Dashboards** | http://localhost:3000 | admin/admin |
| **Prometheus** | http://localhost:9090 | - |
| **Tempo Traces** | http://localhost:3200 | - |
| **Loki Logs** | http://localhost:3100 | - |

## 🧪 Testing & Verification

### 1. Health Check
```bash
curl http://localhost:8081/actuator/health
```

### 2. API Testing
```bash
# Get user statistics (with SQL tracing)
curl http://localhost:8081/api/users/stats

# Create product (with payload tracing)
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Docker Product","price":99.99,"stockQuantity":10,"category":"docker"}'

# Trigger error (with trace correlation)
curl http://localhost:8081/api/users/999
```

### 3. Observability Verification
```bash
# Check metrics
curl http://localhost:9090/api/v1/query?query=serversage_http_requests_total

# View traces in Grafana
open http://localhost:3000

# Check application logs
docker-compose logs -f serversage-app
```

## 🔧 Configuration

### Environment Variables
```bash
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/serversage
SPRING_DATASOURCE_USERNAME=serversage
SPRING_DATASOURCE_PASSWORD=serversage123

# OpenTelemetry Configuration
OTEL_SERVICE_NAME=serversage
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_RESOURCE_ATTRIBUTES=service.name=serversage,deployment.environment=docker

# Application Configuration
SERVER_PORT=8081
LOGGING_LEVEL_TECH_REMIGES_SERVERSAGE=INFO
```

### Custom Configuration
```bash
# Override default settings
docker run -d \
  --name serversage-custom \
  -p 8081:8081 \
  -e OTEL_SERVICE_NAME=my-serversage \
  -e OTEL_EXPORTER_OTLP_ENDPOINT=https://my-collector:4317 \
  -e OTEL_EXPORTER_OTLP_HEADERS="api-key=my-secret-key" \
  -e JAVA_OPTS="-Xms512m -Xmx1024m" \
  serversage:latest
```

## 🛠️ Management Commands

### Container Management
```bash
# View running containers
docker-compose ps

# View logs
docker-compose logs -f serversage-app

# Restart specific service
docker-compose restart serversage-app

# Scale application (if needed)
docker-compose up -d --scale serversage-app=2

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Image Management
```bash
# List images
docker images serversage

# Remove old images
docker image prune -f

# Rebuild image
./docker-build.sh

# Push to registry (if configured)
docker tag serversage:latest your-registry/serversage:latest
docker push your-registry/serversage:latest
```

## 🔍 Troubleshooting

### Common Issues

#### 1. Container Won't Start
```bash
# Check container logs
docker-compose logs serversage-app

# Check resource usage
docker stats

# Verify port availability
netstat -tulpn | grep :8081
```

#### 2. Database Connection Issues
```bash
# Check PostgreSQL status
docker-compose logs postgres

# Verify database connectivity
docker-compose exec postgres psql -U serversage -d serversage -c "SELECT 1;"

# Reset database
docker-compose down -v
docker-compose up -d postgres
```

#### 3. OpenTelemetry Issues
```bash
# Check collector status
docker-compose logs otel-collector

# Verify OTLP endpoint
curl -v http://localhost:4317

# Check trace export
docker-compose logs serversage-app | grep -i otel
```

#### 4. Performance Issues
```bash
# Increase memory limits
docker run -d \
  --name serversage \
  -p 8081:8081 \
  -e JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC" \
  serversage:latest

# Monitor resource usage
docker stats serversage-app
```

### Health Checks
```bash
# Application health
curl http://localhost:8081/actuator/health

# Database health
docker-compose exec postgres pg_isready -U serversage

# Observability stack health
curl http://localhost:9090/-/ready  # Prometheus
curl http://localhost:3200/ready    # Tempo
curl http://localhost:3000/api/health # Grafana
```

## 📊 Monitoring & Observability

### Key Metrics to Monitor
- **Application**: Response times, error rates, throughput
- **Database**: Connection pool, query performance
- **JVM**: Memory usage, garbage collection, threads
- **Container**: CPU, memory, disk usage

### Grafana Dashboards
1. **Business Dashboard**: KPIs and business metrics
2. **Technical Dashboard**: Application performance with exemplars
3. **JVM Dashboard**: Java Virtual Machine monitoring

### Alerting
- High error rate (>10% for 1 minute)
- Slow response times (>2000ms P95 for 2 minutes)
- Database connection failures
- Container resource exhaustion

## 🚀 Production Deployment

### Security Considerations
```bash
# Use secrets for sensitive data
docker run -d \
  --name serversage \
  -p 8081:8081 \
  --secret db_password \
  -e SPRING_DATASOURCE_PASSWORD_FILE=/run/secrets/db_password \
  serversage:latest

# Run with read-only filesystem
docker run -d \
  --name serversage \
  -p 8081:8081 \
  --read-only \
  --tmpfs /tmp \
  serversage:latest
```

### Performance Optimization
```bash
# Production JVM settings
-e JAVA_OPTS="-Xms1024m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport"

# Resource limits
docker run -d \
  --name serversage \
  --memory=2g \
  --cpus=2 \
  -p 8081:8081 \
  serversage:latest
```

### High Availability
```bash
# Use Docker Swarm or Kubernetes for HA
docker service create \
  --name serversage \
  --replicas 3 \
  --publish 8081:8081 \
  serversage:latest
```

## 📝 Summary

The ServerSage Docker deployment provides:

✅ **Complete Observability Stack** - Metrics, traces, logs, and dashboards
✅ **Production Ready** - Security, performance, and monitoring built-in
✅ **Easy Deployment** - One-command setup with docker-compose
✅ **Enhanced Tracing** - SQL queries, request payloads, and error correlation
✅ **Scalable Architecture** - Container-native design
✅ **Developer Friendly** - Local development and testing support

Ready to demonstrate enterprise-grade observability with Docker! 🎉
