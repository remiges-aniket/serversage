

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: "0.0.0.0:4317"
      http:
        endpoint: "0.0.0.0:4318"

  prometheus/keycloak:
    config:
      scrape_configs:
        - job_name: 'app1'
          scrape_interval: 5s
          metrics_path: '/metrics'
          static_configs:
            - targets: ['app1:8080'] # Keycloak service endpoint

        - job_name: 'app2'
          scrape_interval: 5s
          metrics_path: '/realms/master/metrics' # Realm-specific metrics
          static_configs:
            - targets: ['app2:8080']

processors:
  # Resource processors to tag applications
  resource/app1:
    attributes:
      - key: app
        value: app1
        action: upsert
  resource/app2:
    attributes:
      - key: app
        value: app2
        action: upsert

  # Filter processors for app1 and app2
  filter/app1:
    metrics:
      include:
        match_type: strict
        resource_attributes:
          - key: app
            value: app1
    traces:
      include:
        match_type: strict
        resource_attributes:
          - key: app
            value: app1
    logs:
      include:
        match_type: strict
        resource_attributes:
          - key: app
            value: app1

  filter/app2:
    metrics:
      include:
        match_type: strict
        resource_attributes:
          - key: app
            value: app2
    traces:
      include:
        match_type: strict
        resource_attributes:
          - key: app
            value: app2
    logs:
      include:
        match_type: strict
        resource_attributes:
          - key: app
            value: app2

exporters:
  # App1 exporters
  prometheus/app1:
    endpoint: "app1-prometheus:9090"
  tempo/app1:
    endpoint: "app1-tempo:3100"
  loki/app1:
    endpoint: "app1-loki:3100"

  # App2 exporters
  prometheus/app2:
    endpoint: "app2-prometheus:9090"
  tempo/app2:
    endpoint: "app2-tempo:3100"
  loki/app2:
    endpoint: "app2-loki:3100"

service:
  pipelines:
    metrics/app1:
      receivers: [otlp]
      processors: [resource/app1, filter/app1]
      exporters: [prometheus/app1]
    metrics/app2:
      receivers: [otlp]
      processors: [resource/app2, filter/app2]
      exporters: [prometheus/app2]
    traces/app1:
      receivers: [otlp]
      processors: [resource/app1, filter/app1]
      exporters: [tempo/app1]
    traces/app2:
      receivers: [otlp]
      processors: [resource/app2, filter/app2]
      exporters: [tempo/app2]
    logs/app1:
      receivers: [otlp]
      processors: [resource/app1, filter/app1]
      exporters: [loki/app1]
    logs/app2:
      receivers: [otlp]
      processors: [resource/app2, filter/app2]
      exporters: [loki/app2]
```