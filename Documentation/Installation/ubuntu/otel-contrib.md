# OpenTelemetry Collector Installation Steps

## Installation as a Service

### Prerequisites

* A Linux system (e.g., Ubuntu, CentOS)
* `sudo` privileges
* Basic command-line knowledge

### Installation Steps

1.  **Download Collector Contrib Binary:**
    * Go to the OpenTelemetry Collector Contrib Releases page: <https://github.com/open-telemetry/opentelemetry-collector-contrib/releases>
    * Download the appropriate binary for your architecture (e.g., `otelcol-contrib_*.linux_amd64.tar.gz`).
    * Extract the archive:
        ```bash
        # Example for version 0.98.0, adjust version and architecture as needed
        
        wget https://github.com/open-telemetry/opentelemetry-collector-contrib/releases/download/v0.98.0/otelcol-contrib_0.98.0_linux_amd64.tar.gz

        tar xvf otelcol-contrib_*.tar.gz
        ```

2.  **Move Binary:**
    * Move the extracted binary to `/usr/local/bin`:
        ```bash
        sudo mv otelcol-contrib /usr/local/bin/
        ```

3.  **Create Directories and Configuration File:**
    * Create directories for configuration:
        ```bash
        sudo mkdir /etc/otelcol-contrib
        ```
    * Create your configuration file (e.g., using the example above) and place it in the directory:
        ```bash
        sudo nano /etc/otelcol-contrib/otel-config.yaml
        # Paste your configuration here
        ```

4.  **Create a Collector User and Group:**
    * For security, create a dedicated user and group:
        ```bash
        sudo groupadd --system otelcol
        sudo useradd -g otelcol --no-create-home --shell=/bin/false otelcol
        sudo chown -R otelcol:otelcol /etc/otelcol-contrib
        # If using file-based storage (e.g., filelog receiver), ensure permissions for data directories too.
        ```

### Configuration

* The main configuration file is `/etc/otelcol-contrib/otel-config.yaml`. Edit this file to define your telemetry pipeline.

### Running the Collector as a Systemd Service

1.  **Create a Systemd Service File:**
    * Create a file at `/etc/systemd/system/otelcol-contrib.service`:
        ```bash
        sudo nano /etc/systemd/system/otelcol-contrib.service
        ```
    * Add the following content:
        ```ini
        [Unit]
        Description=OpenTelemetry Collector Contrib
        Requires=network.target
        After=network.target

        [Service]
        User=otelcol
        Group=otelcol
        Type=simple
        ExecStart=/usr/local/bin/otelcol-contrib --config /etc/otelcol-contrib/otel-config.yaml
        Restart=always
        RestartSec=5

        [Install]
        WantedBy=multi-user.target
        ```

2.  **Enable and Start the Service:**
    ```bash
    sudo systemctl daemon-reload
    sudo systemctl enable otelcol-contrib
    sudo systemctl start otelcol-contrib
    sudo systemctl status otelcol-contrib # Check the status
    journalctl -u otelcol-contrib -f # View logs
    ```

## Installation with Docker Compose

This method uses Docker and Docker Compose for a containerized deployment.

### Prerequisites

* Docker installed: [Get Docker](https://docs.docker.com/get-docker/)
* Docker Compose installed: [Get Docker Compose](https://docs.docker.com/compose/install/)

### Installation Steps

1.  **Create Configuration File:**
    * Create your `otel-config.yaml` file in a local directory (e.g., `./otel-config.yaml`). Use the example provided earlier as a starting point.

2.  **Create Docker Compose File:**
    * Create a file named `docker-compose.yml` in the same directory:
        ```bash
        nano docker-compose.yml
        ```
    * Add the following content:
        ```yaml
        version: '3.7'
        services:
          otel-collector:
            image: otel/opentelemetry-collector-contrib:latest # Use a specific version tag in production
            container_name: otel-collector-contrib
            command: ["--config=/etc/otelcol-contrib/otel-config.yaml"]
            volumes:
              - ./otel-config.yaml:/etc/otelcol-contrib/otel-config.yaml
            ports:
              # OTLP ports (adjust if needed)
              - "4317:4317" # OTLP gRPC
              - "4318:4318" # OTLP HTTP
              # Other receiver ports (e.g., Jaeger, Zipkin)
              # - "14250:14250" # Jaeger gRPC
              # - "9411:9411" # Zipkin
              # Extension ports (adjust if needed)
              - "13133:13133" # Health check
              - "1777:1777"   # pprof
            restart: always
            # Add memory limits appropriate for your system
            # deploy:
            #   resources:
            #     limits:
            #       memory: 1G
        ```

### Configuration

* The configuration is managed by the `otel-config.yaml` file mounted into the container.
* Ensure the ports exposed in `docker-compose.yml` match the endpoints defined in your `otel-config.yaml` for receivers and extensions.

### Running the Collector

1.  **Start the Collector:**
    * Run the following command in the directory containing `docker-compose.yml` and `otel-config.yaml`:
        ```bash
        docker-compose up -d
        ```

2.  **Check Logs:**
    ```bash
    docker-compose logs -f otel-collector
    ```

## Installation on Kubernetes

Deploying the Collector on Kubernetes is common for cloud-native environments.

### Prerequisites

* A running Kubernetes cluster
* `kubectl` command-line tool configured
* Helm (Recommended for Helm chart installation)

### Installation Methods

#### Using the OpenTelemetry Operator (Recommended)

The Operator simplifies deployment, configuration management (using Custom Resources like `OpenTelemetryCollector`), and upgrades.

1.  **Install the Operator:** Follow the official installation guide: <https://opentelemetry.io/docs/kubernetes/operator/installation/>
    ```bash
    # Example command, check official docs for the latest version
    kubectl apply -f https://github.com/open-telemetry/opentelemetry-operator/releases/latest/download/opentelemetry-operator.yaml
    ```

2.  **Create an `OpenTelemetryCollector` Custom Resource:**
    * Define your collector configuration within the `spec.config` field of the CR.
    * Example `otelcol-cr.yaml`:
        ```yaml
        apiVersion: opentelemetry.io/v1alpha1
        kind: OpenTelemetryCollector
        metadata:
          name: otelcol-contrib-deployment # Or choose DaemonSet/StatefulSet mode
        spec:
          mode: deployment # Other modes: daemonset, statefulset
          image: otel/opentelemetry-collector-contrib:latest # Use specific version
          config: | # Paste your otel-config.yaml content here
            receivers:
              otlp:
                protocols:
                  grpc:
                    endpoint: 0.0.0.0:4317
                  http:
                    endpoint: 0.0.0.0:4318
            processors:
              batch: {}
              memory_limiter:
                 check_interval: 1s
                 limit_mib: 512
                 spike_limit_mib: 128
            exporters:
              logging:
                loglevel: info
            extensions:
              health_check: {}
              pprof: {}
            service:
              extensions: [health_check, pprof]
              pipelines:
                traces:
                  receivers: [otlp]
                  processors: [memory_limiter, batch]
                  exporters: [logging]
                metrics:
                  receivers: [otlp]
                  processors: [memory_limiter, batch]
                  exporters: [logging]
                logs:
                  receivers: [otlp]
                  processors: [memory_limiter, batch]
                  exporters: [logging]
        ```
    * Apply the CR: `kubectl apply -f otelcol-cr.yaml -n <your-namespace>`

#### Using the Helm Chart

Helm provides a templated way to deploy the collector.

1.  **Add Helm Repository:**
    ```bash
    helm repo add open-telemetry [https://open-telemetry.github.io/opentelemetry-helm-charts](https://open-telemetry.github.io/opentelemetry-helm-charts)
    helm repo update
    ```

2.  **Install the Chart:**
    * You'll need to provide your configuration, typically via a `values.yaml` file or `--set` flags.
    * Create a `values.yaml` file:
        ```yaml
        # values.yaml
        mode: deployment # Or daemonset/statefulset
        image:
          repository: otel/opentelemetry-collector-contrib
          tag: latest # Use specific version
        config: | # Paste your otel-config.yaml content here
          receivers:
            otlp:
              protocols:
                grpc:
                  endpoint: 0.0.0.0:4317
                http:
                  endpoint: 0.0.0.0:4318
          # ... rest of your config like Exporters, etc....
          service:
            pipelines:
              traces:
                receivers: [otlp]
                processors: [batch]
                exporters: [logging]
        ```
    * Install the chart:
        ```bash
        helm install my-otel-collector open-telemetry/opentelemetry-collector -f values.yaml -n <your-namespace> --create-namespace
        ```

### Configuration

* **Operator:** Configuration is embedded within the `OpenTelemetryCollector` CR's `spec.config` field.
* **Helm:** Configuration is typically provided in the `config` section of the `values.yaml` file. Alternatively, you can mount a ConfigMap containing your `otel-config.yaml`.

### Reference to actual file:
```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

exporters:
  otlp:
    endpoint: tempo:4317
    tls:
      insecure: true
  otlp/logs:
    endpoint: "0.0.0.0:4317"
    tls:
      insecure: true
  prometheus:
    endpoint: "0.0.0.0:8889"
  opensearch:
    logs_index: otel
    http:
      endpoint: "http://opensearch:9200"
      tls:
        insecure: true

connectors:
  spanmetrics:
    dimensions:
      - name: http.method
      - name: http.status_code
      - name: http.route

processors:
  batch:

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp,spanmetrics]
    metrics/spanmetrics:
      receivers: [otlp,spanmetrics]
      processors: [batch]
      exporters: [prometheus]
    logs:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp/logs, opensearch]


```

### Deployment Strategies

Choose the Kubernetes deployment strategy based on your needs:

* **Deployment:** A central collector instance (or multiple replicas) receiving data from applications. Good for stateless processing or when applications push data directly.
* **DaemonSet:** Runs one collector instance per node. Ideal for collecting node-level metrics, logs, or when applications send data to a local agent (e.g., via UDP or localhost).
* **StatefulSet:** Similar to Deployment but provides stable network identifiers and persistent storage per pod. Useful if processors or exporters require persistent state (less common).

## Important Considerations

* **Versioning:** Always use specific version tags for the collector image (e.g., `otel/opentelemetry-collector-contrib:0.98.0`) in production instead of `latest`.
* **Resource Limits:** Configure appropriate CPU and memory requests/limits for the collector container, especially in Kubernetes. Use the `memory_limiter` processor to prevent crashes.
* **Security:** Secure communication channels (e.g., use TLS for OTLP exporters). If exposing receiver ports externally, ensure proper network policies and authentication are in place.
* **Configuration Management:** Store your `otel-config.yaml` in version control.
* **Monitoring the Collector:** The collector exposes its own metrics (usually via Prometheus) and has a health check extension. Monitor these to ensure the collector itself is healthy.

## Troubleshooting

* **Check Logs:** The primary source of information. Use the appropriate command for your environment ( `journalctl`, `docker logs`, `kubectl logs`). Increase log verbosity in the `logging` exporter if needed (`loglevel: debug`).
* **Validate Configuration:** Use the collector binary to validate your config file before starting: `otelcol-contrib --config /path/to/otel-config.yaml --validate`
* **Check Ports:** Ensure firewalls or network policies aren't blocking required ports for receivers or exporters.
* **Resource Issues:** Check CPU/memory usage. Adjust limits and ensure the `memory_limiter` processor is configured.
* **Health Check:** Access the health check endpoint (e.g., `http://localhost:13133`) if enabled.
* **Exporter Endpoints:** Verify exporter endpoints are correct and reachable from the collector. Check for authentication/token issues.
