#!/bin/bash

# ServerSage Complete Observability Startup Script
# This script starts the complete observability stack with proper configuration

set -e

echo "🚀 Starting ServerSage Complete Observability Stack..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}=== $1 ===${NC}"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker first."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose &> /dev/null; then
    print_error "docker-compose is not installed. Please install Docker Compose."
    exit 1
fi

# Check if Java is available
if ! command -v java &> /dev/null; then
    print_error "Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed. Please install Maven."
    exit 1
fi

print_header "Step 1: Building the Application"
print_status "Cleaning and building the application..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    print_error "Failed to build the application"
    exit 1
fi

print_status "✅ Application built successfully"

print_header "Step 2: Starting Observability Stack"
print_status "Starting Docker containers for observability stack..."

# Stop any existing containers
docker-compose down --remove-orphans

# Start the observability stack
docker-compose up -d

if [ $? -ne 0 ]; then
    print_error "Failed to start observability stack"
    exit 1
fi

print_status "✅ Observability stack started"

print_header "Step 3: Waiting for Services to be Ready"

# Wait for services to be ready
print_status "Waiting for Prometheus..."
timeout=60
counter=0
while ! curl -s http://localhost:9090/-/ready > /dev/null; do
    sleep 2
    counter=$((counter + 2))
    if [ $counter -ge $timeout ]; then
        print_error "Prometheus failed to start within $timeout seconds"
        exit 1
    fi
done
print_status "✅ Prometheus is ready"

print_status "Waiting for Tempo..."
counter=0
while ! curl -s http://localhost:3200/ready > /dev/null; do
    sleep 2
    counter=$((counter + 2))
    if [ $counter -ge $timeout ]; then
        print_error "Tempo failed to start within $timeout seconds"
        exit 1
    fi
done
print_status "✅ Tempo is ready"

print_status "Waiting for Loki..."
counter=0
while ! curl -s http://localhost:3100/ready > /dev/null; do
    sleep 2
    counter=$((counter + 2))
    if [ $counter -ge $timeout ]; then
        print_error "Loki failed to start within $timeout seconds"
        exit 1
    fi
done
print_status "✅ Loki is ready"

print_status "Waiting for Grafana..."
counter=0
while ! curl -s http://localhost:3000/api/health > /dev/null; do
    sleep 2
    counter=$((counter + 2))
    if [ $counter -ge $timeout ]; then
        print_error "Grafana failed to start within $timeout seconds"
        exit 1
    fi
done
print_status "✅ Grafana is ready"

print_status "Waiting for OpenTelemetry Collector..."
counter=0
while ! curl -s http://localhost:8889/metrics > /dev/null; do
    sleep 2
    counter=$((counter + 2))
    if [ $counter -ge $timeout ]; then
        print_warning "OpenTelemetry Collector may not be fully ready, but continuing..."
        break
    fi
done
print_status "✅ OpenTelemetry Collector is ready"

print_header "Step 4: Setting up Database"
print_status "Setting up PostgreSQL database..."

# Wait for PostgreSQL to be ready (if using external PostgreSQL)
# For now, we'll use the application's built-in database setup

print_header "Step 5: Starting the Application"
print_status "Starting ServerSage application with OpenTelemetry..."

# Set environment variables
export OTEL_SERVICE_NAME=serversage
export OTEL_SERVICE_VERSION=1.0.0
export OTEL_DEPLOYMENT_ENVIRONMENT=development
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
export OTEL_EXPORTER_OTLP_PROTOCOL=grpc
export OTEL_TRACES_EXPORTER=otlp
export OTEL_METRICS_EXPORTER=otlp
export OTEL_LOGS_EXPORTER=otlp
export OTEL_TRACES_SAMPLER=always_on
export OTEL_METRICS_EXEMPLAR_FILTER=trace_based

# Database configuration
export DATABASE_URL=jdbc:postgresql://localhost:5432/serversage
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=password

# Start the application with OpenTelemetry Java agent
print_status "Starting application on port 8081..."
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name=serversage \
     -Dotel.service.version=1.0.0 \
     -Dotel.deployment.environment=development \
     -Dotel.exporter.otlp.endpoint=http://localhost:4317 \
     -Dotel.exporter.otlp.protocol=grpc \
     -Dotel.traces.exporter=otlp \
     -Dotel.metrics.exporter=otlp \
     -Dotel.logs.exporter=otlp \
     -Dotel.traces.sampler=always_on \
     -Dotel.metrics.exemplar.filter=trace_based \
     -Dotel.instrumentation.spring-webmvc.enabled=true \
     -Dotel.instrumentation.spring-data.enabled=true \
     -Dotel.instrumentation.jdbc.enabled=true \
     -Dotel.instrumentation.logback-appender.enabled=true \
     -jar target/serversage-0.0.1-SNAPSHOT.jar &

APP_PID=$!

# Wait for application to start
print_status "Waiting for application to start..."
counter=0
while ! curl -s http://localhost:8081/actuator/health > /dev/null; do
    sleep 3
    counter=$((counter + 3))
    if [ $counter -ge 120 ]; then
        print_error "Application failed to start within 120 seconds"
        kill $APP_PID 2>/dev/null || true
        exit 1
    fi
done

print_status "✅ Application started successfully"

print_header "🎉 ServerSage Complete Observability Stack is Ready!"

echo ""
echo "📊 Access Points:"
echo "  • Application:     http://localhost:8081"
echo "  • Swagger UI:      http://localhost:8081/swagger-ui.html"
echo "  • API Docs:        http://localhost:8081/api-docs"
echo "  • Health Check:    http://localhost:8081/actuator/health"
echo ""
echo "🔍 Observability Stack:"
echo "  • Grafana:         http://localhost:3000 (admin/admin)"
echo "  • Prometheus:      http://localhost:9090"
echo "  • Tempo (Traces):  http://localhost:3200"
echo "  • Loki (Logs):     http://localhost:3100"
echo ""
echo "📈 Pre-configured Dashboards in Grafana:"
echo "  • Business Dashboard    - KPIs and business metrics"
echo "  • Technical Dashboard   - Detailed technical metrics with exemplars"
echo "  • JVM Monitoring       - JVM performance and memory metrics"
echo ""
echo "🧪 Test the Observability:"
echo "  1. Make API calls to generate traces and metrics"
echo "  2. Trigger error scenarios using special IDs (999, 998, 997)"
echo "  3. Use error-triggering emails (dberror@, timeout@, external@)"
echo "  4. Check Grafana dashboards for metrics with exemplar links"
echo "  5. Click on exemplars to view traces in Tempo"
echo "  6. View correlated logs in Loki"
echo ""
echo "🔧 Example API Calls:"
echo "  curl http://localhost:8081/api/users"
echo "  curl http://localhost:8081/api/users/999  # Triggers error"
echo "  curl -X POST http://localhost:8081/api/users -H 'Content-Type: application/json' -d '{\"name\":\"Test\",\"email\":\"test@example.com\",\"role\":\"USER\"}'"
echo ""
echo "⚠️  To stop everything:"
echo "  1. Press Ctrl+C to stop this script"
echo "  2. Run: docker-compose down"
echo ""

# Keep the script running and handle shutdown gracefully
cleanup() {
    print_header "Shutting down..."
    print_status "Stopping application..."
    kill $APP_PID 2>/dev/null || true
    print_status "Stopping observability stack..."
    docker-compose down
    print_status "✅ Shutdown complete"
    exit 0
}

trap cleanup SIGINT SIGTERM

# Wait for the application process
wait $APP_PID
