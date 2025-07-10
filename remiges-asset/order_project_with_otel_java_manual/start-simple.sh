#!/bin/bash

echo "🚀 Starting Simple Observability Stack"
echo "======================================"

# Kill any existing application
pkill -f "serversage" 2>/dev/null || true

# Build application
echo "🔨 Building application..."
mvn clean package -DskipTests -q

# Start observability stack (no postgres, no jaeger)
echo "🐳 Starting observability services..."
docker-compose up -d

# Wait for services
echo "⏳ Waiting for services to start..."
sleep 20

# Start application locally
echo "🚀 Starting application..."
DATABASE_URL=jdbc:postgresql://localhost:5432/serversage \
DATABASE_USERNAME=postgres \
DATABASE_PASSWORD=password \
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317 \
java -jar target/serversage-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# Wait for app to start
echo "⏳ Waiting for application to start..."
sleep 15

# Test application
echo "🧪 Testing application..."
if curl -s http://localhost:8081/actuator/health | grep -q "UP"; then
    echo "✅ Application is running!"
    echo ""
    echo "📊 Access Points:"
    echo "Application: http://localhost:8081"
    echo "Grafana: http://localhost:3000 (admin/admin)"
    echo "Prometheus: http://localhost:9090"
    echo "Tempo: http://localhost:3200"
    echo ""
    echo "🧪 Run load test:"
    echo "docker-compose exec k6 k6 run /scripts/simple-load-test.js"
    echo ""
    echo "🎯 Test error scenarios:"
    echo "curl http://localhost:8081/api/products/999"
else
    echo "❌ Application failed to start. Check app.log"
    tail -20 app.log
fi
