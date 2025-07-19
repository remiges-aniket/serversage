# Kubernetes Log Aggregation System with ClickHouse

This repository contains a complete solution for collecting, storing, and visualizing logs from Kubernetes clusters using Fluent Bit, ClickHouse, and Grafana.

## Prerequisites

1.  **Kubernetes Cluster**: A running Kubernetes cluster is required to deploy the logging stack.
2.  **Namespace "logging"**: The `logging` namespace must be created in your Kubernetes cluster before deployment.

## Components

1. **Fluent Bit**: Collects logs from Kubernetes pods and enriches them with metadata
2. **ClickHouse**: Stores and indexes logs for fast querying
3. **Grafana**: Visualizes logs and provides alerting capabilities

## Directory Structure

```
log-aggregator/
├── manifests/
│   ├── fluent-bit/
│   ├── clickhouse/
│   ├── grafana/
│   └── test-apps/
├── dashboards/
├── scripts/
└── docs/
```

## Architecture

   ![System Architecture Diagram](architecture.png "Overview of the log aggregation flow")


## Quick Start

1. Deploy the entire stack:
   ```
   ./scripts/deploy.sh
   ```

2. Create `database` and `table` schema in clickhouse:
   ```
   ./scripts/init-clickhouse.sh
   ```

3. Verify the deployment:
   ```
   ./scripts/verify.sh
   ```

4. Access Grafana:
   - URL: http://localhost:3000 (when port-forwarded)
   - Default credentials: admin/admin

## Key Features

- **Structured Logs**: Kubernetes logs are parsed and stored in a structured format
- **Metadata Enrichment**: Logs are enriched with Kubernetes metadata (namespace, pod, container, etc.)
- **Fast Queries**: ClickHouse provides high-performance log querying
- **Interactive Dashboards**: Grafana dashboards for log analysis and visualization
- **Scalable Architecture**: Components can be scaled independently to handle large log volumes

## Detailed Documentation

- [Setup Guide](docs/SETUP.md)
- [Configuration Guide](docs/CONFIGURATION.md)
- [ClickHouse Integration](docs/CLICKHOUSE-INTEGRATION.md)
- [Alerting Guide](docs/ALERTING.md)
- [Troubleshooting](docs/TROUBLESHOOTING.md)

## Maintenance

- To clean up resources (NOTE : this will only delete services from namespace):
  ```
  ./scripts/cleanup.sh
  ```


## Testing

The system includes a test application that generates sample logs:

```bash
kubectl apply -f manifests/test-apps/test-logger.yaml
```

This will create a pod that generates INFO, DEBUG, and ERROR logs for testing.

To view data from table in clickhouse:

```bash
SELECT * FROM "fluentbit"."kube" LIMIT 100
```

To delete data from table in clickhouse:

```bash
TRUNCATE TABLE "fluentbit"."kube";

```