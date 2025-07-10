#!/bin/bash

echo "🚀 Starting Realistic Email Service Monitor..."

# Kill any existing process
pkill -f broadsid-email-realistic 2>/dev/null || true
sleep 2

# Start the service
echo "📧 Starting realistic email service..."
./broadsid-email-realistic &
SERVICE_PID=$!
echo "✅ Service started with PID: $SERVICE_PID"

# Wait for service to initialize
sleep 5

# Function to show provider distribution
show_provider_stats() {
    echo "📊 Provider Distribution:"
    echo "========================"
    
    # Get total counts by provider
    for provider in gmail hotmail rediff yahoo outlook others; do
        total=$(curl -s http://localhost:8088/metrics | grep "bs_email_total.*provider.*$provider" | grep -o 'bs_email_total[^}]*}[[:space:]]*[0-9]*' | awk '{sum += $2} END {print sum+0}')
        echo "$provider: $total emails"
    done
    echo ""
}

# Function to show status distribution  
show_status_stats() {
    echo "📈 Status Distribution:"
    echo "======================"
    
    for status in sent in_transit bounced rejected; do
        total=$(curl -s http://localhost:8088/metrics | grep "bs_email_total.*status.*$status" | grep -o 'bs_email_total[^}]*}[[:space:]]*[0-9]*' | awk '{sum += $2} END {print sum+0}')
        echo "$status: $total emails"
    done
    echo ""
}

# Monitor for 2 minutes
echo "🔍 Monitoring realistic email patterns for 2 minutes..."
echo "Press Ctrl+C to stop monitoring"

for i in {1..8}; do
    echo "--- Update $i ($(date)) ---"
    show_provider_stats
    show_status_stats
    echo "Waiting 15 seconds..."
    sleep 15
done

echo "✅ Monitoring completed!"
echo "🌐 Metrics endpoint: http://localhost:8088/metrics"
echo "🏥 Health endpoint: http://localhost:8088/health"
echo "📊 Service PID: $SERVICE_PID"
echo ""
echo "To stop the service: kill $SERVICE_PID"
