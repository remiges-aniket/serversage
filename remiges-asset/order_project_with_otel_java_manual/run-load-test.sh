#!/bin/bash

# ServerSage Optimized Demo Load Test Runner
# This script runs realistic K6 load tests optimized for client demonstrations

set -e

echo "🚀 ServerSage Demo Load Test Runner"
echo "===================================="

# Check if application is running
echo "📡 Checking if ServerSage application is running..."
if ! curl -s http://localhost:8081/actuator/health > /dev/null; then
    echo "❌ ServerSage application is not running on port 8081"
    echo "   Please start the application first:"
    echo "   ./mvnw spring-boot:run"
    exit 1
fi

echo "✅ Application is running"

# Check if K6 is installed
if ! command -v k6 &> /dev/null; then
    echo "❌ K6 is not installed"
    echo "   Install K6 from: https://k6.io/docs/get-started/installation/"
    echo "   Or run with Docker: docker run --rm -i grafana/k6:latest run - <k6-tests/optimized-demo-test.js"
    exit 1
fi

echo "✅ K6 is available"

# Run the optimized demo test
echo ""
echo "🎯 Starting optimized demo load test..."
echo "   ⚡ Optimized for client demonstrations"
echo "   ⏱️  Duration: ~9 minutes"
echo "   👥 Max concurrent users: 5 (gentle load)"
echo "   🎲 Randomized realistic user behavior"
echo "   📊 Enhanced observability data generation"
echo ""

# Run K6 test with the optimized script
k6 run k6-tests/optimized-demo-test.js

echo ""
echo "🎉 Demo load test completed successfully!"
echo ""
echo "📊 View detailed metrics and traces in:"
echo "   • 📈 Grafana Dashboards: http://localhost:3000 (admin/admin)"
echo "     - Business Dashboard: Real-time KPIs and order metrics"
echo "     - Technical Dashboard: Response times, error rates with exemplars"
echo "     - JVM Dashboard: Memory, threads, garbage collection"
echo "   • 🔍 Prometheus Metrics: http://localhost:9090"
echo "   • 📋 Application Logs: tail -f app.log"
echo ""
echo "🔥 Key Demo Features:"
echo "   ✅ Order creation with real-time metrics updates"
echo "   ✅ Distributed tracing with correlation IDs"
echo "   ✅ Error scenarios with proper trace correlation"
echo "   ✅ Business metrics (revenue, user growth, order volume)"
echo "   ✅ Exemplar support - click from metrics to traces"
echo "   ✅ Alert-ready error logs with trace context"
echo ""
echo "💡 Pro Tip: Refresh Grafana dashboards to see the latest data!"
