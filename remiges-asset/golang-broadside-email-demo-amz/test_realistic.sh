#!/bin/bash

echo "🚀 Starting Realistic Email Service Test..."

# Kill any existing process on port 8088
echo "🔍 Checking for existing processes on port 8088..."
if lsof -ti:8088; then
    echo "⚠️  Killing existing process on port 8088..."
    kill -9 $(lsof -ti:8088) 2>/dev/null || true
    sleep 2
fi

# Start the realistic email service in background
echo "🎯 Starting realistic email service..."
./broadsid-email-realistic &
SERVICE_PID=$!

# Wait for service to start
echo "⏳ Waiting for service to start..."
sleep 3

# Check if service is running
if ! ps -p $SERVICE_PID > /dev/null; then
    echo "❌ Service failed to start!"
    exit 1
fi

echo "✅ Service started with PID: $SERVICE_PID"

# Test health endpoint
echo "🏥 Testing health endpoint..."
curl -s http://localhost:8088/health | jq '.' || echo "Health check failed"

# Wait a bit for metrics to be generated
echo "📊 Waiting for metrics generation (30 seconds)..."
sleep 30

# Test metrics endpoint
echo "📈 Testing metrics endpoint..."
echo "Sample metrics:"
curl -s http://localhost:8088/metrics | grep "bs_email" | head -20

echo ""
echo "🔍 Provider distribution in metrics:"
curl -s http://localhost:8088/metrics | grep "bs_email_provider_total" | head -10

echo ""
echo "📊 Status distribution in metrics:"
curl -s http://localhost:8088/metrics | grep "bs_email_total.*status" | head -10

echo ""
echo "✅ Test completed! Service is running with realistic patterns."
echo "📊 You can now check Grafana dashboard to see realistic email distribution."
echo "🌐 Metrics available at: http://localhost:8088/metrics"
echo "🏥 Health check at: http://localhost:8088/health"
echo ""
echo "To stop the service, run: kill $SERVICE_PID"
echo "Service PID: $SERVICE_PID"
