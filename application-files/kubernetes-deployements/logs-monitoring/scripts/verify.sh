#!/bin/bash

set -e

echo "Verifying Log Aggregation Stack with ClickHouse..."

# Check if all pods are running
echo "Checking pod status..."
kubectl get pods -n logging

# Check ClickHouse logs
echo -e "\nChecking ClickHouse logs..."
kubectl -n logging logs -l app=clickhouse --tail=10

# Check Fluent Bit logs
echo -e "\nChecking Fluent Bit logs..."
kubectl -n logging logs -l app=fluent-bit --tail=10

# Check Grafana logs
echo -e "\nChecking Grafana logs..."
kubectl -n logging logs -l app=grafana --tail=10

# Check if logs are being sent to ClickHouse
echo -e "\nChecking if logs are being sent to ClickHouse..."
CLICKHOUSE_POD=$(kubectl -n logging get pod -l app=clickhouse -o jsonpath='{.items[0].metadata.name}')
kubectl -n logging exec -it $CLICKHOUSE_POD -- clickhouse-client --user default --password clickhouse --query="SELECT count() FROM fluentbit.kube" || echo "No logs in ClickHouse yet. Make sure the database and table are created."

# Check test application logs
echo -e "\nChecking test application logs..."
kubectl logs -l app=test-logger --tail=10 || echo "Test logger not deployed yet"

echo -e "\nVerification complete!"
