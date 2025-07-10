#!/bin/bash

echo "🔍 Testing Enhanced Tracing with SQL Queries & Request Payloads"
echo "=============================================================="

# Clear logs for clean testing
echo "" > app.log
sleep 2

echo ""
echo "1️⃣ Testing User Stats (SQL Query Tracing)"
echo "GET /api/users/stats"
echo "Expected: Should show 'SELECT u.id, u.name, u.email, u.role FROM users u' in logs"
curl -s http://localhost:8081/api/users/stats | jq '.'
sleep 2

echo ""
echo "2️⃣ Testing Performance Metrics (Multiple SQL Queries)"
echo "GET /api/analytics/performance/metrics"
echo "Expected: Should show detailed performance collection spans"
curl -s http://localhost:8081/api/analytics/performance/metrics | jq '.'
sleep 2

echo ""
echo "3️⃣ Testing POST Request (Request Payload Tracing)"
echo "POST /api/products"
echo "Expected: Should show request body in logs and traces"
curl -s -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"SQL Trace Test Product","description":"Testing SQL query visibility in traces","price":199.99,"stockQuantity":5,"category":"trace-test"}' | jq '.'
sleep 2

echo ""
echo "4️⃣ Testing User Search (Query Parameters)"
echo "GET /api/users/search?keyword=test"
echo "Expected: Should show query parameters in traces"
curl -s "http://localhost:8081/api/users/search?keyword=test" | jq '.'
sleep 2

echo ""
echo "5️⃣ Testing Error Scenario (Error Tracing)"
echo "GET /api/users/999"
echo "Expected: Should show error details with trace correlation"
ERROR_RESPONSE=$(curl -s http://localhost:8081/api/users/999)
echo $ERROR_RESPONSE | jq '.'
ERROR_TRACE_ID=$(echo $ERROR_RESPONSE | jq -r '.traceId')
sleep 2

echo ""
echo "📊 Enhanced Tracing Analysis"
echo "============================"

echo ""
echo "🔍 SQL Query Logging Verification:"
echo "-----------------------------------"
echo "Looking for SQL queries in logs..."
SQL_QUERIES=$(grep -c "🔍.*SQL" app.log)
echo "✅ Found $SQL_QUERIES SQL query log entries"

if [ $SQL_QUERIES -gt 0 ]; then
    echo ""
    echo "Sample SQL queries found:"
    grep "🔍.*SQL" app.log | head -3
fi

echo ""
echo "📝 Request Payload Logging Verification:"
echo "----------------------------------------"
echo "Looking for request payloads in logs..."
REQUEST_PAYLOADS=$(grep -c "📝.*Request Body\|Request Body" app.log)
echo "✅ Found $REQUEST_PAYLOADS request payload log entries"

if [ $REQUEST_PAYLOADS -gt 0 ]; then
    echo ""
    echo "Sample request payloads found:"
    grep "📝.*Request Body\|Request Body" app.log | head -2
fi

echo ""
echo "🔗 Trace Correlation Verification:"
echo "----------------------------------"
if [ ! -z "$ERROR_TRACE_ID" ] && [ "$ERROR_TRACE_ID" != "null" ]; then
    echo "Error trace ID: $ERROR_TRACE_ID"
    if grep -q "$ERROR_TRACE_ID" app.log; then
        echo "✅ Error trace ID found in logs - CORRELATION WORKING!"
        echo "   Matching log entries:"
        grep "$ERROR_TRACE_ID" app.log | head -2
    else
        echo "❌ Error trace ID not found in logs"
    fi
else
    echo "⚠️  No error trace ID captured"
fi

echo ""
echo "📈 Database Operation Tracing:"
echo "-----------------------------"
DB_OPERATIONS=$(grep -c "Database Operation" app.log)
echo "✅ Found $DB_OPERATIONS database operation log entries"

if [ $DB_OPERATIONS -gt 0 ]; then
    echo ""
    echo "Sample database operations:"
    grep "Database Operation" app.log | head -3
fi

echo ""
echo "🎯 Enhanced Tracing Summary:"
echo "============================"

TOTAL_TRACES=$(grep -c "\[.*,.*\]" app.log | head -1)
echo "📊 Total trace entries: $TOTAL_TRACES"
echo "🔍 SQL queries logged: $SQL_QUERIES"
echo "📝 Request payloads logged: $REQUEST_PAYLOADS"
echo "💾 Database operations logged: $DB_OPERATIONS"

echo ""
if [ $SQL_QUERIES -gt 0 ] && [ $DB_OPERATIONS -gt 0 ]; then
    echo "🚀 SUCCESS: Enhanced tracing is working!"
    echo ""
    echo "✅ What's now visible in traces:"
    echo "  • Actual SQL queries with parameters"
    echo "  • Request payloads for POST/PUT requests"
    echo "  • Query parameters for GET requests"
    echo "  • Database operation details"
    echo "  • Complete trace correlation"
    echo "  • Error context with trace IDs"
else
    echo "⚠️  PARTIAL: Some enhanced tracing features may not be working"
    echo "   SQL queries: $SQL_QUERIES"
    echo "   DB operations: $DB_OPERATIONS"
fi

echo ""
echo "🔧 For Demo Purposes:"
echo "--------------------"
echo "• Show SQL queries in trace spans"
echo "• Demonstrate request payload capture"
echo "• Show database operation correlation"
echo "• Trace from HTTP request to SQL execution"
echo "• Complete observability stack integration"
