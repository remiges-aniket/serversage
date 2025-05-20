Here's a comprehensive **Standard Operating Procedure (S.O.P.)** to install **Prometheus**, **Node Exporter**, and **Grafana** on a **Linux server (Ubuntu-based)**. This includes installation and configuration steps.

---

# 📘 S.O.P. for Installing Prometheus, Node Exporter, and Grafana

---

## 🧰 Prerequisites

* A Linux server (Ubuntu 20.04+ or similar)
* Root or sudo privileges
* Ports:

  * Prometheus: `9090`
  * Node Exporter: `9100`
  * Grafana: `3000`

---

## 🔧 Step 1: Create Prometheus User

```bash
sudo useradd --no-create-home --shell /bin/false prometheus
```

Create necessary directories:

```bash
sudo mkdir /etc/prometheus
sudo mkdir /var/lib/prometheus
```

Set permissions:

```bash
sudo chown prometheus:prometheus /etc/prometheus /var/lib/prometheus
```

---

## 📦 Step 2: Install Prometheus

1. Download and extract:

```bash
curl -LO https://github.com/prometheus/prometheus/releases/download/v3.4.0/prometheus-3.4.0.linux-amd64.tar.gz
tar xvf prometheus-3.4.0.linux-amd64.tar.gz
cd prometheus-3.4.0.linux-amd64
```

2. Move binaries and set permissions:

```bash
sudo cp prometheus /usr/local/bin/
sudo cp promtool /usr/local/bin/
sudo chown prometheus:prometheus /usr/local/bin/prometheus /usr/local/bin/promtool
```

3. Move configuration:

```bash
sudo cp -r consoles/ console_libraries/ /etc/prometheus/
sudo cp prometheus.yml /etc/prometheus/
sudo chown -R prometheus:prometheus /etc/prometheus
```

---

## ⚙️ Step 3: Configure Prometheus

Edit the config file:

```bash
sudo nano /etc/prometheus/prometheus.yml
```

**Example Config:**

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: "prometheus"
    static_configs:
      - targets: ["localhost:9090"]

  - job_name: "node_exporter"
    static_configs:
      - targets: ["localhost:9100"]
```

---

## 🔌 Step 4: Create Prometheus Systemd Service

```bash
sudo tee /etc/systemd/system/prometheus.service > /dev/null <<EOF
[Unit]
Description=Prometheus
Wants=network-online.target
After=network-online.target

[Service]
User=prometheus
# ExecStart=/usr/local/bin/prometheus \
#   --config.file=/etc/prometheus/prometheus.yml \
#   --storage.tsdb.path=/var/lib/prometheus/ \
#   --storage.tsdb.retention.time=30d \
#   --web.console.templates=/etc/prometheus/consoles \
#   --web.console.libraries=/etc/prometheus/console_libraries


ExecStart=/usr/local/bin/prometheus  \
    --config.file=/etc/prometheus/prometheus.yml  \
    --storage.tsdb.path=/var/serversage/data/prometheus-tsdb  \
    --storage.tsdb.retention.time=30d  \
    --web.console.templates=/var/serversage/data/prometheus/consoles  \
    --web.console.libraries=/var/serversage/data/prometheus/console_libraries


[Install]
WantedBy=multi-user.target
EOF
```

Start and enable:

```bash
sudo systemctl daemon-reexec
sudo systemctl daemon-reload
sudo systemctl enable prometheus
sudo systemctl start prometheus
```

---

## 🖥️ Step 5: Install Node Exporter

1. Download and extract:

```bash
curl -LO https://github.com/prometheus/node_exporter/releases/download/v1.9.1/node_exporter-1.9.1.linux-amd64.tar.gz
tar xvf node_exporter-1.9.1.linux-amd64.tar.gz
cd node_exporter-1.9.1.linux-amd64
```

2. Move binary:

```bash
sudo cp node_exporter /usr/local/bin/
sudo useradd --no-create-home --shell /bin/false node_exporter
sudo chown node_exporter:node_exporter /usr/local/bin/node_exporter
```

3. Create systemd service:

```bash
sudo tee /etc/systemd/system/node_exporter.service > /dev/null <<EOF
[Unit]
Description=Node Exporter
Wants=network-online.target
After=network-online.target

[Service]
User=node_exporter
ExecStart=/usr/local/bin/node_exporter

[Install]
WantedBy=default.target
EOF
```

Start and enable:

```bash
sudo systemctl daemon-reload
sudo systemctl enable node_exporter
sudo systemctl start node_exporter
```

---

## 📊 Step 6: Install Grafana

1. Add repo and install:

```bash
sudo apt-get install -y software-properties-common
sudo add-apt-repository "deb https://packages.grafana.com/oss/deb stable main"
wget -q -O - https://packages.grafana.com/gpg.key | sudo apt-key add -
sudo apt-get update
sudo apt-get install grafana -y
```

2. Start and enable Grafana:

```bash
sudo systemctl enable grafana-server
sudo systemctl start grafana-server
```


port allow on machine:

ufw allow 9100/tcp

---

## 🧩 Step 7: Configure Grafana

1. Open Grafana at [http://localhost:3000](http://localhost:3000)
2. Default login: `admin / admin`
3. Add **Prometheus Data Source**:

   * URL: `http://localhost:9090`
   * Save and test
4. Import dashboards or create new panels to monitor `node_exporter` metrics like:

   * CPU usage: `rate(node_cpu_seconds_total[1m])`
   * Memory: `node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes`
   * Disk: `node_filesystem_avail_bytes`

---

## ✅ Verification Checklist

* [ ] Prometheus web UI accessible at `http://localhost:9090`
* [ ] Node Exporter metrics visible in Prometheus targets
* [ ] Grafana running at `http://localhost:3000`
* [ ] Prometheus configured as Grafana data source
* [ ] Dashboards/panels showing metrics

---

Let me know if you want the same setup using **Docker** or **Kubernetes**.
