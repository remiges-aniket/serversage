#!/bin/bash

echo "🚀 Starting Realistic Email Service"
echo "=================================="

# Kill any existing process
pkill -f broadsid-email-realistic 2>/dev/null || true
sleep 2

# Build if needed
if [ ! -f "./broadsid-email-realistic" ]; then
    echo "📦 Building realistic email service..."
    go build -o broadsid-email-realistic main_realistic.go
fi

# Start the service
echo "🎯 Starting realistic email service..."
./broadsid-email-realistic &
SERVICE_PID=$!

# Wait for startup
sleep 3

# Check if running
if ps -p $SERVICE_PID > /dev/null; then
    echo "✅ Service started successfully!"
    echo "📊 Service PID: $SERVICE_PID"
    echo ""
    echo "🌐 Endpoints:"
    echo "   Metrics: http://localhost:8088/metrics"
    echo "   Health:  http://localhost:8088/health"
    echo ""
    echo "📈 Provider Distribution (Realistic):"
    echo "   Gmail:   45% (dominant)"
    echo "   Hotmail: 20%"
    echo "   Rediff:  12%"
    echo "   Yahoo:   10%"
    echo "   Outlook:  8%"
    echo "   Others:   5% (smallest)"
    echo ""
    echo "⏰ Features:"
    echo "   ✓ Variable timing (1-8 seconds)"
    echo "   ✓ Time-based volume scaling"
    echo "   ✓ Realistic status distribution"
    echo "   ✓ Provider-specific success rates"
    echo "   ✓ Non-uniform state patterns"
    echo ""
    echo "🛑 To stop: kill $SERVICE_PID"
    echo "📊 To monitor: ./monitor_realistic.sh"
    echo "📈 To view metrics: ./show_metrics.sh"
else
    echo "❌ Failed to start service!"
    exit 1
fi
