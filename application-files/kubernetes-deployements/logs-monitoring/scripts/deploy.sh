#!/bin/bash

set -e

echo "Deploying Log Aggregation Stack with ClickHouse..."

# Create namespace if it doesn't exist
kubectl create namespace logging --dry-run=client -o yaml | kubectl apply -f -

# Deploy ClickHouse
echo "Deploying ClickHouse..."
kubectl apply -f ../manifests/clickhouse/clickhouse-deployment.yaml

# Deploy Fluent Bit
echo "Deploying Fluent Bit..."
kubectl apply -f ../manifests/fluent-bit/

# Deploy Grafana
echo "Deploying Grafana..."
kubectl apply -f ../manifests/grafana/

# Wait for deployments to be ready
echo "Waiting for ClickHouse to be ready..."
kubectl -n logging wait --for=condition=ready --timeout=300s pod -l app=clickhouse

echo "Waiting for Fluent Bit to be ready..."
kubectl -n logging wait --for=condition=ready --timeout=300s pod -l app=fluent-bit

echo "Waiting for Grafana to be ready..."
kubectl -n logging wait --for=condition=ready --timeout=300s pod -l app=grafana

# Initialize ClickHouse database and tables
echo "Initializing ClickHouse database and tables..."
chmod +x ../scripts/init-clickhouse.sh
../scripts/init-clickhouse.sh

# Deploy test application
echo "Deploying test application..."
kubectl apply -f ../manifests/test-apps/test-logger.yaml

echo "Log aggregation stack deployed successfully!"
echo "To access Grafana, run: kubectl -n logging port-forward svc/grafana 3000:3000"
echo "Then open http://localhost:3000 in your browser"
echo "Default credentials: admin/admin"
