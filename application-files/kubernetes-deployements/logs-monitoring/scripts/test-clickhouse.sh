#!/bin/bash

set -e

echo "Testing ClickHouse Log Integration..."

# Get the ClickHouse pod name
CLICKHOUSE_POD=$(kubectl -n logging get pod -l app=clickhouse -o jsonpath='{.items[0].metadata.name}')

# Check if ClickHouse is running
echo "Checking ClickHouse status..."
if ! kubectl -n logging get pod -l app=clickhouse | grep -q Running; then
  echo "ClickHouse is not running. Please deploy the stack first."
  exit 1
fi

# Test direct connection to ClickHouse
echo "Testing direct connection to ClickHouse..."
kubectl -n logging exec -it $CLICKHOUSE_POD -- clickhouse-client --user default --password clickhouse --query="SELECT 1"

# Check if the database exists
echo "Checking if fluentbit database exists..."
DB_EXISTS=$(kubectl -n logging exec -it $CLICKHOUSE_POD -- clickhouse-client --user default --password clickhouse --query="SELECT count() FROM system.databases WHERE name = 'fluentbit'")

if [ "$DB_EXISTS" -eq "0" ]; then
  echo "Database fluentbit does not exist. Running initialization script..."
  ../scripts/init-clickhouse.sh
fi

# Run some test queries
echo "Running test queries..."

echo -e "\nTotal log count:"
kubectl -n logging exec -it $CLICKHOUSE_POD -- clickhouse-client --user default --password clickhouse --query="SELECT count() FROM fluentbit.kube"

echo -e "\nLogs by namespace (if any):"
kubectl -n logging exec -it $CLICKHOUSE_POD -- clickhouse-client --user default --password clickhouse --query="SELECT count(), log.kubernetes.namespace_name as namespace FROM fluentbit.kube GROUP BY namespace FORMAT PrettyCompactMonoBlock" || echo "No logs with namespace information yet"

echo -e "\nRecent logs (last 5, if any):"
kubectl -n logging exec -it $CLICKHOUSE_POD -- clickhouse-client --user default --password clickhouse --query="SELECT timestamp, pod_name, log.log FROM fluentbit.kube ORDER BY timestamp DESC LIMIT 5 FORMAT PrettyCompactMonoBlock" || echo "No logs available yet"

echo -e "\nTesting complete!"

# Port-forward to Grafana
echo "To access the Grafana dashboard, run:"
echo "kubectl -n logging port-forward svc/grafana 3000:3000"
echo "Then open http://localhost:3000 in your browser"
echo "Default credentials: admin/admin"
