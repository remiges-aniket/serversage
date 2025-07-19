# ClickHouse Integration Summary

This document summarizes the key points from the article "Sending Kubernetes logs To ClickHouse with Fluent Bit" (https://clickhouse.com/blog/kubernetes-logs-to-clickhouse-fluent-bit).

## Overview

The integration allows collecting Kubernetes logs using Fluent Bit, enriching them with Kubernetes metadata, and storing them in ClickHouse for efficient querying and analysis.

## Key Components

1. **Fluent Bit**: Collects logs from Kubernetes pods and enriches them with metadata
2. **ClickHouse**: Stores and indexes logs for fast querying
3. **Kubernetes API**: Provides metadata for log enrichment

## Benefits

- Enriches logs with Kubernetes context (namespace, pod, container, etc.)
- Structures logs for efficient querying
- Enables advanced analytics on log data
- Provides fast query performance through ClickHouse

## Implementation Steps

1. **Create ClickHouse Table**:
   ```sql
   CREATE DATABASE fluentbit;
   
   SET allow_experimental_object_type = 1;
   
   CREATE TABLE fluentbit.kube
   (
       timestamp DateTime,
       log JSON,
       host LowCardinality(String),
       pod_name LowCardinality(String)
   )
   Engine = MergeTree ORDER BY tuple(host, pod_name, timestamp);
   ```

2. **Configure Fluent Bit**:
   - Set up Kubernetes filter to enrich logs
   - Use Lua script to extract fields
   - Configure HTTP output to send logs to ClickHouse

3. **Fluent Bit Configuration Components**:
   - **Functions.lua**: Extracts fields from Kubernetes metadata
   - **Filters**: Process and enrich logs
   - **Output**: Sends logs to ClickHouse using HTTP

4. **Example Query**:
   ```sql
   SELECT
       count(),
       namespace
   FROM fluentbit.kube
   GROUP BY log.kubernetes.namespace_name AS namespace;
   ```

## Best Practices

- Use `LowCardinality(String)` for fields with limited unique values
- Consider tuning Fluent Bit flush interval based on log volume
- ClickHouse prefers batches of at least 1000 records
- Use the JSON type for structured log data

## Visualization

The logs can be visualized in Grafana by connecting to ClickHouse as a data source and creating dashboards for log analysis.
