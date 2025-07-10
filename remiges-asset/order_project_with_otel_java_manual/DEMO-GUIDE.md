# ServerSage - Complete Observability Demo Guide

## 🎯 Demo Overview

ServerSage is a comprehensive Spring Boot application designed to showcase **enterprise-grade observability** with OpenTelemetry. This demo includes optimized load testing, real-time dashboards, distributed tracing, and intelligent alerting.

## 🚀 Quick Demo Setup

### 1. Start the Complete Stack
```bash
# Start the application
./mvnw spring-boot:run &

# Start observability stack (Grafana, Prometheus, Tempo, Loki)
docker-compose up -d

# Wait for services to be ready (30-60 seconds)
./test-setup.sh
```

### 2. Run Optimized Load Test
```bash
# Run the demo-optimized load test
./run-load-test.sh
```

### 3. Access Dashboards
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Tempo**: http://localhost:3200
- **Application**: http://localhost:8081

## 📊 Key Demo Features

### ✅ Optimized Load Testing
- **Realistic User Behavior**: Randomized scenarios with proper think times
- **Low System Impact**: Maximum 5 concurrent users, gentle load patterns
- **Continuous Data Generation**: 9-minute test with varied user patterns
- **Business-Focused**: Weighted scenarios emphasizing order creation and analytics

### ✅ Complete Observability Stack
- **Distributed Tracing**: Full request correlation with Tempo
- **Custom Metrics**: Business and technical metrics with exemplars
- **Structured Logging**: Correlated logs with trace context
- **Real-time Dashboards**: 3 comprehensive Grafana dashboards
- **Intelligent Alerting**: Error detection with webhook notifications

### ✅ Business Intelligence
- **Order Metrics**: Real-time order creation, revenue tracking
- **User Analytics**: Registration rates, role distribution
- **Product Management**: Inventory levels, category performance
- **Performance KPIs**: Response times, error rates, throughput

## 🎪 Demo Scenarios

### Scenario 1: Real-time Business Monitoring
1. Run load test: `./run-load-test.sh`
2. Open Business Dashboard in Grafana
3. Show real-time order creation and revenue metrics
4. Demonstrate metric-to-trace correlation via exemplars

### Scenario 2: Error Detection and Alerting
1. Trigger error scenarios in the load test
2. Show error logs with trace correlation
3. Demonstrate alert firing in Grafana
4. Show webhook notifications in application logs

### Scenario 3: Performance Analysis
1. Open Technical Dashboard
2. Show response time distributions
3. Click exemplar points to view traces in Tempo
4. Analyze performance bottlenecks

### Scenario 4: Full-Stack Troubleshooting
1. Identify error in Business Dashboard
2. Click exemplar to view trace in Tempo
3. Correlate with structured logs
4. Show root cause analysis workflow

## 📈 Dashboard Guide

### Business Dashboard
- **Revenue Metrics**: Real-time order values and trends
- **Order Volume**: Creation rates and status distribution
- **User Growth**: Registration and activity metrics
- **Product Performance**: Category-wise sales and inventory

### Technical Dashboard
- **Response Times**: P50, P95, P99 percentiles with exemplars
- **Error Rates**: HTTP status code distribution
- **Throughput**: Requests per second by endpoint
- **Database Performance**: Query times and connection health

### JVM Dashboard
- **Memory Usage**: Heap, non-heap, and garbage collection
- **Thread Management**: Active threads and pool utilization
- **Performance Metrics**: CPU usage and system resources

## 🔧 Load Test Configuration

### Optimized Settings
```javascript
stages: [
  { duration: '1m', target: 2 },   // Gentle ramp up
  { duration: '3m', target: 3 },   // Main demo period
  { duration: '2m', target: 5 },   // Peak load
  { duration: '2m', target: 3 },   // Stabilization
  { duration: '1m', target: 0 },   // Ramp down
]
```

### Scenario Weights
- **Order Creation**: 25% (highest priority)
- **Analytics Dashboard**: 20% (demo-focused)
- **User Management**: 15%
- **Product Management**: 15%
- **Search Operations**: 15%
- **Error Scenarios**: 5% (controlled)
- **Mixed Workload**: 5%

## 🚨 Alert Configuration

### Configured Alerts
1. **High Error Rate**: >10% error rate for 1 minute
2. **High Response Time**: >2000ms P95 for 2 minutes
3. **Order Creation Failures**: Failed order API calls
4. **Database Connection Issues**: Database error detection

### Alert Endpoints
- **General Alerts**: `POST /api/alerts/webhook`
- **Critical Alerts**: `POST /api/alerts/critical`
- **Warning Alerts**: `POST /api/alerts/warning`
- **Alert History**: `GET /api/alerts/history`

## 🎯 Demo Tips

### For Maximum Impact
1. **Start with Business Dashboard**: Show immediate business value
2. **Demonstrate Exemplars**: Click from metrics to traces
3. **Show Error Correlation**: Trigger errors and show trace correlation
4. **Highlight Automation**: Show alerts firing automatically
5. **End with ROI**: Emphasize reduced MTTR and improved reliability

### Common Demo Points
- "See how we can click from this metric spike directly to the trace"
- "Notice how the error includes the trace ID for immediate correlation"
- "This alert fired automatically when error rate exceeded 10%"
- "We can see the exact database query that's causing the slowdown"

## 🔍 Troubleshooting

### If Order Metrics Don't Appear
1. Check if OrderService has ObservabilityService injection
2. Verify metrics are being incremented in order creation
3. Wait 30-60 seconds for metric collection
4. Check Prometheus targets: http://localhost:9090/targets

### If Traces Don't Appear
1. Verify OpenTelemetry agent is loaded
2. Check collector configuration
3. Ensure Tempo is receiving data
4. Wait for trace ingestion (30-60 seconds)

### If Alerts Don't Fire
1. Check Grafana alerting configuration
2. Verify Prometheus metrics are available
3. Ensure alert thresholds are appropriate
4. Check webhook endpoints are accessible

## 📚 API Endpoints for Manual Testing

### Create Test Data
```bash
# Create User
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo User","email":"demo@test.com","role":"USER"}'

# Create Product
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo Laptop","price":1299.99,"stockQuantity":10,"category":"electronics"}'

# Create Order
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":1,"quantity":1}'
```

### Trigger Errors
```bash
# Array Index Error
curl http://localhost:8081/api/users/999

# Null Pointer Error
curl http://localhost:8081/api/users/998

# Database Error
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"DB Error","email":"dberror@test.com","role":"USER"}'
```

## 🎉 Success Metrics

After running the demo, you should see:
- ✅ Orders being created in real-time
- ✅ Revenue metrics updating in Business Dashboard
- ✅ Response time exemplars linking to traces
- ✅ Error scenarios generating correlated traces
- ✅ Alerts firing for high error rates
- ✅ Complete observability correlation across metrics, traces, and logs

---

**Ready to demonstrate enterprise-grade observability!** 🚀
