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

## Permission issue in Thanos-storage:

go to that worker using docker exec and give dir right to nobody user : (65534 : nobody)

```sh
sudo chown -R 65534:65534 /bse-data/thanos-store/thanos-store-gateway-0

```

## Running approach:
1. run 1.namespace : keep "monitoring:proemetheus" as it is
2. run 2.init
3. run 3.prometheus-op.yaml
4. done..