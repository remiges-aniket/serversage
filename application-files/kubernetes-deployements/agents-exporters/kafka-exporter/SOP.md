This SOP will guide you through deploying the Kafka Exporter on Kubernetes, allowing you to expose Kafka metrics for Prometheus consumption and subsequent visualization in ServerSage.

### Prerequisites

Before you begin, ensure you have the following:

1.  **Kubernetes Cluster**: A running Kubernetes cluster.
2.  **`kubectl`**: Command-line tool for interacting with your Kubernetes cluster, configured to connect to your cluster.
3.  **Kafka Cluster**: A running Kafka cluster within your Kubernetes environment. The Kafka Exporter needs to connect to it.
4.  **Private Docker Registry Access**: If `danielqsj/kafka-exporter:v1.9.0` is truly a private repository, ensure your Kubernetes nodes have the necessary configuration (e.g., `imagePullSecrets`) to pull images from it.
5.  **Prometheus (for metrics collection)**: A running Prometheus instance configured to scrape metrics.
6.  **ServerSage (for visualization)**: A running ServerSage instance, accessible from your network.

### Step-by-Step Deployment Guide

This SOP focuses on deploying the Kafka Exporter. You'll then need to configure Prometheus to scrape these metrics and ServerSage to visualize them.

#### 1\. Review and Modify Kafka Exporter Manifest

The provided YAML defines a Deployment and a Service for the Kafka Exporter.

  * **Namespace (`starmf-uat`)**:

      * The provided manifests use the namespace `starmf-uat`. If this namespace doesn't exist, you'll need to create it before applying the manifests. If you prefer a different namespace, ensure you update the `metadata.namespace` field in both the Deployment and the Service resources.
      * To create the namespace:
        ```bash
        kubectl create namespace starmf-uat
        ```

  * **Kafka Broker Address (`--kafka.server=kafka.serversage-app.svc.cluster.local:9092`)**:

      * The `args` section in the Deployment specifies the Kafka broker address.
      * **Crucial Change**: This value `kafka.serversage-app.svc.cluster.local:9092` assumes your Kafka cluster's service name is `kafka` and it resides in the `serversage-app` namespace.
      * You **must** update this to reflect the actual service name and namespace of your Kafka cluster. The format is `[kafka-service-name].[kafka-namespace].svc.cluster.local:[port]`.

  * **Kafka Exporter Image (`danielqsj/kafka-exporter:v1.9.0`)**:

      * The `image` field points to `danielqsj/kafka-exporter:v1.9.0`.
      * **Crucial Change**: If this is a private registry, ensure your Kubernetes cluster has `imagePullSecrets` configured in the ServiceAccount used by this Deployment (or directly in the Deployment's `spec.template.spec`) to authenticate with your private repository.
      * If you are using a public Kafka Exporter image (e.g., `danielqsj/kafka-exporter:latest` or a specific version like `danielqsj/kafka-exporter:1.6.0`), update the `image` field accordingly.
      * Consider using a specific version tag instead of `latest` for production stability.

  * **Resource Limits (Optional but Recommended)**:

      * For production environments, it's highly recommended to add `resources.limits` and `resources.requests` to the `kafka-exporter` container. This ensures proper resource allocation and prevents resource exhaustion.

      * Example for `kafka-exporter` container:

        ```yaml
              containers:
                - name: kafka-exporter
                  image: danielqsj/kafka-exporter:v1.9.0
                  imagePullPolicy: IfNotPresent
                  securityContext:
                    runAsUser: nobody
                  ports:
                    - containerPort: 9308
                  args: ["--kafka.server=kafka.serversage-app.svc.cluster.local:9092"]
                  resources:
                    limits:
                      cpu: 100m
                      memory: 128Mi
                    requests:
                      cpu: 50m
                      memory: 64Mi
        ```

#### 2\. Deploy Kafka Exporter

Save the provided YAML content into a file, e.g., `kafka-exporter.yaml`.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kafka-exporter
  namespace: starmf-uat
  labels:
    app: kafka-exporter
spec:
  replicas: 1
  revisionHistoryLimit: 5
  selector:
    matchLabels:
      app: kafka-exporter
  template:
    metadata:
      labels:
        app: kafka-exporter
    spec:
      containers:
      - name: kafka-exporter
        image: danielqsj/kafka-exporter:v1.9.0
        imagePullPolicy: IfNotPresent
        securityContext:
          runAsUser: nobody
        ports:
        - containerPort: 9308
        args: ["--kafka.server=kafka.serversage-app.svc.cluster.local:9092"]
---
apiVersion: v1
kind: Service
metadata:
  name: kafka-exporter
  namespace: starmf-uat
spec:
  selector:
    app: kafka-exporter
  ports:
    - protocol: TCP
      port: 9308
      targetPort: 9308
  type: ClusterIP
```

Now, apply the manifest to your Kubernetes cluster:

```bash
kubectl apply -f kafka-exporter.yaml
```

**Verification:**
Check if the Kafka Exporter pod is running and the Service is created:

```bash
kubectl -n starmf-uat get pods -l app=kafka-exporter
kubectl -n starmf-uat get svc kafka-exporter
```

You should see the pod in a `Running` state and the Service with a `ClusterIP`.

#### 3\. Configure Prometheus to Scrape Kafka Exporter Metrics

Prometheus needs to be configured to discover and scrape metrics from the Kafka Exporter service.

  * **If you are using Prometheus Operator**:
    You would typically create a `ServiceMonitor` resource.

    ```yaml
    apiVersion: monitoring.coreos.com/v1
    kind: ServiceMonitor
    metadata:
      name: kafka-exporter
      namespace: starmf-uat # The namespace where Kafka Exporter is deployed
      labels:
        release: prometheus-stack # Label to match with your Prometheus instance's selector
    spec:
      selector:
        matchLabels:
          app: kafka-exporter # Matches the labels of the Kafka Exporter Service
      endpoints:
      - port: 9308 # The port exposed by the Kafka Exporter Service
        interval: 30s # How often to scrape metrics
      namespaceSelector:
        matchNames:
        - starmf-uat # Ensures Prometheus only looks in this namespace
    ```

    Apply this `ServiceMonitor` to the namespace where your Prometheus Operator is watching (often the same namespace as Prometheus).

  * **If you are using a custom Prometheus setup (e.g., a Prometheus Deployment with a ConfigMap)**:
    You need to add a new `scrape_config` entry to your Prometheus configuration file (`prometheus.yml`):

    ```yaml
    # ... existing scrape_configs ...
    - job_name: 'kafka-exporter'
      kubernetes_sd_configs:
      - role: service
        namespaces:
          names: ['starmf-uat'] # The namespace where Kafka Exporter is deployed
      relabel_configs:
      - source_labels: [__meta_kubernetes_service_name]
        action: keep
        regex: kafka-exporter
      - source_labels: [__meta_kubernetes_service_port]
        action: replace
        target_label: __metrics_path__
        regex: (.*)
        replacement: /metrics
      - source_labels: [__address__]
        regex: (.+):9308
        replacement: ${1}:9308
        target_label: __address__
    ```

    After modifying the Prometheus configuration, you'll need to restart or reload your Prometheus instance for the changes to take effect.

**Verification:**
Access your Prometheus UI (typically at `http://prometheus-service-ip:9090` or via port-forwarding) and navigate to "Status" -\> "Targets". You should see `kafka-exporter` listed as a healthy target.

#### 4\. Configure ServerSage for Kafka Metrics Visualization

1.  **Access ServerSage**: Log in to your ServerSage instance.
2.  **Add Data Source**:
      * Navigate to **Configuration** (gear icon on the left) -\> **Data Sources**.
      * Click **Add data source**.
      * Select **Prometheus**.
3.  **Prometheus Data Source Configuration**:
      * **Name**: Give it a descriptive name (e.g., `Prometheus`).
      * **URL**: Enter the URL for your Prometheus service. If ServerSage is in the same Kubernetes cluster, use the internal service name: `http://prometheus-operated.prometheus.svc.cluster.local:9090` (adjust service name and namespace based on your Prometheus deployment). If ServerSage is external, use the external IP/hostname of your Prometheus service.
      * Click **Save & Test**. You should see "Data source is working" if configured correctly.
4.  **Import a Kafka Exporter Dashboard**:
      * ServerSage has a rich community of shared dashboards. You can import pre-built dashboards for Kafka Exporter.
      * Navigate to **Dashboards** (four squares icon on the left) -\> **Import**.
      * You can find Kafka Exporter dashboards on the ServerSage Dashboards website (e.g., search for "Kafka Exporter" or ID `758`, `721`, `10403`).
      * Enter the Dashboard ID or upload the JSON file.
      * Select your Prometheus data source when prompted.
      * Click **Import**.
      * This will create a pre-configured dashboard displaying various Kafka metrics (consumer lag, broker stats, topic metrics, etc.).
5.  **Create Custom Dashboards (Explore Metrics)**:
      * Navigate to **Explore** (compass icon on the left).
      * Select your Prometheus data source.
      * You can query for Kafka Exporter metrics, which typically start with `kafka_`. Examples include:
          * `kafka_consumergroup_lag`
          * `kafka_broker_info`
          * `kafka_topic_partitions`
      * Use these metrics to build custom panels and dashboards tailored to your monitoring needs.

### Troubleshooting

  * **Kafka Exporter Pod Not Running**:
      * Check `kubectl -n starmf-uat describe pod <pod-name>` for events and error messages, especially concerning `ImagePullBackOff` if using a private registry without proper `imagePullSecrets`.
      * Check `kubectl -n starmf-uat logs <pod-name>` for Kafka Exporter internal logs. Ensure it can connect to your Kafka broker.
  * **Prometheus Not Scrapping Metrics**:
      * Verify the `ServiceMonitor` (if using Prometheus Operator) or `scrape_config` in Prometheus's configuration is correct, especially the `selector` and `namespace` settings.
      * Check Prometheus's "Targets" page in its UI for errors related to the `kafka-exporter` job.
      * Ensure network connectivity between Prometheus and the Kafka Exporter service.
  * **No Metrics in ServerSage**:
      * Confirm your Prometheus data source in ServerSage is correctly configured and "Save & Test" is successful.
      * Verify that Prometheus is successfully scraping metrics from Kafka Exporter.
      * Ensure your dashboard queries are correct and the time range is appropriate.

By following these steps, you will have a deployed Kafka Exporter, sending Kafka metrics to Prometheus, which can then be visualized in ServerSage, providing valuable insights into your Kafka cluster's health and performance.