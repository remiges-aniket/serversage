# Configuration Guide

This guide explains how to configure the different components of the log aggregation system.

## Fluent Bit Configuration

Fluent Bit is configured using a ConfigMap that contains two main files:
- `fluent-bit.conf`: The main configuration file
- `parsers.conf`: Contains parser definitions for different log formats

### Main Configuration Sections

The main configuration file has several sections:

1. **SERVICE**: General settings for the Fluent Bit service
2. **INPUT**: Defines where and how logs are collected
3. **FILTER**: Processes and enriches logs
4. **OUTPUT**: Defines where logs are sent

### Customizing Log Collection

To modify which logs are collected, edit the INPUT section in `manifests/fluent-bit/fluent-bit-config.yaml`:

```
[INPUT]
    Name              tail
    Tag               kube.*
    Path              /var/log/containers/*.log
    Parser            docker
    DB                /var/log/flb_kube.db
    Mem_Buf_Limit     5MB
    Skip_Long_Lines   On
    Refresh_Interval  10
```

### Adding Custom Parsers

To add a custom parser for specific log formats, add it to the `parsers.conf` section:

```
[PARSER]
    Name   my-custom-format
    Format regex
    Regex  ^(?<time>[^ ]+) (?<level>[^ ]+) (?<message>.+)$
    Time_Key time
    Time_Format %Y-%m-%dT%H:%M:%S.%L
```

## Loki Configuration

Loki is configured through the `loki.yaml` file in the ConfigMap.

### Storage Configuration

By default, Loki uses an in-memory storage with filesystem backup. For production, consider configuring object storage:

```yaml
storage_config:
  aws:
    s3: s3://access_key:secret_key@region/bucket_name
    s3forcepathstyle: true
```

### Retention Configuration

To configure log retention, modify the `table_manager` section:

```yaml
table_manager:
  retention_deletes_enabled: true
  retention_period: 168h  # 7 days
```

## Grafana Configuration

### Adding Data Sources

Data sources are configured in `manifests/grafana/grafana-config.yaml`. To add a new data source:

```yaml
datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    version: 1
    editable: true
    isDefault: true
  - name: MyNewDataSource
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    version: 1
    editable: true
```

### Adding Dashboards

Dashboards are stored in the `grafana-dashboards` ConfigMap. To add a new dashboard:

1. Export your dashboard from Grafana as JSON
2. Add it to the ConfigMap in `manifests/grafana/grafana-config.yaml`

## Applying Configuration Changes

After modifying any configuration files, apply the changes:

```bash
kubectl apply -f manifests/fluent-bit/fluent-bit-config.yaml
kubectl apply -f manifests/loki/loki-deployment.yaml
kubectl apply -f manifests/grafana/grafana-config.yaml
```

Some components may need to be restarted to pick up the changes:

```bash
kubectl -n logging rollout restart deployment/loki
kubectl -n logging rollout restart daemonset/fluent-bit
kubectl -n logging rollout restart deployment/grafana
```
