To install the Prometheus CloudWatch Exporter on Ubuntu, follow these steps:

---


install java first:

   ```bash
    sudo apt update
    sudo apt install openjdk-17-jre -y
   ```

### 📦 Step 1: Download the CloudWatch Exporter

1. **Create a directory for the exporter:**

   ```bash
   sudo mkdir -p /opt/cloudwatch_exporter
   cd /opt/cloudwatch_exporter
   ```

2. **Download the latest CloudWatch Exporter JAR file:**

   Visit the [Prometheus CloudWatch Exporter releases page](https://github.com/prometheus/cloudwatch_exporter/releases) and download the latest `.jar` file. For example:

   ```bash
   sudo wget https://github.com/prometheus/cloudwatch_exporter/releases/download/v0.16.0/cloudwatch_exporter-0.16.0.jar -O cloudwatch_exporter.jar
   ```

---

### ⚙️ Step 2: Configure AWS Credentials

The CloudWatch Exporter requires AWS credentials to access CloudWatch metrics.

1. **Install the AWS CLI (if not already installed):**

   ```bash
   sudo apt update
   sudo apt install awscli
   ```

2. **Configure AWS credentials:**

   ```bash
   aws configure
   ```

   You'll be prompted to enter your AWS Access Key ID, Secret Access Key, default region, and output format.

   Alternatively, you can set environment variables or use an IAM role if running on an AWS EC2 instance with appropriate permissions.

---

### 🛠️ Step 3: Create the Configuration File

1. **Create a configuration file named `config.yml`:**

   ```bash
   sudo nano /opt/cloudwatch_exporter/config.yml
   ```

2. **Add the following content to the file:**

   ```yaml
   region: us-east-1
   metrics:
     - aws_namespace: AWS/EC2
       aws_metric_name: CPUUtilization
       dimensions: [InstanceId]
       statistics: [Average]
       period_seconds: 300
   ```



Replace `us-east-1` with your AWS region and adjust the metrics as needed.

---

### 🚀 Step 4: Create a Systemd Service

1. **Create a systemd service file:**

   ```bash
   sudo nano /etc/systemd/system/cloudwatch_exporter.service
   ```

2. **Add the following content:**

   ```ini
   [Unit]
   Description=Prometheus CloudWatch Exporter
   After=network.target

   [Service]
   User=nobody
   ExecStart=/usr/bin/java -jar /opt/cloudwatch_exporter/cloudwatch_exporter.jar 9106 /opt/cloudwatch_exporter/config.yml
   SuccessExitStatus=143

   [Install]
   WantedBy=multi-user.target
   ```



---

### 🔄 Step 5: Start and Enable the Service

1. **Reload systemd to recognize the new service:**

   ```bash
   sudo systemctl daemon-reload
   ```

2. **Start the CloudWatch Exporter service:**

   ```bash
   sudo systemctl start cloudwatch_exporter
   ```

3. **Enable the service to start on boot:**

   ```bash
   sudo systemctl enable cloudwatch_exporter
   ```

4. **Check the status of the service:**

   ```bash
   sudo systemctl status cloudwatch_exporter
   ```

---

### 📈 Step 6: Configure Prometheus to Scrape Metrics

1. **Edit the Prometheus configuration file:**

   ```bash
   sudo nano /etc/prometheus/prometheus.yml
   ```

2. **Add a new job under `scrape_configs`:**

   ```yaml
   scrape_configs:
     - job_name: 'cloudwatch_exporter'
       static_configs:
         - targets: ['localhost:9106']
   ```



3. **Restart Prometheus to apply the changes:**

   ```bash
   sudo systemctl restart prometheus
   ```

---

You can now access the CloudWatch Exporter's metrics at `http://localhost:9106/metrics`. Prometheus will scrape these metrics and store them according to your configuration.([Idevopz][1], [AWS Documentation][2])


[1]: https://www.idevopz.com/step-by-step-guide-to-install-prometheus-nodeexporter-grafana-on-a-ubuntu-20-04/?utm_source=chatgpt.com "Step-by-Step guide to install Prometheus + NodeExporter + Grafana ..."
[2]: https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Agent-PrometheusEC2.html?utm_source=chatgpt.com "Set up and configure Prometheus metrics collection on Amazon EC2 ..."
