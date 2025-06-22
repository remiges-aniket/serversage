To deploy a centralized logging solution using Fluent Bit, Kafka, and Elasticsearch with visualization in ServerSage on Kubernetes, follow these steps. This guide assumes a basic understanding of Kubernetes concepts.

### Prerequisites

Before you begin, ensure you have the following:

1.  **Kubernetes Cluster**: A running Kubernetes cluster (e.g., MiniKube, Kubeadm, GKE, EKS, AKS).
2.  **`kubectl`**: Command-line tool for interacting with your Kubernetes cluster, configured to connect to your cluster.
3.  **Elasticsearch, Kafka, ServerSage**: Running instances of this services needed, accessible from your network.

Assuming you have above all instances ready, hence assuming url's for them as follows:
kafka : "kafka.kafka.svc.cluster.local:9092"
elasticsearch : "elasticsearch.elasticsearch.svc.cluster.loca"


### Step-by-Step Deployment Guide

This SOP will guide you through deploying Fluent Bit as a log shipper, a Fluent Bit consumer for Kafka, and configuring it to send logs to Elasticsearch for visualization in ServerSage.


#### 1\. Prepare Fluent Bit Manifests

The provided YAML files (`fluent-bit-dep.yaml` and `fluent-bit-kafka-consumer.yaml`) are used for this deployment.

**Review and Modify:**

  * **Namespace (`serversage-app`)**:

      * The provided manifests use the namespace `serversage-app`. You can change this if you prefer a different namespace. If you change it, ensure you update the `metadata.namespace` field in all resources (ConfigMap, DaemonSet, ServiceAccount, ClusterRoleBinding's `subjects` section for the ServiceAccount).
      * Create the namespace if it doesn't exist:
        ```bash
        kubectl create namespace serversage-app
        ```

  * **Fluent Bit Image**:

      * The `fluent-bit` DaemonSet uses `cr.fluentbit.io/fluent/fluent-bit:4.0`. This is a stable version. You can update it to a newer version if necessary.

  * **Kafka Broker Address**:

      * In `fluent-bit-dep.yaml` (Fluent Bit producer) and `fluent-bit-kafka-consumer.yaml` (Fluent Bit consumer), the Kafka broker address is `kafka.kafka.svc.cluster.local:9092`.
      * **Crucial Change**: If your Kafka cluster is in a different namespace or has a different service name, you **must** update this value. The format is `[kafka-service-name].[kafka-namespace].svc.cluster.local:[port]`.

  * **Elasticsearch Host and Port**:

      * In `fluent-bit-kafka-consumer.yaml`, the Elasticsearch host is `elasticsearch.elasticsearch.svc.cluster.local` and the port is `9200`.
      * **Crucial Change**: If your Elasticsearch cluster is in a different namespace or has a different service name, you **must** update this value. The format is `[elasticsearch-service-name].[elasticsearch-namespace].svc.cluster.local:[port]`.

  * **Log Exclusion (Fluent Bit Producer - `fluent-bit-dep.yaml`)**:

      * The `Exclude_Path` in the `[INPUT]` section for `tail` specifies paths to exclude from logging. Currently, it excludes:
          * `/var/log/containers/fluent-bit*` (to prevent Fluent Bit from logging its own logs)
          * Various `staging-*` backend logs.
      * **Optional Change**: You can modify this list to include or exclude other application logs based on your needs.

  * **Resource Limits (Optional but Recommended)**:

      * For production environments, it's highly recommended to add `resources.limits` and `resources.requests` to the `fluent-bit` containers in both the DaemonSet and the Deployment. This ensures proper resource allocation and prevents resource exhaustion.

      * Example for `fluent-bit` container in `fluent-bit-dep.yaml` (DaemonSet):

        ```yaml
              containers:
                - name: fluent-bit
                  image: cr.fluentbit.io/fluent/fluent-bit:4.0
                  resources:
                    limits:
                      cpu: 200m
                      memory: 200Mi
                    requests:
                      cpu: 100m
                      memory: 100Mi
        ```

      * Apply similar resource limits to the `fluent-bit-kafka-consumer` deployment.

  * **Elasticsearch Index Name (`kubernetes-logs`)**:

      * In `fluent-bit-kafka-consumer.yaml`, the `[OUTPUT]` section for `es` specifies `Index kubernetes-logs`.
      * **Optional Change**: You can change this index name. This is the index that will be created in Elasticsearch to store your logs.

#### 4\. Deploy Fluent Bit (Log Shipper)

This deploys Fluent Bit as a DaemonSet, ensuring it runs on every node to collect container logs.

```bash
kubectl apply -f fluent-bit-dep.yaml
```

**Verification:**
Check if the Fluent Bit pods are running:

```bash
kubectl -n serversage-app get pods -l k8s-app=fluent-bit
```

You should see pods in a `Running` state, one for each node in your cluster (unless tainted nodes are present that Fluent Bit is not tolerating).

#### 5\. Deploy Fluent Bit Kafka Consumer

This deploys a Fluent Bit instance configured to consume messages from Kafka and send them to Elasticsearch.

```bash
kubectl apply -f fluent-bit-kafka-consumer.yaml
```

**Verification:**
Check if the Fluent Bit Kafka Consumer pod is running:

```bash
kubectl -n serversage-app get pods -l app=fluent-bit-kafka-consumer
```

You should see a pod in a `Running` state.

#### 6\. Verify Log Ingestion in Elasticsearch

Once the Fluent Bit Kafka Consumer is running, logs should start flowing into your Elasticsearch cluster.

You can verify this by querying Elasticsearch. If you have `curl` and access to your Elasticsearch cluster, you can try:

```bash
# Forward a port to your Elasticsearch service (example)
kubectl port-forward service/elasticsearch -n elasticsearch 9200:9200 &

# Then, from your local machine, query Elasticsearch indices
curl "localhost:9200/_cat/indices?v"
```

You should see an index named `kubernetes-logs` (or whatever you configured) with a non-zero document count.

#### 7\. Configure ServerSage for Log Visualization

1.  **Access ServerSage**: Log in to your ServerSage instance.
2.  **Add Data Source**:
      * Navigate to **Configuration** (gear icon on the left) -\> **Data Sources**.
      * Click **Add data source**.
      * Select **Elasticsearch**.
3.  **Elasticsearch Data Source Configuration**:
      * **Name**: Give it a descriptive name (e.g., `Kubernetes Logs`).
      * **URLs**: Enter the URL for your Elasticsearch service. If ServerSage is in the same Kubernetes cluster, you can use the internal service name: `http://elasticsearch.elasticsearch.svc.cluster.local:9200` (adjust namespace and service name if yours are different). If ServerSage is external, use the external IP/hostname of your Elasticsearch service.
      * **Auth**: If your Elasticsearch cluster requires authentication, configure it here.
      * **Index name**: `kubernetes-logs` (or your chosen index name).
      * **Time field**: `@timestamp` (this is the field Fluent Bit uses for timestamps).
      * **Version**: Select the version of your Elasticsearch cluster.
      * **Min time interval**: `10s` (or adjust as needed).
      * Click **Save & Test**. You should see "Data source is working" if configured correctly.
4.  **Create a Dashboard (Explore Logs)**:
      * Navigate to **Explore** (compass icon on the left).
      * Select your newly created Elasticsearch data source.
      * In the query editor, you can start by simply selecting "Logs" or writing Lucene queries (e.g., `kubernetes.namespace_name:serversage-app`).
      * You can then create dashboards with various panels to visualize your logs, such as:
          * **Logs panel**: To view raw log messages.
          * **Graph panel**: To see log volume over time.
          * **Table panel**: To display structured log data.

### Troubleshooting

  * **Fluent Bit Pods Not Running**:
      * Check `kubectl -n serversage-app describe pod <pod-name>` for events and error messages.
      * Check `kubectl -n serversage-app logs <pod-name>` for Fluent Bit internal logs.
  * **No Logs in Kafka**:
      * Verify the `fluent-bit-dep` pods are running and their logs.
      * Ensure the Kafka broker address in `fluent-bit.conf` is correct.
      * Check Kafka broker logs for connection issues.
  * **No Logs in Elasticsearch**:
      * Verify the `fluent-bit-kafka-consumer` pod is running and its logs.
      * Ensure the Kafka broker and Elasticsearch host/port in `fluent-bit-kafka-consumer-config` are correct.
      * Check Elasticsearch cluster health and logs.
  * **ServerSage Data Source Issues**:
      * Double-check the Elasticsearch URL, index name, and time field in the ServerSage data source configuration.
      * Ensure network connectivity between ServerSage and Elasticsearch.

This SOP provides a comprehensive guide for deploying a centralized logging solution using the provided Fluent Bit configurations within a Kubernetes environment. Remember to adapt the configurations to your specific cluster setup and requirements.