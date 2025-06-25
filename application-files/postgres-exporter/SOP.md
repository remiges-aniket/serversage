To install and configure `postgres-exporter` on Kubernetes, follow this comprehensive Standard Operating Procedure (SOP):

## Standard Operating Procedure: PostgreSQL Exporter Deployment on Kubernetes

This SOP outlines the steps to deploy the Prometheus `postgres-exporter` on a Kubernetes cluster to monitor your PostgreSQL database instances. This guide assumes you have a running Kubernetes cluster and a PostgreSQL database instance accessible from within the cluster.

### 1\. Prerequisites

Before you begin, ensure the following prerequisites are met:

  * **Kubernetes Cluster:** A functional Kubernetes cluster (version 1.16+ is generally recommended for recent `postgres-exporter` versions).
  * **`kubectl`:** Configured to interact with your Kubernetes cluster.
  * **PostgreSQL Database:** A running PostgreSQL instance. The `postgres-exporter` will connect to this database.
  * **`pg_stat_statements` Extension (Recommended):** For comprehensive query performance metrics, the `pg_stat_statements` extension should be enabled in your PostgreSQL configuration.
      * To enable it, add `shared_preload_libraries = 'pg_stat_statements'` to your `postgresql.conf` file and restart PostgreSQL.
      * Then, execute `CREATE EXTENSION pg_stat_statements;` in your PostgreSQL database.
  * **PostgreSQL User and Permissions:** A dedicated PostgreSQL user with appropriate permissions for monitoring.

### 2\. PostgreSQL User Configuration

It is highly recommended to create a dedicated, non-superuser PostgreSQL user for the `postgres-exporter` with the principle of least privilege. Using the `postgres` user with the `postgres` database is often suggested for better visibility and simplified setup, but a dedicated user is more secure.

**Steps to create a dedicated user and grant permissions:**

1.  **Connect to your PostgreSQL database:**

    ```bash
    psql -U postgres -d postgres
    ```

    (Replace `postgres` with your superuser and `postgres` with your database name if different)

2.  **Create the `postgres_exporter` user:**

    ```sql
    CREATE USER postgres_exporter WITH PASSWORD 'your_secure_password';
    ```

    **Note:** Replace `'your_secure_password'` with a strong, unique password.

3.  **Grant Permissions:**

      * **For PostgreSQL versions 10 and above:**
        The `pg_monitor` built-in role simplifies permission management significantly.

        ```sql
        GRANT pg_monitor TO postgres_exporter;
        ```

        This role grants the necessary permissions to access most `pg_stat_*` views and functions required by the exporter.

      * **For PostgreSQL versions older than 10, or for granular control:**
        If `pg_monitor` is not available or if you need more granular control, grant the following specific permissions:

        ```sql
        GRANT CONNECT ON DATABASE postgres TO postgres_exporter; -- Or your target database name
        GRANT SELECT ON pg_stat_database TO postgres_exporter;
        GRANT SELECT ON pg_stat_activity TO postgres_exporter;
        GRANT SELECT ON pg_stat_replication TO postgres_exporter;
        -- Add other pg_stat_* views as needed based on desired metrics
        -- For pg_stat_statements, you need to grant SELECT on pg_stat_statements:
        GRANT SELECT ON pg_stat_statements TO postgres_exporter;
        ```

        **Important Note:** If you do not use the `postgres` database and specify a different database in the `DATA_SOURCE_NAME` environment variable, the exporter will *only* analyze or monitor that specific database. To monitor multiple databases, you might need to enable `PG_EXPORTER_AUTO_DISCOVER_DATABASES` and ensure the `postgres_exporter` user has `CONNECT` privilege on all databases you wish to monitor.

### 3\. Kubernetes Deployment

We will create Kubernetes manifests for a Secret (to hold database credentials), a Deployment (for the `postgres-exporter` pods), and a Service (to expose the exporter's metrics endpoint).

#### 3.1. Create a Kubernetes Secret for Database Credentials

Create a `Secret` to securely store the `postgres_exporter` user's password.

**`postgres-exporter-secret.yaml`**

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: postgres-exporter-secret
  namespace: default # Or your desired namespace
type: Opaque
stringData:
  # The DATA_SOURCE_NAME environment variable for postgres-exporter
  # Format: "postgresql://user:password@host:port/database?sslmode=disable"
  # Use your PostgreSQL service name (e.g., 'your-postgresql-service') or IP
  # Ensure the database name is correct (e.g., 'postgres')
  DATA_SOURCE_NAME: "postgresql://postgres_exporter:your_secure_password@your-postgresql-service:5432/postgres?sslmode=disable"
```

**Replace:**

  * `your_secure_password` with the password you set for `postgres_exporter`.
  * `your-postgresql-service` with the actual Kubernetes Service name (or IP address) of your PostgreSQL database.
  * `postgres` with the specific database name you want to monitor (e.g., `my_application_db`).

Apply the secret:

```bash
kubectl apply -f postgres-exporter-secret.yaml
```

#### 3.2. Create Kubernetes Deployment for PostgreSQL Exporter

This manifest defines the `Deployment` for the `postgres-exporter` pods.

**`postgres-exporter-deployment.yaml`**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres-exporter
  namespace: default # Or your desired namespace
  labels:
    app: postgres-exporter
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres-exporter
  template:
    metadata:
      labels:
        app: postgres-exporter
    spec:
      containers:
      - name: postgres-exporter
        image: quay.io/prometheuscommunity/postgres-exporter:latest # Use a specific version in production (e.g., v0.12.0)
        ports:
        - name: metrics
          containerPort: 9187
        env:
        - name: DATA_SOURCE_NAME
          valueFrom:
            secretKeyRef:
              name: postgres-exporter-secret
              key: DATA_SOURCE_NAME
        # Optional: Disable default metrics or include/exclude specific databases
        # - name: PG_EXPORTER_DISABLE_DEFAULT_METRICS
        #   value: "false"
        # - name: PG_EXPORTER_AUTO_DISCOVER_DATABASES
        #   value: "true" # Set to true to discover and monitor all databases the user has CONNECT access to
        # - name: PG_EXPORTER_EXCLUDE_DATABASES
        #   value: "template0,template1"
        # - name: PG_EXPORTER_INCLUDE_DATABASES
        #   value: "your_target_db1,your_target_db2"
        resources:
          requests:
            memory: "64Mi"
            cpu: "50m"
          limits:
            memory: "128Mi"
            cpu: "100m"
        livenessProbe:
          httpGet:
            path: /healthz
            port: metrics
          initialDelaySeconds: 10
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /metrics
            port: metrics
          initialDelaySeconds: 5
          periodSeconds: 5
```

**Note on `DATA_SOURCE_NAME`:**
The `DATA_SOURCE_NAME` environment variable is critical. It defines how the exporter connects to PostgreSQL.

  * If `PG_EXPORTER_AUTO_DISCOVER_DATABASES` is set to `true`, the `postgres_exporter` user must have `CONNECT` permissions on all databases you want to monitor.
  * If `PG_EXPORTER_AUTO_DISCOVER_DATABASES` is `false` (default), the exporter will only monitor the database specified in the `DATA_SOURCE_NAME` connection string.

Apply the deployment:

```bash
kubectl apply -f postgres-exporter-deployment.yaml
```

#### 3.3. Create Kubernetes Service for PostgreSQL Exporter

This manifest exposes the `postgres-exporter` metrics endpoint within the cluster.

**`postgres-exporter-service.yaml`**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres-exporter
  namespace: default # Or your desired namespace
  labels:
    app: postgres-exporter
spec:
  selector:
    app: postgres-exporter
  ports:
    - protocol: TCP
      port: 9187
      targetPort: metrics # Refers to the port name 'metrics' in the Deployment
      name: metrics
  type: ClusterIP # Use ClusterIP for internal cluster access, or NodePort/LoadBalancer if you need external access (not recommended for metrics)
```

Apply the service:

```bash
kubectl apply -f postgres-exporter-service.yaml
```

### 4\. Verification

After deploying, verify that the `postgres-exporter` pod is running and exposing metrics:

1.  **Check Pod Status:**

    ```bash
    kubectl get pods -n default -l app=postgres-exporter
    ```

    Ensure the pod is in a `Running` state.

2.  **Check Service Endpoints:**

    ```bash
    kubectl get endpoints -n default postgres-exporter
    ```

    Verify that the endpoint points to your `postgres-exporter` pod.

3.  **Access Metrics (Port Forwarding for testing):**

    ```bash
    kubectl port-forward svc/postgres-exporter 9187:9187 -n default
    ```

    Then, open your web browser and navigate to `http://localhost:9187/metrics`. You should see a long list of PostgreSQL metrics.

### 5\. Integration with Prometheus (Optional)

If you have a Prometheus instance running in your cluster, you can configure it to scrape metrics from the `postgres-exporter` service.

Add a new scrape job to your Prometheus configuration (e.g., in `prometheus.yml` or a `ServiceMonitor` if using Prometheus Operator):

```yaml
# Example Prometheus scrape configuration
- job_name: 'postgres'
  static_configs:
    - targets: ['postgres-exporter.default.svc.cluster.local:9187'] # Adjust service name and namespace if different
  # Or if using Kubernetes service discovery:
  # kubernetes_sd_configs:
  # - role: endpoints
  #   namespaces:
  #     names: ['default'] # Or your namespace
  # relabel_configs:
  # - source_labels: [__meta_kubernetes_service_label_app]
  #   regex: postgres-exporter
  #   action: keep
  # - source_labels: [__meta_kubernetes_endpoint_port_name]
  #   regex: metrics
  #   action: keep
```

### 6\. Important Notes and Considerations

  * **Permissions:** Always adhere to the principle of least privilege. The `pg_monitor` role is ideal for modern PostgreSQL versions. For older versions, grant only the necessary `SELECT` permissions on `pg_stat_*` views.
  * **Database Monitoring Scope:**
      * By default, `postgres-exporter` (without `PG_EXPORTER_AUTO_DISCOVER_DATABASES=true`) will only monitor the database specified in the `DATA_SOURCE_NAME` connection string.
      * If you need to monitor all databases on a PostgreSQL instance, set `PG_EXPORTER_AUTO_DISCOVER_DATABASES` to `true` in your deployment manifest. Ensure the `postgres_exporter` user has `CONNECT` privilege on all databases you want to monitor. You can use `PG_EXPORTER_EXCLUDE_DATABASES` or `PG_EXPORTER_INCLUDE_DATABASES` to filter.
  * **Security:**
      * Never expose the `postgres-exporter` service directly to the public internet. Use `ClusterIP` for internal cluster access.
      * Store PostgreSQL credentials in Kubernetes Secrets and mount them as environment variables, as shown in this SOP.
  * **Custom Queries:** For additional metrics not provided by default, you can define custom queries in a YAML file and provide its path to the exporter using the `PG_EXPORTER_EXTEND_QUERY_PATH` environment variable.
  * **PostgreSQL Running:** This SOP assumes your PostgreSQL database is already running and accessible from the Kubernetes cluster.
  * **Resource Limits:** Adjust `resources` requests and limits in the Deployment YAML based on your environment and expected load.
  * **Image Versioning:** In a production environment, always pin your Docker image to a specific version (e.g., `quay.io/prometheuscommunity/postgres-exporter:v0.12.0`) to ensure consistent deployments.