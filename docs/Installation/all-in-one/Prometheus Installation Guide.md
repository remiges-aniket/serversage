# Prometheus Installation Guide

Prometheus is an open-source monitoring solution with a dimensional data model, flexible query language, efficient time series database and alerting approach.

This document provides instructions for installing Prometheus in the following ways:

* As a service on a Linux system
* Using Docker Compose
* On a Kubernetes cluster

It also covers configuring persistent storage and managing data retention.

## Table of Contents

* [Installation as a Service](#installation-as-a-service)
    * [Prerequisites](#prerequisites-1)
    * [Installation Steps](#installation-steps)
    * [Configuration](#configuration)
    * [Running Prometheus](#running-prometheus)
* [Installation with Docker Compose](#installation-with-docker-compose)
    * [Prerequisites](#prerequisites-2)
    * [Installation Steps](#installation-steps-1)
    * [Configuration](#configuration-1)
    * [Running Prometheus](#running-prometheus-1)
* [Installation on Kubernetes](#installation-on-kubernetes)
    * [Prerequisites](#prerequisites-3)
    * [Installation Steps](#installation-steps-2)
    * [Configuration](#configuration-2)
    * [Running Prometheus](#running-prometheus-2)
    * [Persistent Storage in Kubernetes](#persistent-storage-in-kubernetes)
* [Persistent Storage](#persistent-storage)
* [Data Retention](#data-retention)
    * [Retention Time](#retention-time)
    * [Retention Size](#retention-size)
* [Important Considerations](#important-considerations)
* [Troubleshooting](#troubleshooting)

## Installation as a Service

This method is suitable for installing Prometheus directly on a Linux server.

### Prerequisites

* A Linux system (e.g., Ubuntu, CentOS)
* `sudo` privileges
* Basic command-line knowledge

### Installation Steps

1.  **Download Prometheus:**
    * Visit the Prometheus download page: <https://prometheus.io/download/>
    * Download the appropriate pre-built binary for your system.
    * Extract the archive:
        ```bash
        tar xvf prometheus-*.tar.gz
        cd prometheus-*
        ```

2.  **Create Directories:**

    * Create directories for Prometheus data and configuration:

        ```bash
        sudo mkdir /etc/prometheus
        sudo mkdir /var/lib/prometheus
        ```

    * Copy the Prometheus binaries to `/usr/local/bin`:

        ```bash
        sudo cp prometheus /usr/local/bin/
        sudo cp promtool /usr/local/bin/
        ```

    * Copy the configuration files:

        ```bash
        sudo cp -r consoles /etc/prometheus
        sudo cp -r console_libraries /etc/prometheus
        sudo cp prometheus.yml /etc/prometheus/prometheus.yml
        ```

3.  **Create a Prometheus User and Group:**

    * For security, create a dedicated user and group for Prometheus:

        ```bash
        sudo groupadd --system prometheus
        sudo useradd -g prometheus --no-create-home --shell=/bin/false prometheus
        sudo chown -R prometheus:prometheus /etc/prometheus /var/lib/prometheus
        ```

### Configuration

* The main configuration file is `prometheus.yml`.  You can edit this file to configure scrape targets, alerting rules, and more.
* The default `prometheus.yml` file that comes with the download is a good starting point.
* See the [Prometheus configuration documentation](https://prometheus.io/docs/prometheus/latest/configuration/configuration/) for details.

### Running Prometheus as a Systemd Service

1.  **Create a Systemd Service File:**

    * Create a service file at `/etc/systemd/system/prometheus.service`:

        ```bash
        sudo nano /etc/systemd/system/prometheus.service
        ```

    * Add the following content:

        ```
        [Unit]
        Description=Prometheus
        Wants=network-online.target
        After=network-online.target

        [Service]
        User=prometheus
        Group=prometheus
        Type=simple
        ExecStart=/usr/local/bin/prometheus \
            --config.file /etc/prometheus/prometheus.yml \
            --storage.tsdb.path /var/lib/prometheus/ \
            --web.console.path=/etc/prometheus/consoles \
            --web.console.templates=/etc/prometheus/console_libraries
        Restart=always
        RestartSec=5
        [Install]
        WantedBy=multi-user.target
        ```

2.  **Enable and Start the Service:**

    ```bash
    sudo systemctl daemon-reload
    sudo systemctl enable prometheus
    sudo systemctl start prometheus
    sudo systemctl status prometheus # Check the status
    ```

3.  **Access Prometheus:**

    * Prometheus will be running on port `9090` by default.  Open your web browser and go to `http://<your_server_ip>:9090`.

## Installation with Docker Compose

This method uses Docker and Docker Compose to run Prometheus in a containerized environment.

### Prerequisites

* Docker installed on your system:  [Get Docker](https://docs.docker.com/get-docker/)
* Docker Compose installed: [Get Docker Compose](https://docs.docker.com/compose/install/)

### Installation Steps

1.  **Create a Docker Compose File:**

    * Create a file named `docker-compose.yml`:

        ```bash
        nano docker-compose.yml
        ```

    * Add the following content:

        ```yaml
        version: '3.7'
        services:
          prometheus:
            image: prom/prometheus:latest
            container_name: prometheus
            ports:
              - 9090:9090
            volumes:
              - ./prometheus.yml:/etc/prometheus/prometheus.yml
              - prometheus_data:/prometheus  # Use a named volume
            command:
              - '--config.file=/etc/prometheus/prometheus.yml'
              - '--storage.tsdb.path=/prometheus'
            restart: always
        volumes:
          prometheus_data: # Declare the named volume
        ```

2. **Create prometheus.yml**
    * Create a file named `prometheus.yml` in the same directory as the `docker-compose.yml`
    * Add the basic configuration, and modify as needed.
        ```yaml
        global:
          scrape_interval:     15s # Set the scrape interval to every 15 seconds.
          evaluation_interval: 15s # Set the evaluation interval to every 15 seconds.
        scrape_configs:
          - job_name: 'prometheus'
            static_configs:
            - targets: ['localhost:9090']
        ```

### Configuration

* The `prometheus.yml` file in the same directory as `docker-compose.yml` is used for configuration.  You can mount a custom configuration file as shown in the `docker-compose.yml` file.
* The  `prometheus_data` volume is used for persistent storage.  This is crucial, so your data persists across container restarts.
* See the [Prometheus configuration documentation](https://prometheus.io/docs/prometheus/latest/configuration/configuration/) for details.

### Running Prometheus

1.  **Start Prometheus:**

    * Run the following command in the directory where you created the `docker-compose.yml` file:

        ```bash
        docker-compose up -d
        ```

2.  **Access Prometheus:**

    * Prometheus will be running on port `9090`.  Open your web browser and go to `http://localhost:9090`.

## Installation on Kubernetes

This method is for deploying Prometheus in a Kubernetes cluster, which is ideal for scalable and resilient monitoring.

### Prerequisites

* A running Kubernetes cluster
* `kubectl` command-line tool configured to communicate with your cluster
* A `StorageClass` configured in your Kubernetes cluster for persistent volumes (if you need persistent storage)
* Helm (Optional, but recommended for easier installation via the Prometheus Operator)

### Installation Steps

There are several ways to install Prometheus on Kubernetes. Here, we'll cover the most common using the Prometheus Operator.

**Using the Prometheus Operator (Recommended):**

The Prometheus Operator simplifies the management of Prometheus in Kubernetes.

1.  **Install the Prometheus Operator:**
    * The most common way to install the Prometheus Operator is using Helm.  First, add the kube-prometheus repository:

        ```bash
        helm repo add prometheus-community [https://prometheus-community.github.io/helm](https://prometheus-community.github.io/helm)
        helm repo update
        ```

    * Then, install the `kube-prometheus-stack` chart, which includes the Prometheus Operator and related components:

        ```bash
        helm install prometheus prometheus-community/kube-prometheus-stack
        ```
    * To install in a specific namespace
         ```bash
         helm install prometheus prometheus-community/kube-prometheus-stack -n monitoring --create-namespace
         ```

2.  **Verify the Installation:**

    * Check that the Prometheus pods are running:

        ```bash
        kubectl get pods -n monitoring #Use the namespace where prometheus is installed.
        ```

### Configuration

* When using the Prometheus Operator, you configure Prometheus through Kubernetes Custom Resources Definitions (CRDs).  The `kube-prometheus-stack` Helm chart provides a default configuration that is a good starting point.  You can customize this configuration by modifying the Helm chart values or by creating/modifying the CRDs.
* Key CRDs include `Prometheus`, `ServiceMonitor`, and `PodMonitor`.
* See the documentation for the [Prometheus Operator](https://prometheus-operator.dev/) and the [kube-prometheus-stack](https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack) for detailed configuration options.

### Running Prometheus

* The Prometheus Operator will automatically deploy and manage Prometheus instances based on the configurations you provide.
* Once the pods are running, you can access the Prometheus web UI via a Kubernetes Service.  The `kube-prometheus-stack` chart usually creates a Service named `prometheus-operated`.
* To access it, you might need to use port forwarding:
     ```bash
     kubectl port-forward svc/prometheus-operated 9090:9090 -n monitoring
     ```
    Then, open your browser to `http://localhost:9090`

### Persistent Storage in Kubernetes

To enable persistent storage in Kubernetes when using the Prometheus Operator, you need to configure the `storage` section in the `Prometheus` CRD.  This typically involves specifying a `volumeClaimTemplate`.

* **Example (using `kube-prometheus-stack`):**
    * You can add this to the values.yaml file, or use the `--set` flag with helm install/upgrade
    ```yaml
    prometheus:
      prometheusSpec:
        storageSpec:
          volumeClaimTemplate:
            spec:
              accessModes:
                - ReadWriteOnce
              resources:
                requests:
                  storage: 10Gi # Adjust the size as needed
              storageClassName: "your-storage-class-name" # Replace with your StorageClass
    ```
    * Replace `"your-storage-class-name"` with the name of your `StorageClass` in Kubernetes.
    * Adjust the `storage` size as needed.
* This will create a PersistentVolumeClaim (PVC) for Prometheus to store its data.  The `StorageClass` will provision the underlying PersistentVolume (PV).
* If you are not using the helm chart, you would apply the yaml directly, using `kubectl apply -f your-prometheus-crds.yaml`

## Persistent Storage

Prometheus stores its data in a local time-series database.  For production deployments, it's crucial to use persistent storage to prevent data loss in case of server restarts or container failures.

* **Local Storage:** Prometheus, by default, stores data in a local directory.
* **Persistent Volumes:** In cloud environments or with container orchestration tools like Kubernetes, you'll typically use persistent volumes to ensure data durability.  This involves allocating storage that outlives the Prometheus instance.
* **Configuration:** The specific configuration for persistent storage depends on your deployment environment:
    * **Service:** You would specify a directory on a persistent disk.
    * **Docker:** Use Docker volumes (named volumes or bind mounts) to map a directory on the host to the container's data directory.
    * **Kubernetes:** Use PersistentVolumes and PersistentVolumeClaims, often managed by a StorageClass.

## Data Retention

Prometheus provides two main parameters to control how long it stores data:

### Retention Time

* **Parameter:** `--storage.tsdb.retention.time`
* **Description:** Specifies the maximum time to keep samples in storage.  Older data will be deleted.
* **Example:** `--storage.tsdb.retention.time=15d` (keep data for 15 days)
* **Units:** `y` (years), `w` (weeks), `d` (days), `h` (hours), `m` (minutes), `s` (seconds), `ms` (milliseconds).

### Retention Size

* **Parameter:** `--storage.tsdb.retention.size`
* **Description:** Specifies the maximum size of storage blocks to retain.  The oldest data will be removed first.
* **Example:** `--storage.tsdb.retention.size=100GB`
* **Units:** `B`, `KB`, `MB`, `GB`, `TB`, `PB`, `EB`.

**Configuration Examples:**

* **Service:**
    * Add the flags to the `ExecStart` line in the systemd service file:
        ```
        ExecStart=/usr/local/bin/prometheus \
          --config.file /etc/prometheus/prometheus.yml \
          --storage.tsdb.path /var/lib/prometheus/ \
          --storage.tsdb.retention.time=30d \
          --storage.tsdb.retention.size=200GB
        ```
* **Docker Compose:**
    * Add the flags to the `command` section in the `docker-compose.yml` file:
        ```yaml
        command:
          - '--config.file=/etc/prometheus/prometheus.yml'
          - '--storage.tsdb.path=/prometheus'
          - '--storage.tsdb.retention.time=30d'
          - '--storage.tsdb.retention.size=200GB'
        ```
* **Kubernetes:**
    * When using the Prometheus Operator, you would configure this in the `Prometheus` CRD, within the `prometheusSpec`:
        ```yaml
        apiVersion: [monitoring.coreos.com/v1](https://monitoring.coreos.com/v1)
        kind: Prometheus
        metadata:
          name: example
          namespace: monitoring
        spec:
          # ... other configurations ...
          storage:
            volumeClaimTemplate:
              spec:
                # ...
          retentionTime: 30d # Add this
          retentionSize: 200GB # and this
        ```
        or in the helm chart values.yaml
        ```yaml
        prometheus:
          prometheusSpec:
            retentionTime: 30d
            retentionSize: 200GB
        ```

## Important Considerations

* **Storage Class:** When using persistent volumes in Kubernetes, ensure your `StorageClass` is configured correctly.
* **Resource Allocation:** Allocate sufficient CPU and memory to the Prometheus instance, especially when dealing with high volumes of metrics.
* **Backup:** Implement a backup strategy for your Prometheus data, even with persistent storage, to protect against data loss.
* **Monitoring Prometheus:** Monitor the health and performance of your Prometheus instance itself.  Prometheus exports metrics about its own operation.
* **Security:** Secure your Prometheus instance to prevent unauthorized access to your monitoring data.  Consider using authentication and authorization.
* **Data Volume:** Carefully consider your data retention settings (`retention.time` and `retention.size`) in relation to your storage capacity.  Prometheus can consume significant storage, especially with high metric volumes and long retention periods.
* **Compaction:** Prometheus compacts data over time to improve query performance.  This process can temporarily increase disk usage.
* **WAL (Write-Ahead Log):** Prometheus uses a Write-Ahead Log (WAL) to ensure data durability.  The WAL can consume additional disk space, especially during periods of high ingestion.
* **Sizing:** Use the formula `needed_disk_space = retention_time_seconds * ingested_samples_per_second * bytes_per_sample` to estimate storage requirements.  A rough estimate of bytes per sample is 1-2 bytes.

## Troubleshooting

* **Check Logs:** Check the Prometheus logs for errors.
    * **Service:** Use `journalctl -u prometheus`
    * **Docker:** Use `docker logs <container_name>`
    * **Kubernetes:** Use `kubectl logs <pod_name> -n <namespace>`
* **Configuration Issues:** Verify that your `prometheus.yml` file is correctly configured.  Use the `promtool` command-line utility to check the configuration: `promtool check config prometheus.yml`
* **Permissions:** Ensure that Prometheus has the necessary permissions to read its configuration files and write data to its storage directory.
* **Network Connectivity:** Verify that Prometheus can reach its scrape targets.  Check firewall rules and network configuration.
* **Storage Issues:**
    * **Service:** Check disk space and file system errors.
    * **Docker:** Check Docker volume configuration and disk space.
    * **Kubernetes:** Check the status of your PersistentVolumeClaims and PersistentVolumes.  Ensure that the storage class is working correctly.
* **Prometheus Status:** Check the Prometheus web UI (usually at port `9090`) to see the current status of Prometheus, including scrape targets, alerts, and errors.
* **Kubernetes Events:** If Prometheus is running in Kubernetes, use `kubectl describe pod <prometheus_pod_name> -n <namespace>` to check for any events related to the pod, such as issues with mounting volumes or scheduling.
