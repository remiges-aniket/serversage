To install and configure the Prometheus PostgreSQL Exporter (`postgres_exporter`) as a systemd service on Ubuntu, follow these steps:

---

## 🧰 Prerequisites

* Ubuntu system with PostgreSQL installed
* Prometheus installed and running
* User with `sudo` privileges([Grafana Labs][1])

---

## 📥 Step 1: Download and Install `postgres_exporter`

1. **Create a directory for the exporter:**

   ```bash
   sudo mkdir -p /opt/postgres_exporter
   cd /opt/postgres_exporter
   ```

2. **Download the latest `postgres_exporter` binary:**

   Visit the [Releases page](https://github.com/prometheus-community/postgres_exporter/releases) to find the latest version. Replace `v0.17.1` with the desired version:

   ```bash
   wget https://github.com/prometheus-community/postgres_exporter/releases/download/v0.17.1/postgres_exporter-0.17.1.linux-amd64.tar.gz
   ```

3. **Extract and move the binary:**

   ```bash
    tar xvf postgres_exporter-0.17.1.linux-amd64.tar.gz
    cd postgres_exporter-0.17.1.linux-amd64
   ```

4. **Set appropriate permissions:**

   ```bash
   sudo chown root:root /usr/local/bin/postgres_exporter
   sudo chmod +x /usr/local/bin/postgres_exporter
   ```

---

## 🔐 Step 2: Configure PostgreSQL User for Exporter

1. **Create a PostgreSQL user for the exporter:**

   Connect to your PostgreSQL instance and run:

   ```sql
   CREATE USER postgres_exporter WITH PASSWORD 'yourpassword';
   ```

2. **Grant necessary permissions:**

   Still within the PostgreSQL prompt:

   ```sql
   GRANT CONNECT ON DATABASE yourdatabase TO postgres_exporter;
   GRANT USAGE ON SCHEMA public TO postgres_exporter;
   GRANT SELECT ON ALL TABLES IN SCHEMA public TO postgres_exporter;
   ```

   Adjust `yourdatabase` and schema as needed.

---

## 📝 Step 3: Set Up Environment Variables

1. **Create an environment file:**

   ```bash
   sudo nano /opt/postgres_exporter/postgres_exporter.env
   ```

2. **Add the following line:**

   ```bash
   DATA_SOURCE_NAME="postgresql://postgres_exporter:yourpassword@localhost:5432/yourdatabase?sslmode=disable"
   ```

   Replace `yourpassword` and `yourdatabase` with your actual PostgreSQL user's password and database name.

---

## 🛠️ Step 4: Create Systemd Service

1. **Create a systemd service file:**

   ```bash
   sudo nano /etc/systemd/system/postgres_exporter.service
   ```

2. **Add the following content:**

   ```ini
   [Unit]
   Description=Prometheus PostgreSQL Exporter
   After=network.target

   [Service]
   User=postgres
   Group=postgres
   EnvironmentFile=/opt/postgres_exporter/postgres_exporter.env
   ExecStart=/usr/local/bin/postgres_exporter
   Restart=always

   [Install]
   WantedBy=multi-user.target
   ```

   Ensure that the `User` and `Group` match the PostgreSQL user.

3. **Reload systemd and start the service:**

   ```bash
   sudo systemctl daemon-reload
   sudo systemctl start postgres_exporter
   sudo systemctl enable postgres_exporter
   ```

4. **Check the service status:**

   ```bash
   sudo systemctl status postgres_exporter
   ```

---

## 📡 Step 5: Configure Prometheus to Scrape Metrics

1. **Edit Prometheus configuration:**

   ```bash
   sudo nano /etc/prometheus/prometheus.yml
   ```

2. **Add a new job under `scrape_configs`:**

   ```yaml
   scrape_configs:
     - job_name: 'postgres_exporter'
       static_configs:
         - targets: ['localhost:9187']
   ```

3. **Restart Prometheus:**

   ```bash
   sudo systemctl restart prometheus
   ```

---

## ✅ Verification

* Access the exporter metrics at [http://localhost:9187/metrics](http://localhost:9187/metrics)
* Check Prometheus targets at [http://localhost:9090/targets](http://localhost:9090/targets) to ensure the `postgres_exporter` is listed and up

---

For more detailed information and advanced configurations, refer to the official documentation:
👉 [Prometheus Community PostgreSQL Exporter](https://github.com/prometheus-community/postgres_exporter)

If you need assistance with setting up Grafana dashboards for PostgreSQL metrics visualization, feel free to ask!

[1]: https://grafana.com/oss/prometheus/exporters/postgres-exporter/?utm_source=chatgpt.com "Postgres exporter - Prometheus OSS - Grafana"
[2]: https://www.howtoforge.com/how-to-monitor-postgresql-with-prometheus-and-grafana/?utm_source=chatgpt.com "How to Monitor PostgreSQL with Prometheus and Grafana on ..."
