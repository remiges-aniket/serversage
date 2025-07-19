#!/bin/bash

set -e

echo "Initializing ClickHouse database and tables..."

# Get the ClickHouse pod name
CLICKHOUSE_POD=$(kubectl -n logging get pod -l app=clickhouse -o jsonpath='{.items[0].metadata.name}')

# Wait for ClickHouse to be ready
echo "Waiting for ClickHouse to be ready..."
until kubectl -n logging exec -it $CLICKHOUSE_POD -- clickhouse-client --user default --password clickhouse --query="SELECT 1" &> /dev/null
do
  echo "Waiting for ClickHouse to be ready..."
  sleep 5
done

echo "ClickHouse is ready. Creating database and tables..."

# Create the database
echo "Creating database fluentbit..."
kubectl -n logging exec -it $CLICKHOUSE_POD -- clickhouse-client --user default --password clickhouse --query="CREATE DATABASE IF NOT EXISTS fluentbit"

# Enable experimental object type for JSON support
echo "Enabling JSON object type..."
kubectl -n logging exec -it $CLICKHOUSE_POD -- clickhouse-client --user default --password clickhouse --query="SET allow_experimental_object_type = 1"

# Create the table for Kubernetes logs
echo "Creating table fluentbit.kube..."
kubectl -n logging exec -it $CLICKHOUSE_POD -- clickhouse-client --user default --password clickhouse --query="
CREATE TABLE IF NOT EXISTS fluentbit.kube
(
    timestamp DateTime,
    level LowCardinality(String),
    msg String,
    namespace_name LowCardinality(String),
    container_name LowCardinality(String),
    pod_name LowCardinality(String),
    pod_ip LowCardinality(String),
    host LowCardinality(String),
    pod_labels JSON,
    log JSON
)
Engine = MergeTree 
ORDER BY tuple(host, pod_name, timestamp)
TTL timestamp + INTERVAL 7 DAY
"

echo "ClickHouse database and tables created successfully!"
