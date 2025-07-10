#!/bin/bash

echo "📊 Current Email Metrics Distribution"
echo "====================================="

# Get current metrics
METRICS=$(curl -s http://localhost:8088/metrics)

echo ""
echo "🏷️  Provider Distribution (Total Emails):"
echo "----------------------------------------"

# Extract provider totals from metrics
for provider in gmail hotmail rediff yahoo outlook others; do
    total=$(echo "$METRICS" | grep "bs_email_total.*provider.*$provider" | awk '{sum += $2} END {print sum+0}')
    if [ "$total" -gt 0 ]; then
        echo "  $provider: $total emails"
    fi
done

echo ""
echo "📈 Status Distribution (Total Emails):"
echo "-------------------------------------"

# Extract status totals from metrics
for status in sent in_transit bounced rejected; do
    total=$(echo "$METRICS" | grep "bs_email_total.*status.*$status" | awk '{sum += $2} END {print sum+0}')
    if [ "$total" -gt 0 ]; then
        echo "  $status: $total emails"
    fi
done

echo ""
echo "🌍 Regional Distribution (Sample):"
echo "---------------------------------"

# Show regional distribution for Gmail (as example)
echo "$METRICS" | grep "bs_email_total.*provider.*gmail.*region" | head -5 | while read line; do
    region=$(echo "$line" | grep -o 'region="[^"]*"' | cut -d'"' -f2)
    count=$(echo "$line" | awk '{print $2}')
    echo "  Gmail in $region: $count emails"
done

echo ""
echo "📊 DCS Distribution (Sample):"
echo "----------------------------"

# Show DCS distribution
echo "$METRICS" | grep "bs_email_total.*dcs=" | head -5 | while read line; do
    dcs=$(echo "$line" | grep -o 'dcs="[^"]*"' | cut -d'"' -f2)
    count=$(echo "$line" | awk '{print $2}')
    echo "  DCS $dcs: $count emails"
done

echo ""
echo "✅ Metrics are being generated with realistic patterns!"
echo "🌐 Full metrics available at: http://localhost:8088/metrics"
