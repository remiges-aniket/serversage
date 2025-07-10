#!/bin/bash

echo "🚀 Starting ServerSage Comprehensive Observability Stack"
echo "========================================================"

# Check if Docker and Docker Compose are installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Build the application
echo "🔨 Building the application..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please fix the build errors."
    exit 1
fi

# Start the observability stack
echo "🐳 Starting Docker Compose stack..."
docker-compose up -d

# Wait for services to be ready
echo "⏳ Waiting for services to start..."
sleep 30

# Check service health
echo "🏥 Checking service health..."

services=(
    "http://localhost:5432 PostgreSQL"
    "http://localhost:8081/actuator/health Application"
    "http://localhost:4317 OpenTelemetry-Collector"
    "http://localhost:16686 Jaeger"
    "http://localhost:9090 Prometheus"
    "http://localhost:3000 Grafana"
    "http://localhost:3100/ready Loki"
)

for service in "${services[@]}"; do
    url=$(echo $service | cut -d' ' -f1)
    name=$(echo $service | cut -d' ' -f2-)
    
    echo "Checking $name..."
    if curl -s -f "$url" > /dev/null 2>&1; then
        echo "✅ $name is healthy"
    else
        echo "⚠️  $name might not be ready yet"
    fi
done

echo ""
echo "🎉 Observability stack is starting up!"
echo ""
echo "📊 Access Points:"
echo "=================="
echo "🌐 Application:           http://localhost:8081"
echo "📋 Swagger UI:            http://localhost:8081/swagger-ui.html"
echo "🏥 Health Check:          http://localhost:8081/actuator/health"
echo "📈 Prometheus Metrics:    http://localhost:8081/actuator/prometheus"
echo ""
echo "🔍 Observability Tools:"
echo "======================="
echo "📊 Grafana Dashboards:    http://localhost:3000 (admin/admin)"
echo "🔍 Jaeger Traces:         http://localhost:16686"
echo "📈 Prometheus:            http://localhost:9090"
echo "📝 Loki Logs:             http://localhost:3100"
echo "🗄️  PgAdmin:               http://localhost:5050 (admin@admin.com/admin)"
echo ""
echo "🧪 Load Testing:"
echo "================"
echo "To run K6 load tests and generate exemplars:"
echo "docker-compose exec k6 /scripts/run-all-tests.sh"
echo ""
echo "Or run individual tests:"
echo "docker-compose exec k6 k6 run /scripts/smoke-test.js"
echo "docker-compose exec k6 k6 run /scripts/load-test-all-endpoints.js"
echo "docker-compose exec k6 k6 run /scripts/exemplar-generation-test.js"
echo "docker-compose exec k6 k6 run /scripts/stress-test.js"
echo ""
echo "📚 Documentation:"
echo "=================="
echo "• README.md - Complete setup and usage guide"
echo "• IMPLEMENTATION_SUMMARY.md - What was implemented"
echo "• grafana-dashboards/ - Ready-to-import Grafana dashboards"
echo ""
echo "🎯 Demo Scenarios:"
echo "=================="
echo "1. Visit Grafana dashboards to see business and technical metrics"
echo "2. Run load tests to generate traffic and exemplars"
echo "3. Click on error rates in Grafana to drill down to traces"
echo "4. View correlated logs in Loki with trace IDs"
echo "5. Test error scenarios:"
echo "   curl http://localhost:8081/api/products/999  # Array index error"
echo "   curl http://localhost:8081/api/products/998  # Null pointer error"
echo ""
echo "✨ Your comprehensive OpenTelemetry observability showcase is ready!"
