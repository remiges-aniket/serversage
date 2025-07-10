# ServerSage - Complete OpenTelemetry Observability Demo

A comprehensive Spring Boot application showcasing **enterprise-grade observability** with OpenTelemetry, featuring 20+ REST APIs, comprehensive error scenarios, and full trace correlation.

## 🚀 Quick Start

### Option 1: Docker (Recommended)
```bash
# Build and run with complete observability stack
./docker-build.sh
./docker-run.sh

# Or manually
docker-compose up -d
```

### Option 2: Local Development
```bash
# Build and run locally
mvn clean package
mvn spring-boot:run

# Start observability stack
docker-compose up -d
```

### 3. Run Load Tests
```bash
# Single command to test all endpoints
./run-load-test.sh
```

### 4. View Results
- **Grafana Dashboards**: http://localhost:3000 (admin/admin)
- **Prometheus Metrics**: http://localhost:9090
- **Application**: http://localhost:8081
- **API Docs**: http://localhost:8081/swagger-ui.html

## 🐳 Docker Deployment

### Building the Docker Image

The project includes a multi-stage Dockerfile that:
- ✅ **Builds the application** using Maven
- ✅ **Downloads OpenTelemetry Java Agent** (v1.32.0)
- ✅ **Creates optimized runtime image** with Alpine Linux
- ✅ **Includes health checks** and security best practices
- ✅ **Pre-configures OpenTelemetry** for container environments

```bash
# Build the Docker image
./docker-build.sh

# or manually
docker build -t otel-demo:1.0.0 .

docker login

# tag image to docker
docker tag otel-demo:1.0.0 aniketxshinde/otel-demo:1.0.0

# push image to docker hub
docker push aniketxshinde/otel-demo:1.0.0
```

### Docker Image Features

**🔧 OpenTelemetry Integration:**
- OpenTelemetry Java Agent v1.32.0 included
- Pre-configured for OTLP export to collector
- Automatic instrumentation for Spring Boot, JPA, JDBC
- Container-optimized resource attributes

**🛡️ Security & Performance:**
- Non-root user execution
- Minimal Alpine Linux base image
- Optimized JVM settings for containers
- Health checks included

**📊 Observability Ready:**
- Traces exported to Tempo via OTLP
- Metrics exported to Prometheus
- Logs with trace correlation
- Complete service topology visibility

### Running with Docker Compose

```bash
# Start complete stack (recommended)
./docker-run.sh

# Or manually
docker-compose up -d

# View logs
docker-compose logs -f serversage-app

# Stop services
docker-compose down

# Stop and remove data
docker-compose down -v
```

### Docker Services

| Service | Port | Description |
|---------|------|-------------|
| **serversage-app** | 8081 | Main application with OpenTelemetry |
| **postgres** | 5432 | PostgreSQL database |
| **otel-collector** | 4317/4318 | OpenTelemetry Collector |
| **prometheus** | 9090 | Metrics storage |
| **tempo** | 3200 | Distributed tracing |
| **loki** | 3100 | Log aggregation |
| **grafana** | 3000 | Visualization dashboards |

### Environment Variables

The Docker image supports these OpenTelemetry environment variables:

```bash
# Service identification
OTEL_SERVICE_NAME=serversage
OTEL_SERVICE_VERSION=1.0.0
OTEL_RESOURCE_ATTRIBUTES=service.name=serversage,deployment.environment=docker

# Export configuration
OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
OTEL_EXPORTER_OTLP_PROTOCOL=grpc
OTEL_METRICS_EXPORTER=otlp
OTEL_LOGS_EXPORTER=otlp
OTEL_TRACES_EXPORTER=otlp

# Instrumentation settings
OTEL_INSTRUMENTATION_JDBC_ENABLED=true
OTEL_INSTRUMENTATION_SPRING_WEB_ENABLED=true
OTEL_INSTRUMENTATION_HIBERNATE_ENABLED=true
```

### Custom Docker Run

```bash
# Run standalone with external database
docker run -d \
  --name serversage \
  -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/serversage \
  -e OTEL_EXPORTER_OTLP_ENDPOINT=http://host.docker.internal:4317 \
  serversage:latest

# Run with custom OpenTelemetry endpoint
docker run -d \
  --name serversage \
  -p 8081:8081 \
  -e OTEL_EXPORTER_OTLP_ENDPOINT=https://your-otel-collector:4317 \
  -e OTEL_EXPORTER_OTLP_HEADERS="api-key=your-api-key" \
  serversage:latest
```

## 🎯 What This Demonstrates

### Complete Observability Stack
- **✅ Distributed Tracing** - Full request correlation with Tempo
- **✅ Custom Metrics** - Business and technical metrics with exemplars
- **✅ Structured Logging** - Correlated logs with trace context
- **✅ Error Tracking** - Comprehensive error scenarios with correlation
- **✅ Performance Monitoring** - JVM, database, and HTTP metrics
- **✅ Business Dashboards** - KPIs for different audiences

### Enhanced Tracing Features
- **✅ SQL Query Visibility** - See actual SQL statements in traces
- **✅ Request Payload Capture** - POST/PUT request bodies in traces
- **✅ Database Operation Details** - Query duration and success status
- **✅ Error Correlation** - Trace IDs in error responses for immediate troubleshooting
- **✅ Service Boundary Isolation** - Clean trace separation per HTTP request

### Key Features
- **20+ REST APIs** with realistic business logic
- **Intentional Error Scenarios** for testing observability
- **Full Trace Correlation** - errors include traceId/spanId
- **Exemplar Support** - click from metrics to traces
- **3 Grafana Dashboards** - Business, Technical, JVM
- **Load Testing** - K6 scripts for all endpoints

## 🔧 Architecture

```
Application (Spring Boot + OpenTelemetry Agent)
    ↓ OTLP
OpenTelemetry Collector
    ↓ ↓ ↓
Prometheus  Tempo  Loki
    ↓ ↓ ↓
    Grafana
```

## 📊 Available APIs

### User Management
- `GET /api/users` - List all users
- `POST /api/users` - Create user
- `GET /api/users/{id}` - Get user (999=Error, 998=NullPointer)
- `PUT /api/users/{id}` - Update user
- `GET /api/users/search?keyword=` - Search users
- `GET /api/users/stats` - User statistics

### Product Management  
- `GET /api/products` - List products
- `POST /api/products` - Create product
- `GET /api/products/category/{category}` - Filter by category
- `GET /api/products/low-stock` - Low stock alerts

### Order Management
- `GET /api/orders` - List orders
- `POST /api/orders` - Create order
- `GET /api/orders/user/{userId}` - User orders
- `PATCH /api/orders/{id}/status` - Update status

### Analytics
- `GET /api/analytics/dashboard` - Dashboard metrics
- `GET /api/analytics/users/statistics` - User analytics
- `GET /api/analytics/performance/metrics` - Performance data

## ⚠️ Error Testing

Trigger specific errors for observability testing:

```bash
# Array Index Error
curl http://localhost:8081/api/users/999

# Null Pointer Error  
curl http://localhost:8081/api/users/998

# Database Error
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"dberror@test.com","role":"USER"}'

# Validation Error
curl http://localhost:8081/api/users/search?keyword=
```

## 🧪 Load Testing

### Simple One-Command Testing
```bash
# Test all endpoints with realistic load
./run-load-test.sh
```

This script:
- ✅ Checks if application is running
- ✅ Runs comprehensive K6 tests
- ✅ Tests all API endpoints
- ✅ Generates observability data
- ✅ Provides summary results

### Test Scenarios
- **User Management** - CRUD operations
- **Product Management** - Inventory operations  
- **Order Processing** - Order lifecycle
- **Error Scenarios** - Intentional failures
- **Mixed Workload** - Realistic usage patterns

## 📈 Observability Features

### Metrics (Prometheus)
- HTTP request rates and durations
- Error rates by endpoint
- Database operation metrics
- JVM memory, threads, GC
- Business KPIs (users, orders, revenue)

### Traces (Tempo)
- Full request tracing with SQL queries
- Span correlation across services
- Error trace correlation
- Performance bottleneck identification
- Request payload visibility

### Logs (Loki)
- Structured JSON logging
- Automatic trace correlation
- Error context preservation
- Performance logging

### Dashboards (Grafana)
1. **Business Dashboard** - KPIs for stakeholders
2. **Technical Dashboard** - Developer metrics with exemplars
3. **JVM Dashboard** - Performance monitoring

## 🔍 Key Observability Patterns

### Enhanced SQL Query Tracing
```bash
# Example trace shows actual SQL
GET /api/users/stats
# Trace contains: "🔍 Executing SQL: SELECT u.id, u.name, u.email, u.role FROM users u"
```

### Error Correlation
```json
{
  "status": 500,
  "error": "ARRAY_INDEX_OUT_OF_BOUNDS", 
  "traceId": "1a833753d4b5fb2cd8bd5d6f831298fc",
  "spanId": "e1fd50beb4bb337e"
}
```

### Request Payload Tracing
```bash
# POST requests show payload in traces
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Trace Product","price":99.99}'
# Trace shows: "📝 Request Body: {\"name\":\"Trace Product\",\"price\":99.99}"
```

### Exemplar Support
- Metrics automatically link to traces
- Click from dashboard to trace details
- Root cause analysis workflow

### Comprehensive Monitoring
- Business metrics (revenue, user growth)
- Technical metrics (response times, errors)
- Infrastructure metrics (JVM, database)

## 🛠️ Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **Docker & Docker Compose**
- **K6** (for load testing)

## 📝 Quick Commands

```bash
# Docker deployment (recommended)
./docker-build.sh && ./docker-run.sh

# Local development
mvn spring-boot:run &
docker-compose up -d
./run-load-test.sh

# View dashboards
open http://localhost:3000

# Check metrics
curl http://localhost:9090/api/v1/query?query=serversage_http_requests_total

# Test API with tracing
curl http://localhost:8081/api/users/stats
```

## 🔧 Troubleshooting

### Docker Issues

```bash
# Check container status
docker-compose ps

# View application logs
docker-compose logs -f serversage-app

# Restart specific service
docker-compose restart serversage-app

# Clean rebuild
docker-compose down -v
docker system prune -f
./docker-build.sh
./docker-run.sh
```

### TraceQL Metrics Query Error in Grafana

If you see the error: `"localblocks processor not found"` in Grafana when trying to use TraceQL metrics queries, this is a known limitation with the current Tempo version. 

**Workaround:**
1. **Use regular trace search** instead of TraceQL metrics queries
2. **Access traces directly** via Tempo UI at http://localhost:3200
3. **Use exemplars** in Grafana dashboards to jump from metrics to traces
4. **Search by trace ID** using the traceId from error responses

### Common Issues

1. **Services not starting**: Check `docker-compose ps` and logs
2. **No traces visible**: Wait 30-60 seconds for trace ingestion
3. **Grafana dashboards empty**: Run `./run-load-test.sh` to generate data
4. **Port conflicts**: Ensure ports 3000, 3200, 9090, 4317, 8081 are available
5. **Database connection**: Ensure PostgreSQL is healthy before app starts

## 🎉 What You'll See

After running the Docker stack:
- **Real-time metrics** in Prometheus
- **Distributed traces with SQL queries** in Tempo  
- **Correlated logs** in Loki
- **Business dashboards** in Grafana
- **Error correlation** across all systems
- **Performance insights** from JVM monitoring
- **Request payload visibility** in traces
- **Complete observability** with trace isolation

This demonstrates **production-ready observability** with complete correlation between metrics, traces, and logs - essential for modern microservices monitoring.

---

**🚀 Ready to explore enterprise observability with Docker!**
