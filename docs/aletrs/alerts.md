alerts used in serversage:

1 . High CPU Utilization
Prom query : 100 * (1 - avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[30s])))
Threshold : 60 < X
Summary : High CPU usage detected on instance {{ $labels.instance }}
Description : The CPU utilisation of instance {{ $labels.instance }} has been above 60 %  for the last 1 minutes, current value is  {{ $values.A }} %.

2. High System Load
Prom query : node_load5
Threshold : 60 < X
Summary : The System Load of instance  has been above 60 for the last 5 minutes in Demo environment. Current Value: {{ $values.A }}
Description : System Load has exceeded 10 for the last 5 minutes.

3. HighDiskUsage
Prom query : (max(100 - ((node_filesystem_avail_bytes * 100) / node_filesystem_size_bytes)) by (instance))
Threshold : 90 < X
Summary : High Disk Usage on {{ $labels.instance }}.  Critical: High Disk Usage on {{ $labels.instance }}
Description : Disk usage on {{ $labels.instance }} is {{ $value.h }}%. This is above the threshold of 90%.

4. High Memory Usage
Prom query : (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes)/ (node_memory_MemTotal_bytes ) * 100
Threshold : 85 < X
Summary : Suggested Actions:
            1. Check the processes consuming the most memory using tools like `top` or `htop`.
            2. Review application logs for memory leaks or excessive memory allocation.
            3. Consider restarting the application or service if necessary.
            4. If the problem persists, evaluate the need for additional memory resources.
            Troubleshooting:
            *   Connect to the instance: `ssh {{ $labels.instance }}`
            *   Check memory usage: `free -m` or `htop`
            Please take immediate action to resolve this issue.
            Instance: {{ $labels.instance }}
Description : The memory usage on this node has exceeded the defined threshold above 85%. Please investigate immediately. on: {{ $labels.instance }}

5. PostgreSQL Database Down
Prom query : pg_up
Threshold : 0.5 > X
Summary : Database is down for instance {{ $labels.instance }}
Description : Instance: {{ $labels.instance }}, job: {{ $labels.job }}, Timestamp: {{ $time }},
              Details: The PostgreSQL instance is currently not reachable. The `pg_up` metric has reported a value of 0, indicating that the database is down.

6. Node Down
Prom query : group by(instance, job) (up)
Threshold : 0.5 > X
Summary : Triggered when the node is unreachable (up{} == 0 for 1 minutes)
Description : Triggered when the node is unreachable (up{} == 0 for 1 minutes)

7. System Reboot Detected
Prom query :     (time() - node_boot_time_seconds)
Threshold : 300 > X
Summary : 🔥 ALERT! System Rebooted: {{ $labels.instance }}, Machine {{ $labels.instance }} has recently rebooted.
            Current uptime: {{ printf "%.0f" (time() - $values.A.Value) }} seconds
Description : Triggered when the node is unreachable (up{} == 0 for 1 minutes)

8. pod reboot detected
Prom query : sum by (namespace, pod, container) (increase(kube_pod_container_status_restarts_total[1m]))
Threshold : 0 < X
Summary : Pod {{ $labels.namespace }}/{{ $labels.pod }} container(s) restarted
Description : Container(s) in pod {{ $labels.pod }} in namespace {{ $labels.namespace }} have restarted recently. Current restart count: {{ $value }}

9. kafka lag
Prom query : max by (consumergroup, topic) (kafka_consumergroup_lag) > 1000
Threshold : 1000 < X
Summary : High Kafka consumer lag detected for group '{{ $labels.consumergroup }}' on topic '{{ $labels.topic }}'
Description : The Kafka consumer group '{{ $labels.group }}' is experiencing high lag on topic '{{ $labels.topic }}' (partition {{ $labels.partition }}). Current lag is {{ $values.A }}. This indicates messages are not being processed quickly enough.




