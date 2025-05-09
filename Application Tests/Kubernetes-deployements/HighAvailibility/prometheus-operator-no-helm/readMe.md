kubectl run -n monitoring --rm -i --tty minio-client --image=minio/mc --restart=Never --command -- /bin/sh -c "mc alias set minio http://minio:9000 minioadmin minioadmin && mc mb minio/thanos"



Check sidecar logs:

kubectl logs -n monitoring prometheus-0 -c thanos-sidecar


Verify bucket creation:

kubectl exec -n monitoring minio-0 -- ls -l /data/thanos/

## Monitoring Stack Flow Diagram:
```text
+--------------------+     +---------------------+      +------------------+
|                    |     |                     |      |                  |
|  Node Exporter     |     | Kube State Metrics  |      |   Prometheus     |
| (on every node)    +---->+ (K8s resource stats)+----->+  (2 replicas, HA)|
|                    |     |                     |      |                  |
+--------------------+     +---------------------+      +------------------+
                                                           |   ^       |
                                                           |   |       |
                                                           v   |       v
                                                     +----------------------+
                                                     |  Thanos Sidecar      |
                                                     +----------------------+
                                                           |   |
                                                           v   |
                                                     +----------------------+
                                                     |      MinIO           |
                                                     | (Object Storage)     |
                                                     +----------------------+
                                                           ^
                                                           |
                                                     +----------------------+
                                                     | Thanos Store Gateway |
                                                     +----------------------+
                                                           ^
                                                           |
                                                     +----------------------+
                                                     |   Thanos Querier     |
                                                     +----------------------+
                                                           ^
                                                           |
                                                     +----------------------+
                                                     |      Grafana         |
                                                     +----------------------+

```