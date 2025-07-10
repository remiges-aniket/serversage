# Realistic Email Metrics Generator

## Overview
This improved version generates realistic email metrics that look like actual production data instead of evenly distributed test data.

## Key Improvements

### 1. **Realistic Provider Distribution**
- **Gmail**: 45% of all emails (dominant provider)
- **Hotmail**: 20% of all emails
- **Rediff**: 12% of all emails  
- **Yahoo**: 10% of all emails
- **Outlook**: 8% of all emails
- **Others**: 5% of all emails (smallest)

### 2. **Realistic Status Distribution**
Each provider has different delivery success rates:
- **Gmail**: 85% sent, 8% in_transit, 5% bounced, 2% rejected
- **Hotmail**: 82% sent, 10% in_transit, 6% bounced, 2% rejected
- **Rediff**: 78% sent, 12% in_transit, 7% bounced, 3% rejected
- **Yahoo**: 80% sent, 11% in_transit, 6% bounced, 3% rejected
- **Outlook**: 83% sent, 9% in_transit, 5% bounced, 3% rejected
- **Others**: 75% sent, 15% in_transit, 8% bounced, 2% rejected

### 3. **Variable Timing**
- Random intervals between 1-8 seconds (instead of fixed 2 seconds)
- Time-based volume multipliers:
  - Night (12-6 AM): 0.2-0.5x volume
  - Morning (6-9 AM): 0.6-1.0x volume
  - Peak Morning (9-12 PM): 1.2-2.0x volume
  - Lunch (12-2 PM): 0.8-1.2x volume
  - Peak Afternoon (2-5 PM): 1.1-1.8x volume
  - Evening (5-8 PM): 0.7-1.2x volume
  - Night (8-12 AM): 0.3-0.7x volume

### 4. **Realistic Volume Patterns**
- Different email counts based on provider and status
- Gmail gets highest volumes (80-400 emails per batch)
- Others get lowest volumes (1-30 emails per batch)
- Status-based volumes (sent > in_transit > bounced > rejected)

### 5. **Non-uniform State Distribution**
- Most emails concentrated in certain states (0, 20, 40, 60, 80)
- Remaining states (85-99) get less traffic
- Mimics real-world traffic patterns

## Files

### Core Files
- `main_realistic.go` - The improved realistic email generator
- `broadsid-email-realistic` - Compiled binary

### Utility Scripts
- `test_realistic.sh` - Test script to verify functionality
- `monitor_realistic.sh` - Monitor service for 2 minutes
- `show_metrics.sh` - Display current metrics distribution

### Configuration
- `go.mod` & `go.sum` - Go module dependencies
- `otel.yaml` - OpenTelemetry configuration
- `bussiness-dash.json` - Grafana dashboard configuration

## Usage

### 1. Build the Service
```bash
go build -o broadsid-email-realistic main_realistic.go
```

### 2. Run the Service
```bash
./broadsid-email-realistic
```

### 3. Monitor Metrics
```bash
# Quick test
./test_realistic.sh

# Extended monitoring
./monitor_realistic.sh

# View current distribution
./show_metrics.sh
```

### 4. Access Endpoints
- **Metrics**: http://localhost:8088/metrics
- **Health**: http://localhost:8088/health

## Expected Output

### Log Sample
```json
{
  "time": "2025-07-10T12:57:48+05:30",
  "level": "INFO", 
  "msg": "Realistic email batch processed",
  "total_emails": 48,
  "total_sent": 7523,
  "total_in_transit": 215,
  "total_bounced": 56,
  "total_rejected": 5,
  "batch_by_provider": {
    "gmail": {"bounced": 21, "in_transit": 154, "sent": 4241},
    "hotmail": {"bounced": 19, "sent": 381},
    "others": {"in_transit": 10, "sent": 43},
    "rediff": {"in_transit": 17, "rejected": 5, "sent": 442},
    "yahoo": {"in_transit": 19, "sent": 424}
  }
}
```

### Provider Configuration
```
gmail:   45% weight, high volume
hotmail: 20% weight, medium-high volume  
rediff:  12% weight, medium volume
yahoo:   10% weight, medium volume
outlook: 8% weight, medium-low volume
others:  5% weight, low volume
```

## Grafana Dashboard

The existing `bussiness-dash.json` will now show:
- **Realistic pie charts** with Gmail dominating
- **Uneven distribution** across providers
- **Realistic status ratios** with mostly "sent" emails
- **Time-based patterns** showing traffic variations

## Metrics Generated

### Counter Metrics
- `bs_email_total` - Total emails by provider, status, DCS, region
- `bs_email_provider_total` - Total emails by provider and status

### Gauge Metrics  
- `bs_email_current` - Current email count by status
- `bs_email_status_count` - Email count by status

### Histogram Metrics
- `bs_email_processing_duration_seconds` - Processing time distribution

## Key Features

✅ **Realistic Provider Weights** - Gmail dominates as expected  
✅ **Variable Status Rates** - Different success rates per provider  
✅ **Random Timing** - No fixed intervals  
✅ **Time-based Scaling** - Volume changes throughout the day  
✅ **Non-uniform Distribution** - Concentrated traffic patterns  
✅ **Comprehensive Logging** - Detailed batch summaries  
✅ **OpenTelemetry Integration** - Full observability stack  
✅ **Health Monitoring** - Service health endpoints  

## Troubleshooting

### OTLP Connection Errors
The service may show OTLP connection errors if you don't have a collector running:
```
traces export: exporter export timeout: rpc error: code = Unavailable desc = connection error
```
This is normal and doesn't affect the Prometheus metrics endpoint.

### Service Not Starting
Check if port 8088 is available:
```bash
lsof -ti:8088
```

### No Metrics Showing
Ensure the service is running and wait a few seconds for metrics generation:
```bash
curl http://localhost:8088/health
curl http://localhost:8088/metrics | head -20
```

## Comparison: Old vs New

| Aspect | Old Version | New Version |
|--------|-------------|-------------|
| Provider Distribution | Even (16.7% each) | Realistic (Gmail 45%, Others 5%) |
| Status Distribution | Random | Provider-specific rates |
| Timing | Fixed 2-3 seconds | Variable 1-8 seconds |
| Volume Patterns | Static | Time-based scaling |
| State Distribution | Uniform | Concentrated patterns |
| Realism | Test-like | Production-like |

This realistic version will make your Grafana dashboards look much more like actual production email service metrics!
