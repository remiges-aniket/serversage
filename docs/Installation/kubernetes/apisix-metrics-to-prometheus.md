# Enable Prometheus Plugin on APISIX:
```sh
curl -i "http://127.0.0.1:9180/apisix/admin/global_rules" -X PUT -d '{
  "id": "rule-for-metrics",
  "plugins": {
    "prometheus":{}
  }
}'
```

# Configure Prometheus yaml:

```sh
echo 'scrape_configs:
  - job_name: "apisix"
    scrape_interval: 15s
    metrics_path: "/apisix/prometheus/metrics"
    static_configs:
      - targets: ["apisix-quickstart:9091"]
' > prometheus.yml

```