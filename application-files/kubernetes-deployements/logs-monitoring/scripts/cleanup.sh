#!/bin/bash

set -e

echo "Cleaning up Log Aggregation Stack..."

# Remove test applications if they exist
echo "Removing test applications..."
kubectl delete -f ../manifests/test-apps/ --ignore-not-found

# Remove Grafana
echo "Removing Grafana..."
kubectl delete -f ../manifests/grafana/ --ignore-not-found

# Remove Fluent Bit
echo "Removing Fluent Bit..."
kubectl delete -f ../manifests/fluent-bit/ --ignore-not-found

# Remove ClickHouse
echo "Removing ClickHouse..."
kubectl delete -f ../manifests/clickhouse/ --ignore-not-found

# Optionally remove the namespace
if [ "$1" == "--remove-namespace" ]; then
  echo "Removing logging namespace..."
  kubectl delete namespace logging
fi

echo "Cleanup complete!"
