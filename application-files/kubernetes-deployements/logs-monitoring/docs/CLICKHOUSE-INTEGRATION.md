# ClickHouse Integration for Kubernetes Logs

This guide explains how to use ClickHouse for storing and analyzing Kubernetes logs collected by Fluent Bit.

## Architecture

The log aggregation system consists of the following components:

1. **Fluent Bit**: Collects logs from Kubernetes pods, enriches them with metadata, and sends them to ClickHouse
2. **ClickHouse**: Stores and indexes logs for fast querying
3. **Grafana**: Visualizes logs and provides dashboards for analysis

## ClickHouse Schema

The logs are stored in a table with the following schema:

```sql
CREATE TABLE fluentbit.kube
(
    timestamp DateTime,
    log JSON,
    host LowCardinality(String),
    pod_name LowCardinality(String)
)
Engine = MergeTree ORDER BY tuple(host, pod_name, timestamp)
```

Key features of this schema:
- `timestamp`: DateTime for when the log was generated
- `log`: JSON field containing the full log message and Kubernetes metadata
- `host`: The node where the log was generated (using LowCardinality for efficiency)
- `pod_name`: The name of the pod that generated the log (using LowCardinality for efficiency)

## Fluent Bit Configuration

Fluent Bit is configured to:

1. Collect logs from Kubernetes pods
2. Enrich logs with Kubernetes metadata
3. Process logs using Lua script to extract host and pod_name
4. Send logs to ClickHouse using the HTTP output plugin

Key configuration sections:

### Kubernetes Filter

```
[FILTER]
    Name                kubernetes
    Match               kube.*
    Kube_URL            https://kubernetes.default.svc:443
    Kube_CA_File        /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
    Kube_Token_File     /var/run/secrets/kubernetes.io/serviceaccount/token
    Kube_Tag_Prefix     kube.var.log.containers.
    Merge_Log           On
    Keep_Log            Off
    K8S-Logging.Parser  On
    K8S-Logging.Exclude Off
```

### Lua Processing

```
[FILTER]
    Name    lua
    Match   *
    script  /fluent-bit/etc/functions.lua
    call    set_fields
```

### ClickHouse Output

```
[OUTPUT]
    Name        http
    Match       *
    Host        clickhouse
    Port        8123
    URI         /?query=INSERT+INTO+fluentbit.kube+FORMAT+JSONEachRow
    Format      json_stream
    Json_Date_Key timestamp
    Json_Date_Format epoch
    HTTP_User   default
    HTTP_Passwd clickhouse
    tls         off
    tls.verify  off
```

## Grafana Dashboard

The Grafana dashboard provides:

1. Log volume by namespace
2. Log volume by pod
3. Error logs over time
4. Top containers by log volume
5. Recent logs with full details

## Example Queries

### Count logs by namespace

```sql
SELECT
    count(),
    namespace
FROM fluentbit.kube
GROUP BY log.kubernetes.namespace_name AS namespace
```

### Find error logs

```sql
SELECT
    timestamp,
    pod_name,
    log.log
FROM fluentbit.kube
WHERE positionCaseInsensitive(log.log, 'error') > 0
ORDER BY timestamp DESC
LIMIT 100
```

### Logs from specific pod

```sql
SELECT
    timestamp,
    log.log
FROM fluentbit.kube
WHERE pod_name = 'my-pod-name'
ORDER BY timestamp DESC
LIMIT 100
```

## Performance Considerations

1. **Batch Size**: ClickHouse prefers batches of at least 1000 records. Fluent Bit is configured to flush every 5 seconds, which should be adjusted based on log volume.

2. **LowCardinality**: Used for fields with limited unique values to improve query performance.

3. **Partitioning**: For large volumes of logs, consider partitioning the table by date:

```sql
CREATE TABLE fluentbit.kube
(
    timestamp DateTime,
    log JSON,
    host LowCardinality(String),
    pod_name LowCardinality(String)
)
Engine = MergeTree
PARTITION BY toYYYYMMDD(timestamp)
ORDER BY tuple(host, pod_name, timestamp)
```

## Troubleshooting

### No logs in ClickHouse

1. Check Fluent Bit logs:
   ```bash
   kubectl -n logging logs -l app=fluent-bit
   ```

2. Verify ClickHouse is accessible from Fluent Bit:
   ```bash
   kubectl -n logging exec -it $(kubectl -n logging get pods -l app=fluent-bit -o jsonpath='{.items[0].metadata.name}') -- curl -v http://clickhouse:8123/ping
   ```

3. Check ClickHouse logs:
   ```bash
   kubectl -n logging logs -l app=clickhouse
   ```

### Query performance issues

1. Review the table schema and consider adding indices for common query patterns
2. Use the `EXPLAIN` command to analyze query execution plans
3. Consider materialized views for common aggregations
