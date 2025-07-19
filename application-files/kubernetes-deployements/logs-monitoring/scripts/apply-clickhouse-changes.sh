#!/bin/bash

set -e

echo "Applying ClickHouse configuration changes..."

# Apply the updated ClickHouse deployment
kubectl apply -f ./manifests/clickhouse/clickhouse-deployment.yaml

# Get the ClickHouse pod name
CLICKHOUSE_POD=$(kubectl -n logging get pod -l app=clickhouse -o jsonpath='{.items[0].metadata.name}')

# Wait for ClickHouse to be ready after configuration changes
echo "Waiting for ClickHouse to be ready..."
until kubectl -n logging exec -it $CLICKHOUSE_POD -- clickhouse-client --user default --password clickhouse --query="SELECT 1" &> /dev/null
do
  echo "Waiting for ClickHouse to be ready..."
  sleep 5
done

echo "ClickHouse is ready. Applying TTL to existing table..."

# Apply TTL to existing table if it exists
kubectl -n logging exec -it $CLICKHOUSE_POD -- clickhouse-client --user default --password clickhouse --query="
ALTER TABLE IF EXISTS fluentbit.kube MODIFY TTL timestamp + INTERVAL 7 DAY
"

echo "ClickHouse configuration updated successfully!"
