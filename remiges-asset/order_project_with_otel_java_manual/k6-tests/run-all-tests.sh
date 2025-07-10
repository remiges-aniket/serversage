#!/bin/bash

echo "🚀 Starting comprehensive K6 load testing for ServerSage observability showcase"
echo "=============================================================================="

# Wait for application to be ready
echo "⏳ Waiting for application to be ready..."
sleep 30

# Run smoke test first
echo "🔍 Running smoke test..."
k6 run --out prometheus=http://prometheus:9090/api/v1/write /scripts/smoke-test.js

sleep 10

# Run exemplar generation test
echo "📊 Running exemplar generation test..."
k6 run --out prometheus=http://prometheus:9090/api/v1/write /scripts/exemplar-generation-test.js

sleep 10

# Run comprehensive load test
echo "🏋️ Running comprehensive load test on all endpoints..."
k6 run --out prometheus=http://prometheus:9090/api/v1/write /scripts/load-test-all-endpoints.js

sleep 10

# Run stress test
echo "💪 Running stress test..."
k6 run --out prometheus=http://prometheus:9090/api/v1/write /scripts/stress-test.js

echo "✅ All K6 tests completed!"
echo "📈 Check Grafana dashboards at http://localhost:3000"
echo "🔍 Check Jaeger traces at http://localhost:16686"
echo "📊 Check Prometheus metrics at http://localhost:9090"
