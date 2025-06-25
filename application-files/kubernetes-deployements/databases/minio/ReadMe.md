
-----

## Standard Operating Procedure: MinIO Metrics on Kubernetes with Prometheus

This SOP guides you through enabling Prometheus metrics on your MinIO instance running in Kubernetes and configuring Prometheus to scrape these metrics. This setup provides valuable insights into your MinIO cluster's performance and health.

### 1\. Prerequisites

Before you begin, ensure the following are in place:

  * **Kubernetes Cluster:** A functional Kubernetes cluster (version 1.16+ recommended).
  * **`kubectl`:** Configured to interact with your Kubernetes cluster.
  * **MinIO Instance on Kubernetes:** A running MinIO deployment (Deployment or StatefulSet) and its corresponding Kubernetes Service.
      * **MinIO Service URL:** You should know the internal cluster DNS name and port for your MinIO service, typically in the format `minio-service.database.svc.cluster.local:<minio-api-port>`.
      * **MinIO API Port:** Identify the port on which your MinIO server's API is exposed (commonly `9000` or `9001`). This is the same port MinIO uses for Prometheus metrics.
  * **Prometheus Instance on Kubernetes:** A Prometheus deployment running within the same or an accessible Kubernetes cluster, configured to scrape targets.

### 2\. Update MinIO Kubernetes Deployment to Expose Metrics

MinIO natively exposes Prometheus-compatible metrics. For newer MinIO versions, it's crucial to explicitly allow unauthenticated access for Prometheus scraping for simpler integration.

1.  **Locate your MinIO Deployment/StatefulSet Manifest:**
    Find the YAML file that defines your MinIO Kubernetes deployment (e.g., `minio-deployment.yaml` or `minio-statefulset.yaml`).

2.  **Add `MINIO_PROMETHEUS_AUTH_TYPE` Environment Variable:**
    Edit the MinIO container specification within your Deployment/StatefulSet to include the `MINIO_PROMETHEUS_AUTH_TYPE` environment variable set to `"public"`. This disables JWT token authentication for the Prometheus metrics endpoint.

    **Example `Deployment` or `StatefulSet` snippet:**

    ```yaml
    apiVersion: apps/v1
    kind: Deployment # Or StatefulSet
    metadata:
      name: minio
      namespace: database # Replace with your MinIO's namespace
      labels:
        app: minio
    spec:
      replicas: 1 # Adjust as per your MinIO setup
      selector:
        matchLabels:
          app: minio
      template:
        metadata:
          labels:
            app: minio
        spec:
          containers:
          - name: minio
            image: quay.io/minio/minio:RELEASE.2024-06-05T09-24-34Z # IMPORTANT: Use a specific, stable version in production!
            ports:
            - name: api-port # Name your MinIO API port for clarity
              containerPort: 9000 # Your MinIO API port (e.g., 9000 or 9001)
            args: ["server", "/data", "--console-address", ":9001"] # Adjust args as per your MinIO setup
            env:
              # --- ESSENTIAL FOR PROMETHEUS METRICS ---
              # Allows unauthenticated access to the Prometheus metrics endpoint.
              # Default is "jwt", requiring a bearer token. Set to "public" for simpler scraping.
            - name: MINIO_PROMETHEUS_AUTH_TYPE
              value: "public"
              # --- END ESSENTIAL FOR PROMETHEUS METRICS ---

              # Existing MinIO credentials (example, adjust according to your setup)
            - name: MINIO_ROOT_USER
              valueFrom:
                secretKeyRef:
                  name: minio-creds # Replace with your secret name
                  key: rootUser
            - name: MINIO_ROOT_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: minio-creds # Replace with your secret name
                  key: rootPassword
            # ... other MinIO specific configurations (volumes, probes, etc.)
    ```

3.  **Update MinIO Kubernetes Service (if necessary):**
    Ensure your MinIO Kubernetes Service explicitly defines a `name` for the port that exposes the MinIO API (e.g., `name: api-port`). This named port will be used by Prometheus's `ServiceMonitor` for discovery.

    **Example `Service` snippet:**

    ```yaml
    apiVersion: v1
    kind: Service
    metadata:
      name: minio-service # Your MinIO Service name
      namespace: database # Your MinIO's namespace
      labels:
        app: minio
    spec:
      selector:
        app: minio # Must match the labels of your MinIO pods
      ports:
        - protocol: TCP
          port: 9000 # The service port
          targetPort: 9000 # The container port (must match `containerPort` in Deployment)
          name: api-port # IMPORTANT: A named port for ServiceMonitor to reference
      type: ClusterIP # Typically ClusterIP for internal cluster access
    ```

4.  **Apply Changes:**
    Apply the updated MinIO deployment and service manifests:

    ```bash
    kubectl apply -f <your-minio-deployment.yaml>
    kubectl apply -f <your-minio-service.yaml> # Only if you changed the Service
    ```

    This will cause your MinIO pods to restart with the new configuration.

### 3\. Configure Prometheus to Scrape MinIO Metrics

MinIO exposes its metrics at the path `/minio/v2/metrics/cluster` on its API port. You can configure Prometheus using either a standard `scrape_config` (for direct Prometheus `ConfigMap` configuration) or a `ServiceMonitor` resource (if using Prometheus Operator).

#### Option A: Standard Prometheus `scrape_config`

If you are managing your Prometheus configuration directly via a `ConfigMap`, add the following job to your `prometheus.yml` under the `scrape_configs:` section:

```yaml
# Add this section to your prometheus.yml
- job_name: 'minio'
  metrics_path: '/minio/v2/metrics/cluster'
  scheme: http # Use http or https based on your MinIO setup
  static_configs:
    - targets: ['minio-service.database.svc.cluster.local:<minio-api-port>'] # Replace with your MinIO Service DNS name and port
```

**Replace:**

  * `<minio-api-port>`: The actual API port of your MinIO service (e.g., `9000`).
  * `http`: Change to `https` if your MinIO instance uses TLS.
  * `minio-service.database.svc.cluster.local`: Adjust the service name and namespace if different.

After updating `prometheus.yml`, you'll need to restart or trigger a reload of your Prometheus instance for the changes to take effect.

#### Option B: Prometheus Operator `ServiceMonitor` (Recommended for Kubernetes)

If you are using the Prometheus Operator, create a `ServiceMonitor` resource:

**`minio-servicemonitor.yaml`**

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: minio-servicemonitor
  namespace: database # Namespace where your MinIO Service is running
  labels:
    app: minio # A label that matches your MinIO Service (for selector)
    release: prometheus-stack # IMPORTANT: This label must match your Prometheus Operator's serviceMonitorSelector
spec:
  selector:
    matchLabels:
      app: minio # This must match the labels on your MinIO Kubernetes Service
  namespaceSelector:
    matchNames:
      - database # This should match the namespace of your MinIO Service
  endpoints:
  - port: api-port # This must match the `name` of the port in your MinIO Service (e.g., `name: api-port`)
    path: /minio/v2/metrics/cluster
    interval: 30s # How frequently Prometheus should scrape
    scheme: http # Use http or https based on your MinIO setup (must match MinIO service)
    # Since MINIO_PROMETHEUS_AUTH_TYPE is "public", no bearerTokenSecret is needed here.
```

**Important Replacements:**

  * `namespace: database`: Ensure this matches the namespace where your MinIO service is deployed.
  * `labels.release: prometheus-stack`: **Crucial:** Adjust this label to match the `serviceMonitorSelector` configured in your Prometheus Operator (or the Helm chart values for your Prometheus deployment, e.g., `kube-prometheus-stack`). This is how Prometheus discovers ServiceMonitors.
  * `selector.matchLabels.app: minio`: This must precisely match the labels defined on your MinIO Kubernetes Service.
  * `namespaceSelector.matchNames`: Ensure this includes the namespace where your MinIO service resides.
  * `port: api-port`: This must match the `name` you gave to the port in your MinIO Kubernetes Service definition (e.g., `name: api-port`).
  * `scheme: http`: Change to `https` if your MinIO instance uses TLS.

Apply the `ServiceMonitor`:

```bash
kubectl apply -f minio-servicemonitor.yaml
```

### 4\. Verification in Prometheus

After configuring Prometheus, verify that it is successfully scraping MinIO metrics:

1.  **Access Prometheus UI:**
    Port-forward your Prometheus service to access its UI (if not already accessible):

    ```bash
    kubectl port-forward service/<your-prometheus-service-name> 9090:9090 -n <your-prometheus-namespace>
    ```

    For example:

    ```bash
    kubectl port-forward service/prometheus-kube-prometheus-operator 9090:9090 -n monitoring # Common for kube-prometheus-stack
    ```

    Then, open your web browser and navigate to `http://localhost:9090`.

2.  **Check Targets:**
    In the Prometheus UI, go to `Status` -\> `Targets`. You should see a target named `minio` (or matching your `job_name` / `ServiceMonitor` configuration) with a state of `UP`. This indicates Prometheus is successfully connecting and scraping metrics.

3.  **Query Metrics:**
    Navigate to the `Graph` tab. In the expression input, start typing `minio_` and Prometheus should auto-complete with available MinIO metrics. Try querying for metrics like `minio_disk_storage_free_bytes` or `minio_cluster_capacity_raw_total_bytes` to see data for your MinIO instance.

By following these steps, you will have successfully enabled Prometheus metrics for your MinIO deployment in Kubernetes and configured Prometheus to collect them.