#!/bin/bash

echo "🔍 Testing Trace Isolation"
echo "=========================="

echo ""
echo "1️⃣ Testing User Profile Endpoint"
echo "GET /api/users/1096/profile"
curl -s http://localhost:8081/api/users/1096/profile | jq '.'
echo ""

echo "2️⃣ Testing Product Creation"
echo "POST /api/products"
curl -s -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Isolation Test Product","description":"Testing trace isolation","price":299.99,"stockQuantity":5,"category":"test"}' | jq '.'
echo ""

echo "3️⃣ Testing Order Creation"
echo "POST /api/orders"
curl -s -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1096,"productId":1068,"quantity":2}' | jq '.'
echo ""

echo "4️⃣ Testing Analytics Dashboard"
echo "GET /api/analytics/dashboard"
curl -s http://localhost:8081/api/analytics/dashboard | jq '.'
echo ""

echo "5️⃣ Testing Error Scenario (should have isolated error trace)"
echo "GET /api/users/999 (triggers error)"
curl -s http://localhost:8081/api/users/999 | jq '.'
echo ""

echo "✅ Trace isolation test completed!"
echo "Check app.log for trace IDs - each request should have its own unique trace ID"
echo ""
echo "To verify trace isolation:"
echo "  tail -f app.log | grep -E '\\[.*,.*\\]' | head -20"
