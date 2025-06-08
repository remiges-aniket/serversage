# Prometheus Installation Guide

This document provides instructions on how to install Prometheus using different methods: as a service, with Docker Compose, and on a Kubernetes cluster. It also covers configuring persistent storage and setting up data retention policies based on time and size.

## 1. Installing Prometheus as a Service (Ubuntu)

This section outlines the steps to install Prometheus as a service on an Ubuntu system.

### Prerequisites:

* A running Ubuntu system (commands tested on Ubuntu 22.04, but might be similar on other versions).
* `sudo` privileges.

### Installation Steps:

1.  **Update System Packages:**
    ```bash
    sudo apt update
    sudo apt upgrade -y
    ```

2.  **Create a System User for Prometheus:**
    ```bash
    sudo groupadd --system prometheus
    sudo useradd --system --gid prometheus prometheus
    ```

3.  **Create Directories for Prometheus:**
    ```bash
    sudo mkdir /etc/prometheus
    sudo mkdir /var/lib/prometheus
    sudo chown prometheus:prometheus /etc/prometheus
    sudo chown prometheus:prometheus /var/lib/prometheus
    ```

4.  **Download Prometheus:**
    Visit the [Prometheus download page](https://prometheus.io/download/) and get the link for the latest stable release for Linux. Then, download it using `wget`. For example:
    ```bash
    wget [https://github.com/prometheus/prometheus/releases/download/vX.Y.Z/prometheus-X.Y.Z.linux-amd64.tar.gz](https://github.com/prometheus/prometheus/releases/download/vX.Y.Z/prometheus-X.Y.Z.linux-amd64.tar.gz)
    ```
    Replace `vX.Y.Z` with the actual version number.

5.  **Extract the Files:**
    ```bash
    tar xvf prometheus-X.Y.Z.linux-amd64.tar.gz
    cd prometheus-X.Y.Z.linux-amd64
    ```

6.  **Move Binary and Configuration Files:**
    ```bash
    sudo mv prometheus promtool /usr/local/bin/
    sudo chown prometheus:prometheus /usr/local/bin/prometheus /usr/local/bin/promtool
    sudo mv consoles console_libraries /etc/prometheus/
    sudo mv prometheus.yml /etc/prometheus/prometheus.yml
    sudo chown -R prometheus:prometheus /etc/prometheus/consoles /etc/prometheus/console_libraries
    ```

7.  **Create Prometheus Systemd Service File:**
    Create a file named `/etc/systemd/system/prometheus.service` with the following content:
    ```ini
    [Unit]
    Description=Prometheus Monitoring System
    Wants=network-online.target
    After=network-online.target

    [Service]
    User=prometheus
    Group=prometheus
    Type=simple
    ExecStart=/usr/local/bin/prometheus \
        --config.file=/etc/prometheus/prometheus.yml \
        --storage.tsdb.path=/var/lib/prometheus/ \
        --web.listen-address=0.0.0.0:9090
    Restart=on-failure

    [Install]
    WantedBy=multi-user.target
    ```

8.  **Reload Systemd:**
    ```bash
    sudo systemctl daemon-reload
    ```

9.  **Start and Enable Prometheus Service:**
    ```bash
    sudo systemctl start prometheus
    sudo systemctl enable prometheus
    ```

10. **Check Prometheus Status:**
    ```bash
    sudo systemctl status prometheus
    ```

11. **Access Prometheus Web Interface:**
    Open your web browser and navigate to `http://your_server_ip:9090`.

### Persistent Storage:

The `--storage.tsdb.path=/var/lib/prometheus/` line in the service file configures the directory where Prometheus will store its data. This directory will persist across reboots of the service.

### Data Retention Configuration:

You can configure data retention by adding the following flags to the `ExecStart` line in the Prometheus systemd service file:

* **Time-based retention:** Use the `--storage.tsdb.retention.time` flag followed by a duration (e.g., `30d` for 30 days, `2h` for 2 hours, `1y` for 1 year).
* **Size-based retention (EXPERIMENTAL):** Use the `--storage.tsdb.retention.size` flag followed by a size (e.g., `10GB`). **Note:** This feature is experimental and might have limitations.

**Example with 30-day retention:**

```ini
ExecStart=/usr/local/bin/prometheus \
    --config.file=/etc/prometheus/prometheus.yml \
    --storage.tsdb.path=/var/lib/prometheus/ \
    --storage.tsdb.retention.time=30d \
    --web.listen-address=0.0.0.0:9090
```

**After modifying the service file, remember to reload systemd and restart the Prometheus service:**
```ini
sudo systemctl daemon-reload
sudo systemctl restart prometheus
```
