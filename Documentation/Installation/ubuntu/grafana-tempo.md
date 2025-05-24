# Grafana Tempo Installation Methods

## Installation as a Service

Installs Tempo directly on a Linux server. Suitable for single-node deployments or testing.

### Installation Steps

1.  **Download Tempo Binary:**
    * Go to the Grafana Tempo Releases page: <https://github.com/grafana/tempo/releases>
    * Download the appropriate binary for your architecture (e.g., `tempo-linux-amd64.gz`).
    * Unzip the binary:
        ```bash
        # Adjust version and architecture as needed
        wget [https://github.com/grafana/tempo/releases/download/v2.4.1/tempo-linux-amd64.gz](https://github.com/grafana/tempo/releases/download/v2.4.1/tempo-linux-amd64.gz)
        gunzip tempo-linux-amd64.gz
        chmod +x tempo-linux-amd64
        ```

2.  **Move Binary:**
    ```bash
    sudo mv tempo-linux-amd64 /usr/local/bin/tempo
    ```

3.  **Create Directories and Configuration File:**
    ```bash
    sudo mkdir -p /etc/tempo /var/tempo/blocks /var/tempo/generator/wal
    sudo nano /etc/tempo/tempo.yaml
    # Paste your configuration (like the example below) here
    ```

    `tempo.yaml` file below for reference, used in actual case.
    ```yaml
    stream_over_http_enabled: true
    server:
      http_listen_port: 3200
      log_level: info

    query_frontend:
      search:
        duration_slo: 5s
        throughput_bytes_slo: 1.073741824e+09
        metadata_slo:
            duration_slo: 5s
            throughput_bytes_slo: 1.073741824e+09
      trace_by_id:
        duration_slo: 5s

    distributor:
      receivers:
        otlp:
          protocols:
            grpc:
              endpoint: "tempo:4317"

    ingester:
      max_block_duration: 5m               # cut the headblock when this much time passes. this is  being set for demo purposes and should probably be left alone normally

    compactor:
      compaction:
        block_retention: 1h                # overall Tempo trace retention. set for demo purposes

    metrics_generator:
      registry:
        external_labels:
          source: tempo
          cluster: docker-compose
      storage:
        path: /var/tempo/generator/wal
        remote_write:
          - url: http://prometheus:9090/api/v1/write
            send_exemplars: true
      traces_storage:
        path: /var/tempo/generator/traces

    storage:
      trace:
        backend: local                     # backend configuration to use
        wal:
          path: /var/tempo/wal             # where to store the wal locally
        local:
          path: /var/tempo/blocks

    overrides:
      defaults:
        metrics_generator:
          processors: [service-graphs, span-metrics, local-blocks] # enables metrics generator
          generate_native_histograms: both    

    ```

4.  **Create a Tempo User a..nd Group:**
    ```bash
    sudo groupadd --system tempo
    sudo useradd -g tempo --no-create-home --shell=/bin/false tempo
    sudo chown -R tempo:tempo /etc/tempo /var/tempo
    ```

### Running Tempo as a Systemd Service

1.  **Create a Systemd Service File:**
    ```bash
    sudo nano /etc/systemd/system/tempo.service
    ```
    * Add the following content:
        ```ini
        [Unit]
        Description=Grafana Tempo
        Wants=network-online.target
        After=network-online.target

        [Service]
        User=tempo
        Group=tempo
        Type=simple
        ExecStart=/usr/local/bin/tempo -config.file=/etc/tempo/tempo.yaml
        Restart=always
        RestartSec=5

        [Install]
        WantedBy=multi-user.target
        ```

2.  **Enable and Start the Service:**
    ```bash
    sudo systemctl daemon-reload
    sudo systemctl enable tempo
    sudo systemctl start tempo
    sudo systemctl status tempo # Check status
    journalctl -u tempo -f # View logs
    ```

### Persistent Storage (Service)

* Ensure the `path` specified in `storage.trace.local.path` (e.g., `/var/tempo/blocks`) resides on a persistent disk partition. This could be a dedicated partition or a mount point for network-attached storage.
* The systemd service automatically uses this path as configured in `tempo.yaml`. Verify the `tempo` user has write permissions to this specific directory.

## Installation with Docker Compose

Uses Docker for a containerized deployment. Good for local development or single-node setups.

### Installation Steps

1.  **Create Configuration File:**
    * Create your `tempo.yaml` file in a local directory (e.g., `./tempo.yaml`).

2.  **Create Docker Compose File:**
    * Create `docker-compose.yml` in the same directory:
        ```yaml
        version: '3.7'

        volumes:
          tempo-data: {} # Declare a named volume for persistence

        services:
          tempo:
            image: grafana/tempo:latest # Use specific version tag in production (e.g., 2.4.1)
            container_name: tempo
            command: ["-config.file=/etc/tempo/tempo.yaml"]
            volumes:
              - ./tempo.yaml:/etc/tempo/tempo.yaml
              - tempo-data:/var/tempo # Mount the named volume to the data path
            ports:
              - "3200:3200"   # Tempo HTTP server
              - "4317:4317"   # OTLP gRPC receiver
              - "4318:4318"   # OTLP HTTP receiver
              # Add other receiver ports if configured (e.g., 14250 for Jaeger gRPC)
            restart: unless-stopped
            # Add resource limits if needed
            # deploy:
            #   resources:
            #     limits:
            #       memory: 2G
        ```

### Running Tempo

1.  **Start Tempo:**
    ```bash
    docker-compose up -d
    ```

2.  **Check Logs:**
    ```bash
    docker-compose logs -f tempo
    ```

### Persistent Storage (Docker Compose)

* The example uses a Docker **named volume** (`tempo-data`) mounted to `/var/tempo`. This ensures data persists even if the container is removed and recreated, as the volume exists independently on the host system managed by Docker.
* Make sure the `storage.trace.local.path` in your `tempo.yaml` matches the mount point inside the container (e.g., `/var/tempo/blocks`). The container's internal user needs write access to this mounted volume.
* Alternatively, you could use a bind mount (e.g., `- ./tempo-data-host:/var/tempo`) to map a specific host directory. This can be simpler for direct access but requires careful management of host permissions. Named volumes are generally preferred for better abstraction and management by Docker.

## Installation on Kubernetes

Recommended for production deployments, leveraging Kubernetes orchestration capabilities.

### Using the Official Helm Chart (Recommended)

The official Helm chart simplifies deployment and configuration.

1.  **Add Helm Repository:**
    ```bash
    helm repo add grafana [https://grafana.github.io/helm-charts](https://grafana.github.io/helm-charts)
    helm repo update
    ```

2.  **Configure Values:**
    * Create a `values.yaml` file to customize the deployment. Key settings include:
        * `tempo.storage`: Configure your backend (local, s3, gcs, azure).
        * `persistence.enabled`: Set to `true` for persistent storage using PVCs.
        * `persistence.storageClassName`: Specify your Kubernetes StorageClass name. Ensure this StorageClass exists in your cluster.
        * `persistence.size`: Set the requested volume size (e.g., `50Gi`).
        * `tempo.receivers`: Enable and configure receivers (OTLP is often enabled by default).
    * **Example `values.yaml` for local persistence:**
        ```yaml
        # values.yaml (minimal example for local persistence)
        persistence:
          enabled: true
          # Replace with your StorageClass name (e.g., standard, gp2, managed-premium)
          # Ensure this StorageClass exists and can provision volumes.
          storageClassName: "standard"
          size: 50Gi # Adjust size as needed

        tempo:
          searchEnabled: false # Disable search by default unless configured
          # Default storage is local path within the PVC mounted at /var/tempo
          storage:
            trace:
              backend: local
              local:
                path: /var/tempo/blocks # This path is inside the persistent volume
          # Receivers are usually enabled by default in the chart, verify if needed
          # receivers:
          #   otlp:
          #     protocols:
          #       grpc:
          #         endpoint: 0.0.0.0:4317
          #       http:
          #         endpoint: 0.0.0.0:4318
        ```
    * **Example `values.yaml` for S3 persistence:**
        ```yaml
        # values.yaml (minimal example for S3 persistence)
        # Persistence for WAL/local cache if needed, not for primary trace storage
        # Set to false if trace data is primarily in S3 and local WAL persistence isn't required
        persistence:
          enabled: false # Typically disabled when using object storage for traces

        # Secrets should be created separately in Kubernetes and referenced
        # Example: kubectl create secret generic tempo-s3 --from-literal=access_key=YOUR_KEY --from-literal=secret_key=YOUR_SECRET -n tempo
        extraSecretMounts:
         - name: tempo-s3-secret-mount
           secretName: tempo-s3 # Name of the k8s secret created above
           mountPath: /etc/secrets/s3 # Path where the secret will be mounted inside the pod
           readOnly: true

        tempo:
          searchEnabled: false
          storage:
            trace:
              backend: s3
              s3:
                bucket: your-tempo-s3-bucket
                endpoint: s3.your-region.amazonaws.com # e.g., s3.us-east-1.amazonaws.com
                # Reference keys from the mounted secret volume
                access_key: /etc/secrets/s3/access_key
                secret_key: /etc/secrets/s3/secret_key
        ```

3.  **Install the Chart:**
    ```bash
    # Install using monolithic mode (simpler, less scalable)
    helm install tempo grafana/tempo -f values.yaml -n tempo --create-namespace

    # Or install using microservices mode (more complex, more scalable)
    # helm install tempo grafana/tempo-distributed -f values.yaml -n tempo --create-namespace
    ```

### Persistent Storage (Kubernetes)

* When using the Helm chart with `persistence.enabled=true`:
    * The chart defines a PersistentVolumeClaim (PVC) resource. Kubernetes attempts to bind this PVC to a suitable PersistentVolume (PV).
    * You **must** have a `StorageClass` available in your cluster that supports dynamic provisioning (automatically creates a PV and underlying storage) OR you must manually create a PV that matches the PVC's requirements (size, access modes).
    * Specify the correct `storageClassName` in your `values.yaml`. If omitted, the cluster's default StorageClass might be used, which may or may not be suitable. Check your cluster's configuration.
* If using object storage (S3, GCS, Azure) as the primary `storage.trace.backend`, trace data resides externally. You typically disable chart persistence (`persistence.enabled=false`) for the main trace storage. You might still enable persistence for specific components like the ingester's Write-Ahead Log (WAL) via their respective Helm values if needed for improved resilience during restarts, but the bulk trace data is managed by the object store.

## Retention Configuration

Tempo's retention primarily applies to **completed blocks** of trace data. The mechanism differs significantly between object storage and local storage.

### Object Storage Retention (S3, GCS, Azure)

* This is the most common, scalable, and recommended way to manage retention in Tempo for production environments.
* Configure the `block_retention` parameter within the `compactor` section of your `tempo.yaml` or the corresponding Helm chart values (`tempo.compaction.block_retention`). This duration dictates how long compacted blocks are kept *before* the compactor considers them eligible for deletion marking.
    ```yaml
    compactor:
      compaction:
        # ... other compaction settings ...
        # Specifies the minimum age of a block before it's marked for deletion.
        block_retention: 720h # Example: Keep blocks for 30 days (30 * 24h)
    ```
* **How it works:** The compactor periodically scans for blocks older than the configured `block_retention`. For eligible blocks, it writes or updates a special `marker.json` file within the tenant's directory in the object storage bucket. This file lists blocks marked for deletion.
* **Actual Deletion (Crucial!):** Tempo **does not directly delete the data blocks** from the object storage bucket. You **must** configure **lifecycle policies** on your storage bucket itself (e.g., S3 Lifecycle Rules, GCS Object Lifecycle Management, Azure Blob Storage lifecycle management). These policies should be configured to:
    1.  Delete the `marker.json` file after a short period (e.g., 24-48 hours) to prevent accidental resurrection of deletion markers.
    2.  Delete the actual trace block objects (`*.gz` files, directories) after they reach an age corresponding to your desired retention period plus a safety margin (e.g., `block_retention` + 2 days). Alternatively, some configurations use the presence of the `marker.json` to trigger deletion, but age-based deletion is often simpler. Consult the Tempo documentation and your cloud provider's documentation for precise lifecycle rule configuration examples. Failure to configure these rules means data will accumulate indefinitely in your bucket.

### Local Storage Retention

* Tempo's built-in retention mechanisms, including `compactor.block_retention`, **do not actively delete files** from the local filesystem backend. The `block_retention` setting in this context mainly influences the compactor's internal state regarding which blocks it considers active or potentially compactable.
* **Manual/External Cleanup Required:** For local storage deployments, **you are responsible** for implementing your own data cleanup mechanism outside of Tempo. This typically involves:
    * A scheduled script (e.g., using `find <path> -type f -mtime +<days> -delete` or similar commands) run periodically via `cron`, systemd timers, or another scheduler.
    * The script must carefully target the trace block directory specified in `storage.trace.local.path` (e.g., `/var/tempo/blocks`).
    * It needs to identify and delete files or directories older than your desired retention period (e.g., based on modification time).
    * **Caution:** Implementing this script requires extreme care. Incorrectly targeting files or running with excessive permissions could lead to deletion of active Tempo data or other system files. Test your cleanup script thoroughly in a non-critical environment before deploying to production. Ensure the script runs with appropriate permissions to delete files owned by the `tempo` user/group.

## Important Considerations

* **Deployment Mode:** Choose the right deployment mode (monolithic vs. microservices) based on your expected trace volume, query load, and operational complexity tolerance. Microservices offer better independent scalability for components like ingesters, queriers, and compactors but require more intricate configuration and resource management.
* **Storage Backend:** Object storage (S3, GCS, Azure) is strongly recommended for production and scalable deployments due to better cost-effectiveness at scale, high durability, and native integration with lifecycle management for reliable retention. Local storage is simpler to set up for testing or small single-node deployments but requires manual retention management and careful capacity planning.
* **Resource Allocation:** Allocate sufficient CPU, memory, and disk IOPS/bandwidth. Ingesters are often memory/CPU intensive, while compactors can require significant IO and CPU during compaction windows. Monitor resource usage closely using Tempo's own metrics and system/Kubernetes metrics. Adjust resource requests/limits accordingly.
* **Configuration:** Keep your `tempo.yaml` or Helm `values.yaml` in version control (like Git) to track changes and facilitate rollbacks.
* **Monitoring Tempo:** Tempo exposes extensive Prometheus metrics (`/metrics` endpoint). Set up monitoring dashboards (e.g., in Grafana using the bundled Tempo dashboards) to track its health, performance (ingest rate, query latency, queue lengths), error rates, and storage usage. Configure alerting based on key metrics.

## Troubleshooting

* **Check Logs:** This is the first place to look for errors. Use `journalctl -u tempo -f`, `docker logs -f tempo`, or `kubectl logs <tempo-pod-name> [-c <container-name>] -n <namespace> -f`. Look for specific error messages related to configuration parsing, component startup, network connections, storage access, or request processing. Increase log verbosity by setting `server.log_level: debug` in the configuration if needed, but be mindful of increased log volume.
