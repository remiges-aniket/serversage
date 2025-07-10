#!/bin/bash

# ServerSage Setup Test Script
# This script tests the complete observability setup

set -e

echo "🧪 ServerSage Setup Test"
echo "========================"

# Test 1: Check if application is running
echo "1️⃣ Testing application health..."
if curl -s http://localhost:8081/actuator/health | grep -q "UP"; then
    echo "✅ Application is healthy"
else
    echo "❌ Application is not running or unhealthy"
    exit 1
fi

# Test 2: Check observability stack
echo ""
echo "2️⃣ Testing observability stack..."

# Check Prometheus
if curl -s http://localhost:9090/-/healthy | grep -q "Prometheus"; then
    echo "✅ Prometheus is running"
else
    echo "❌ Prometheus is not accessible"
fi

# Check Grafana
if curl -s http://localhost:3000/api/health | grep -q "ok"; then
    echo "✅ Grafana is running"
else
    echo "❌ Grafana is not accessible"
fi

# Check Tempo
if curl -s http://localhost:3200/ready | grep -q "ready"; then
    echo "✅ Tempo is running"
else
    echo "❌ Tempo is not accessible"
fi

# Test 3: Create test data
echo ""
echo "3️⃣ Creating test data..."

# Create a test user
USER_RESPONSE=$(curl -s -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@demo.com","role":"USER"}')

if echo "$USER_RESPONSE" | grep -q "id"; then
    echo "✅ User creation successful"
    USER_ID=$(echo "$USER_RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
else
    echo "❌ User creation failed"
    USER_ID=1
fi

# Create a test product
PRODUCT_RESPONSE=$(curl -s -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Laptop","description":"Demo laptop","price":999.99,"stockQuantity":10,"category":"electronics"}')

if echo "$PRODUCT_RESPONSE" | grep -q "id"; then
    echo "✅ Product creation successful"
    PRODUCT_ID=$(echo "$PRODUCT_RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
else
    echo "❌ Product creation failed"
    PRODUCT_ID=1
fi

# Create a test order
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d "{\"userId\":$USER_ID,\"productId\":$PRODUCT_ID,\"quantity\":1}")

if echo "$ORDER_RESPONSE" | grep -q "id"; then
    echo "✅ Order creation successful"
    ORDER_ID=$(echo "$ORDER_RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
else
    echo "❌ Order creation failed"
    ORDER_ID=1
fi

# Test 4: Check metrics
echo ""
echo "4️⃣ Testing metrics collection..."

sleep 2  # Wait for metrics to be collected

# Check if order metrics are available
if curl -s "http://localhost:9090/api/v1/query?query=serversage_orders_total" | grep -q "result"; then
    echo "✅ Order metrics are being collected"
else
    echo "⚠️ Order metrics not yet available (may need more time)"
fi

# Check HTTP metrics
if curl -s "http://localhost:9090/api/v1/query?query=serversage_http_requests_total" | grep -q "result"; then
    echo "✅ HTTP metrics are being collected"
else
    echo "⚠️ HTTP metrics not yet available"
fi

# Test 5: Trigger an error for observability
echo ""
echo "5️⃣ Testing error scenarios..."

# Trigger a known error
ERROR_RESPONSE=$(curl -s -w "%{http_code}" http://localhost:8081/api/users/999)
if echo "$ERROR_RESPONSE" | grep -q "500"; then
    echo "✅ Error scenario triggered successfully"
else
    echo "⚠️ Error scenario may not have triggered as expected"
fi

# Test 6: Check alert endpoints
echo ""
echo "6️⃣ Testing alert endpoints..."

ALERT_STATUS=$(curl -s http://localhost:8081/api/alerts/status)
if echo "$ALERT_STATUS" | grep -q "totalAlerts"; then
    echo "✅ Alert endpoints are working"
else
    echo "❌ Alert endpoints are not working"
fi

echo ""
echo "🎉 Setup test completed!"
echo ""
echo "📊 Next steps:"
echo "   1. Run the optimized load test: ./run-load-test.sh"
echo "   2. Open Grafana: http://localhost:3000 (admin/admin)"
echo "   3. Check the dashboards for real-time data"
echo "   4. Monitor alerts in the application logs"
echo ""
echo "💡 Created test data:"
echo "   - User ID: $USER_ID"
echo "   - Product ID: $PRODUCT_ID" 
echo "   - Order ID: $ORDER_ID"
