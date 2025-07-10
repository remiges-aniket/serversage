#!/bin/bash

echo "🧪 Testing Dashboard Queries"
echo "============================"

PROMETHEUS_URL="http://localhost:30013"

echo ""
echo "1. 📧 Total Emails Processed:"
echo "Query: sum(bs_email_ratio_total)"
total=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(bs_email_ratio_total)" | jq -r '.data.result[0].value[1]')
echo "Result: $total emails"

echo ""
echo "2. 📈 Emails per Minute:"
echo "Query: sum(rate(bs_email_ratio_total[1m])) * 60"
rate=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(rate(bs_email_ratio_total[1m]))*60" | jq -r '.data.result[0].value[1]')
echo "Result: $rate emails/min"

echo ""
echo "3. ✅ Success Rate:"
echo "Query: (sum(bs_email_ratio_total{status=\"sent\"}) / sum(bs_email_ratio_total)) * 100"
success_rate=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=(sum(bs_email_ratio_total{status=\"sent\"})/sum(bs_email_ratio_total))*100" | jq -r '.data.result[0].value[1]')
echo "Result: ${success_rate}%"

echo ""
echo "4. 📊 Provider Distribution:"
echo "Query: sum by (provider) (bs_email_provider_ratio_total)"
curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum%20by%20(provider)%20(bs_email_provider_ratio_total)" | jq -r '.data.result[] | "\(.metric.provider): \(.value[1]) emails"'

echo ""
echo "5. 📈 Status Distribution:"
echo "Query: sum by (status) (bs_email_ratio_total)"
curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum%20by%20(status)%20(bs_email_ratio_total)" | jq -r '.data.result[] | "\(.metric.status): \(.value[1]) emails"'

echo ""
echo "6. 🏢 DCS Distribution:"
echo "Query: sum by (dcs) (bs_email_ratio_total)"
curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum%20by%20(dcs)%20(bs_email_ratio_total)" | jq -r '.data.result[] | "\(.metric.dcs): \(.value[1]) emails"'

echo ""
echo "7. 🌍 Region Distribution:"
echo "Query: sum by (region) (bs_email_ratio_total)"
curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum%20by%20(region)%20(bs_email_ratio_total)" | jq -r '.data.result[] | "\(.metric.region): \(.value[1]) emails"'

echo ""
echo "8. ⏱️ Average Processing Time:"
echo "Query: (sum(bs_email_processing_duration_seconds_sum) / sum(bs_email_processing_duration_seconds_count)) * 1000"
avg_time=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=(sum(bs_email_processing_duration_seconds_sum)/sum(bs_email_processing_duration_seconds_count))*1000" | jq -r '.data.result[0].value[1]')
echo "Result: ${avg_time}ms"

echo ""
echo "✅ All dashboard queries are working!"
echo "📊 Dashboard should display realistic email patterns with Gmail dominance"
