## This dashboard includes panels for the following metrics:

* Commands Per Second
* Commands Latency Per Second
* Hit Ratio Per Instance
* Total Memory Usage
* Memory Fragmantation Ratio Per Instance
* Key Evictions Per Second Per Instance
* Connected/Blocked Clients
* Total Items Per DB
* Expiring vs Non-Expiring Keys
* Connected Slaves Per Instance
* Time Since Last Master Connection

You can import the dashboard directly using the following dashboard ID: 763



## This quickstart includes the following alerting rules:

* RedisDown

Redis instance is down

* RedisOutOfMemory

Redis is running out of memory

* RedisTooManyConnections

Redis has more than X connected clients


```yaml
groups:
- name: redis
  rules:
  - alert: RedisDown
    annotations:
      description: |-
        Redis instance is down
          VALUE = {{ $value }}
          LABELS: {{ $labels }}
      summary: Redis down (instance {{ $labels.instance }})
    expr: redis_up == 0
    for: 5m
    labels:
      severity: critical
  - alert: RedisOutOfMemory
    annotations:
      description: |-
        Redis is running out of memory (> 90%)
          VALUE = {{ $value }}
          LABELS: {{ $labels }}
      summary: Redis out of memory (instance {{ $labels.instance }})
    expr: redis_memory_used_bytes / redis_total_system_memory_bytes * 100 > 90
    for: 5m
    labels:
      severity: warning
  - alert: RedisTooManyConnections
    annotations:
      description: |-
        Redis instance has too many connections
          VALUE = {{ $value }}
          LABELS: {{ $labels }}
      summary: Redis too many connections (instance {{ $labels.instance }})
    expr: redis_connected_clients > 100
    for: 5m
    labels:
      severity: warning


```




Reference: [Here](https://grafana.com/oss/prometheus/exporters/redis-exporter/?tab=dashboards)

