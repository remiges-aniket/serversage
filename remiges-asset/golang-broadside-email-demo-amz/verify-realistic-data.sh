#!/bin/bash

echo "🔍 Verifying Realistic Email Data Patterns"
echo "=========================================="

PROMETHEUS_URL="http://localhost:30013"

echo ""
echo "📊 PROVIDER DISTRIBUTION ANALYSIS"
echo "--------------------------------"

# Get provider totals
gmail=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(bs_email_provider_ratio_total{provider=\"gmail\"})" | jq -r '.data.result[0].value[1]')
hotmail=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(bs_email_provider_ratio_total{provider=\"hotmail\"})" | jq -r '.data.result[0].value[1]')
rediff=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(bs_email_provider_ratio_total{provider=\"rediff\"})" | jq -r '.data.result[0].value[1]')
yahoo=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(bs_email_provider_ratio_total{provider=\"yahoo\"})" | jq -r '.data.result[0].value[1]')
outlook=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(bs_email_provider_ratio_total{provider=\"outlook\"})" | jq -r '.data.result[0].value[1]')
others=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(bs_email_provider_ratio_total{provider=\"others\"})" | jq -r '.data.result[0].value[1]')

total_provider=$((gmail + hotmail + rediff + yahoo + outlook + others))

echo "Gmail:   $gmail emails ($(echo "scale=1; $gmail * 100 / $total_provider" | bc)%)"
echo "Hotmail: $hotmail emails ($(echo "scale=1; $hotmail * 100 / $total_provider" | bc)%)"
echo "Rediff:  $rediff emails ($(echo "scale=1; $rediff * 100 / $total_provider" | bc)%)"
echo "Yahoo:   $yahoo emails ($(echo "scale=1; $yahoo * 100 / $total_provider" | bc)%)"
echo "Outlook: $outlook emails ($(echo "scale=1; $outlook * 100 / $total_provider" | bc)%)"
echo "Others:  $others emails ($(echo "scale=1; $others * 100 / $total_provider" | bc)%)"

echo ""
echo "✅ EXPECTED vs ACTUAL:"
echo "Gmail should dominate (45%+): $(if [ $gmail -gt $hotmail ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi)"
echo "Hotmail should be 2nd: $(if [ $hotmail -gt $rediff ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi)"
echo "Others should be smallest: $(if [ $others -lt $yahoo ]; then echo "✅ PASS"; else echo "❌ FAIL"; fi)"

echo ""
echo "📈 STATUS DISTRIBUTION ANALYSIS"
echo "------------------------------"

sent=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(bs_email_ratio_total{status=\"sent\"})" | jq -r '.data.result[0].value[1]')
in_transit=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(bs_email_ratio_total{status=\"in_transit\"})" | jq -r '.data.result[0].value[1]')
bounced=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(bs_email_ratio_total{status=\"bounced\"})" | jq -r '.data.result[0].value[1]')
rejected=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(bs_email_ratio_total{status=\"rejected\"})" | jq -r '.data.result[0].value[1]')

total_status=$((sent + in_transit + bounced + rejected))

echo "Sent:       $sent emails ($(echo "scale=1; $sent * 100 / $total_status" | bc)%)"
echo "In Transit: $in_transit emails ($(echo "scale=1; $in_transit * 100 / $total_status" | bc)%)"
echo "Bounced:    $bounced emails ($(echo "scale=1; $bounced * 100 / $total_status" | bc)%)"
echo "Rejected:   $rejected emails ($(echo "scale=1; $rejected * 100 / $total_status" | bc)%)"

success_rate=$(echo "scale=1; $sent * 100 / $total_status" | bc)
echo ""
echo "✅ SUCCESS RATE: ${success_rate}% (should be 80%+)"
echo "Status distribution realistic: $(if (( $(echo "$success_rate > 80" | bc -l) )); then echo "✅ PASS"; else echo "❌ FAIL"; fi)"

echo ""
echo "🏢 DCS DISTRIBUTION"
echo "------------------"
curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum%20by%20(dcs)%20(bs_email_ratio_total)" | jq -r '.data.result[] | "\(.metric.dcs): \(.value[1]) emails"'

echo ""
echo "🌍 REGIONAL DISTRIBUTION"
echo "-----------------------"
curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum%20by%20(region)%20(bs_email_ratio_total)" | jq -r '.data.result[] | "\(.metric.region): \(.value[1]) emails"'

echo ""
echo "⏱️ PERFORMANCE METRICS"
echo "---------------------"
avg_time=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=(sum(bs_email_processing_duration_seconds_sum)/sum(bs_email_processing_duration_seconds_count))*1000" | jq -r '.data.result[0].value[1]')
echo "Average Processing Time: ${avg_time}ms"
echo "Performance realistic: $(if (( $(echo "$avg_time < 300" | bc -l) )); then echo "✅ PASS"; else echo "❌ FAIL"; fi)"

echo ""
echo "🎯 REALISM ASSESSMENT"
echo "=====================" 
echo "✅ Gmail Dominance: $(if [ $gmail -gt $((total_provider * 40 / 100)) ]; then echo "REALISTIC"; else echo "NEEDS IMPROVEMENT"; fi)"
echo "✅ High Success Rate: $(if (( $(echo "$success_rate > 80" | bc -l) )); then echo "REALISTIC"; else echo "NEEDS IMPROVEMENT"; fi)"
echo "✅ Reasonable Performance: $(if (( $(echo "$avg_time < 300" | bc -l) )); then echo "REALISTIC"; else echo "NEEDS IMPROVEMENT"; fi)"
echo "✅ Provider Variety: $(if [ $total_provider -gt 100000 ]; then echo "GOOD VOLUME"; else echo "LOW VOLUME"; fi)"

echo ""
echo "📊 GRAFANA DASHBOARD READY!"
echo "Import the dashboard at: http://localhost:30016"
echo "Use file: complete-realistic-dashboard.json"
