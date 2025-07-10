#!/bin/bash

# ServerSage Demo Verification Script
# Comprehensive verification of the optimized demo setup

set -e

echo "🔍 ServerSage Demo Setup Verification"
echo "====================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

success_count=0
total_checks=0

check_service() {
    local service_name=$1
    local url=$2
    local expected_content=$3
    
    total_checks=$((total_checks + 1))
    echo -n "Checking $service_name... "
    
    if curl -s "$url" | grep -q "$expected_content"; then
        echo -e "${GREEN}✅ OK${NC}"
        success_count=$((success_count + 1))
    else
        echo -e "${RED}❌ FAILED${NC}"
    fi
}

echo "1️⃣ Verifying Core Services"
echo "-------------------------"

check_service "Application Health" "http://localhost:8081/actuator/health" "UP"
check_service "Prometheus" "http://localhost:9090/-/healthy" "Prometheus"
check_service "Grafana" "http://localhost:3000/api/health" "ok"
check_service "Tempo" "http://localhost:3200/ready" "ready"

echo ""
echo "2️⃣ Verifying API Endpoints"
echo "--------------------------"

check_service "Users API" "http://localhost:8081/api/users" "\\[\\]\\|id"
check_service "Products API" "http://localhost:8081/api/products" "\\[\\]\\|id"
check_service "Orders API" "http://localhost:8081/api/orders" "\\[\\]\\|id"
check_service "Analytics API" "http://localhost:8081/api/analytics/dashboard" "totalUsers\\|users"
check_service "Alert Status API" "http://localhost:8081/api/alerts/status" "totalAlerts"

echo ""
echo "3️⃣ Verifying Observability Features"
echo "-----------------------------------"

# Create test data for metrics verification
echo "Creating test data for verification..."

# Create user
USER_RESPONSE=$(curl -s -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Verify User","email":"verify@demo.com","role":"USER"}' || echo "")

# Create product  
PRODUCT_RESPONSE=$(curl -s -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Verify Product","price":99.99,"stockQuantity":5,"category":"test"}' || echo "")

# Wait for metrics collection
sleep 3

check_service "HTTP Metrics" "http://localhost:9090/api/v1/query?query=serversage_http_requests_total" "result"
check_service "User Metrics" "http://localhost:9090/api/v1/query?query=serversage_users_total" "result"

echo ""
echo "4️⃣ Verifying Load Test Configuration"
echo "-----------------------------------"

total_checks=$((total_checks + 1))
if [ -f "./k6-tests/optimized-demo-test.js" ]; then
    echo -e "Optimized K6 Script: ${GREEN}✅ EXISTS${NC}"
    success_count=$((success_count + 1))
else
    echo -e "Optimized K6 Script: ${RED}❌ MISSING${NC}"
fi

total_checks=$((total_checks + 1))
if [ -x "./run-load-test.sh" ]; then
    echo -e "Load Test Runner: ${GREEN}✅ EXECUTABLE${NC}"
    success_count=$((success_count + 1))
else
    echo -e "Load Test Runner: ${RED}❌ NOT EXECUTABLE${NC}"
fi

echo ""
echo "5️⃣ Verifying Alert Configuration"
echo "-------------------------------"

total_checks=$((total_checks + 1))
if [ -f "./observability/grafana/provisioning/alerting/alert-rules.yml" ]; then
    echo -e "Alert Rules: ${GREEN}✅ CONFIGURED${NC}"
    success_count=$((success_count + 1))
else
    echo -e "Alert Rules: ${RED}❌ MISSING${NC}"
fi

total_checks=$((total_checks + 1))
if [ -f "./observability/grafana/provisioning/alerting/notification-policies.yml" ]; then
    echo -e "Notification Policies: ${GREEN}✅ CONFIGURED${NC}"
    success_count=$((success_count + 1))
else
    echo -e "Notification Policies: ${RED}❌ MISSING${NC}"
fi

echo ""
echo "6️⃣ Testing Error Scenarios"
echo "-------------------------"

# Test error endpoint
ERROR_RESPONSE=$(curl -s -w "%{http_code}" http://localhost:8081/api/users/999 || echo "000")
total_checks=$((total_checks + 1))
if echo "$ERROR_RESPONSE" | grep -q "500"; then
    echo -e "Error Scenario (999): ${GREEN}✅ WORKING${NC}"
    success_count=$((success_count + 1))
else
    echo -e "Error Scenario (999): ${RED}❌ NOT WORKING${NC}"
fi

echo ""
echo "📊 Verification Summary"
echo "======================"
echo "Successful checks: $success_count/$total_checks"

if [ $success_count -eq $total_checks ]; then
    echo -e "${GREEN}🎉 ALL CHECKS PASSED! Demo is ready.${NC}"
    echo ""
    echo "🚀 Next Steps:"
    echo "1. Run load test: ./run-load-test.sh"
    echo "2. Open Grafana: http://localhost:3000 (admin/admin)"
    echo "3. Monitor dashboards during load test"
    echo "4. Check alert firing in application logs"
    exit 0
else
    echo -e "${YELLOW}⚠️  Some checks failed. Please review the issues above.${NC}"
    echo ""
    echo "🔧 Common fixes:"
    echo "- Ensure all services are running: docker-compose up -d"
    echo "- Wait for services to be ready (30-60 seconds)"
    echo "- Check application logs: tail -f app.log"
    exit 1
fi
