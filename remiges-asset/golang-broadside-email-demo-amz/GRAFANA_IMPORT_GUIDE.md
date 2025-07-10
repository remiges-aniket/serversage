# 📊 Grafana Dashboard Import Guide

## Dashboard Overview
This realistic email service dashboard shows production-like metrics with:
- **Gmail dominance** (66%+ of traffic)
- **Realistic status distribution** (96%+ success rate)
- **Time-based patterns** and **regional distribution**
- **Performance metrics** and **processing times**

## 🚀 Quick Import Steps

### Method 1: Manual Import via Grafana UI

1. **Open Grafana**: http://localhost:30016
2. **Login** (if required)
3. **Go to Dashboards** → **New** → **Import**
4. **Copy and paste** the content from `complete-realistic-dashboard.json`
5. **Click Import**

### Method 2: Direct File Import

1. **Download** the `complete-realistic-dashboard.json` file
2. **Open Grafana**: http://localhost:30016
3. **Go to Dashboards** → **New** → **Import**
4. **Upload JSON file** → Select `complete-realistic-dashboard.json`
5. **Configure datasource** as "prometheus" (should auto-detect)
6. **Click Import**

## 📊 Dashboard Panels

### Top Row - Key Metrics
1. **📧 Total Emails Processed** - Running total count
2. **📈 Emails/Minute** - Current processing rate
3. **✅ Success Rate** - Percentage of successfully sent emails
4. **⏱️ Avg Processing Time** - Average processing duration in ms

### Main Visualizations
5. **📊 Email Volume by Provider** - Pie chart showing Gmail dominance
6. **📈 Email Status Distribution** - Status breakdown (sent/in_transit/bounced/rejected)
7. **📈 Email Processing Rate by Provider** - Time series showing processing rates
8. **🏢 Email Volume by DCS** - Data center distribution
9. **🌍 Email Volume by Region** - Regional distribution
10. **⏱️ Processing Duration Percentiles** - Performance metrics (50th/95th percentile)

## 🔧 Datasource Configuration

Make sure your Prometheus datasource is configured as:
- **Name**: `prometheus`
- **URL**: `http://localhost:30013` (or your Prometheus URL)
- **Access**: Server (default)

## 📈 Expected Results

### Provider Distribution (Realistic)
- **Gmail**: ~66% (dominant)
- **Hotmail**: ~17%
- **Rediff**: ~7%
- **Yahoo**: ~5%
- **Outlook**: ~3%
- **Others**: ~2%

### Status Distribution
- **Sent**: ~96% (high success rate)
- **In Transit**: ~2.5%
- **Bounced**: ~0.8%
- **Rejected**: ~0.1%

### Performance
- **Processing Time**: ~120ms average
- **Throughput**: Variable based on time-of-day patterns

## 🧪 Testing Dashboard Queries

Run the test script to verify all queries work:
```bash
./test-dashboard-queries.sh
```

## 🔍 Troubleshooting

### Dashboard Not Loading
1. Check Prometheus is accessible: http://localhost:30013
2. Verify email service is running: `ps aux | grep broadsid-email-realistic`
3. Check metrics are being generated: `curl http://localhost:8088/metrics`

### No Data in Panels
1. Verify datasource configuration in Grafana
2. Check time range (use "Last 15 minutes")
3. Ensure metrics exist: `curl "http://localhost:30013/api/v1/query?query=bs_email_ratio_total"`

### Authentication Issues
If Grafana requires authentication:
1. Use default credentials (admin/admin)
2. Or check your Grafana configuration
3. Create API key for programmatic access

## 📊 Dashboard Features

### ✅ Working Features
- Real-time updates (5-second refresh)
- Realistic provider distribution
- Time-based patterns
- Interactive legends
- Drill-down capabilities
- Performance monitoring

### 🎯 Business Value
- **Provider Performance**: Compare email success rates by provider
- **Capacity Planning**: Monitor processing rates and identify bottlenecks
- **Regional Analysis**: Understand geographic distribution
- **SLA Monitoring**: Track success rates and processing times
- **Operational Insights**: Real-time visibility into email operations

## 🔗 URLs
- **Grafana**: http://localhost:30016
- **Prometheus**: http://localhost:30013
- **Email Service Metrics**: http://localhost:8088/metrics
- **Email Service Health**: http://localhost:8088/health

## 📝 Notes
- Dashboard auto-refreshes every 5 seconds
- Time range set to "Last 15 minutes" by default
- All panels use realistic data patterns
- Gmail dominance reflects real-world email provider usage
- Processing times and success rates are realistic for production systems
