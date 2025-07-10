#!/bin/bash

echo "🚀 Running ServerSage with Complete Observability Stack"
echo "======================================================"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

echo ""
echo "🔧 Starting services in order:"
echo "  1. PostgreSQL Database"
echo "  2. OpenTelemetry Collector"
echo "  3. Prometheus, Tempo, Loki"
echo "  4. Grafana"
echo "  5. ServerSage Application"
echo ""

# Start the complete stack
echo "🐳 Starting Docker Compose stack..."
docker-compose up -d

# Wait for services to be ready
echo ""
echo "⏳ Waiting for services to start..."
sleep 30

# Check service health
echo ""
echo "🔍 Checking service health:"

# Check PostgreSQL
if curl -s http://localhost:5432 > /dev/null 2>&1; then
    echo "✅ PostgreSQL: Ready"
else
    echo "⏳ PostgreSQL: Starting..."
fi

# Check Application
if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo "✅ ServerSage App: Ready"
else
    echo "⏳ ServerSage App: Starting..."
fi

# Check Prometheus
if curl -s http://localhost:9090/-/ready > /dev/null 2>&1; then
    echo "✅ Prometheus: Ready"
else
    echo "⏳ Prometheus: Starting..."
fi

# Check Grafana
if curl -s http://localhost:3000/api/health > /dev/null 2>&1; then
    echo "✅ Grafana: Ready"
else
    echo "⏳ Grafana: Starting..."
fi

# Check Tempo
if curl -s http://localhost:3200/ready > /dev/null 2>&1; then
    echo "✅ Tempo: Ready"
else
    echo "⏳ Tempo: Starting..."
fi

echo ""
echo "🎯 Access URLs:"
echo "================================"
echo "🌐 ServerSage Application: http://localhost:8081"
echo "📊 Swagger API Docs:       http://localhost:8081/swagger-ui.html"
echo "📈 Grafana Dashboards:     http://localhost:3000 (admin/admin)"
echo "🔍 Prometheus Metrics:     http://localhost:9090"
echo "🔗 Tempo Traces:           http://localhost:3200"
echo "📝 Loki Logs:              http://localhost:3100"
echo ""
echo "🧪 Test Commands:"
echo "================================"
echo "# Health Check"
echo "curl http://localhost:8081/actuator/health"
echo ""
echo "# Get User Stats"
echo "curl http://localhost:8081/api/users/stats"
echo ""
echo "# Create Product (with tracing)"
echo "curl -X POST http://localhost:8081/api/products \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"name\":\"Docker Product\",\"price\":99.99,\"stockQuantity\":10,\"category\":\"docker\"}'"
echo ""
echo "# Trigger Error (for trace correlation)"
echo "curl http://localhost:8081/api/users/999"
echo ""
echo "🔧 Management Commands:"
echo "================================"
echo "# View logs"
echo "docker-compose logs -f serversage-app"
echo ""
echo "# Stop all services"
echo "docker-compose down"
echo ""
echo "# Stop and remove volumes"
echo "docker-compose down -v"
echo ""
echo "✅ ServerSage is running with complete observability!"
