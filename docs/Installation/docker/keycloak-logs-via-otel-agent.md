### **SOP: Sending Keycloak Logs to OpenTelemetry (OTEL)**

This SOP outlines the steps to send Keycloak logs to OpenTelemetry using the OpenTelemetry Java Agent and Collector, based on the implementation in the provided GitHub repository.

---

### **1. Prerequisites**
Ensure the following are installed:
- **Docker** (version 20.10+)
- **Docker Compose** (version 1.29+)
- At least **4 GB of free RAM** for the services.

---

### **2. Download OpenTelemetry Java Agent**
Download the OpenTelemetry Java Agent:
```bash
wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

mkdir -p ./data/etc && mv opentelemetry-javaagent.jar ./data/etc/
```

---

### **3. Configure Keycloak**
Modify your `docker-compose.yaml` file to include the OpenTelemetry Java Agent and logging configuration:

#### **Keycloak Service Configuration**
```yaml
keycloak:
  image: bitnami/keycloak:22.0.4
  environment:
    - KEYCLOAK_ADMIN=admin
    - KEYCLOAK_ADMIN_PASSWORD=admin
    - KC_LOG=console
    - KC_LOG_CONSOLE_COLOR=true
    - KC_LOG_CONSOLE_INCLUDE_TRACE=true
    - KC_LOG_CONSOLE_LEVEL=debug
    - KC_LOG_CONSOLE_OUTPUT=json
    - OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318
    - JAVA_TOOL_OPTIONS=-javaagent:/opt/opentelemetry-javaagent.jar -Dotel.resource.attributes=service.name=keycloak
  volumes:
    - ./data/etc/opentelemetry-javaagent.jar:/opt/opentelemetry-javaagent.jar
  ports:
    - "8080:8080"
    - "9000:9000"
```

---

### **4. Configure OpenTelemetry Collector**
Create an `otel-collector-config.yaml` file to receive logs from Keycloak:

#### **OpenTelemetry Collector Configuration**
```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: "0.0.0.0:4317"
      http:
        endpoint: "0.0.0.0:4318"

processors:
  batch:

exporters:
  debug:
    verbosity: detailed
  otlphttp/logs:
    endpoint: "http://loki:3100/otlp" # you can add kafka or elasticsearch here where you want to send the logs refer respective docs
    tls:
      insecure: true
  # elasticsearch:
  #   index: "keycloak-logs"
  #   endpoint: ["http://elasticsearch:9200"]

service:
  pipelines:
    logs:
      receivers: [otlp]
      processors: [batch]
      exporters: [debug,otlphttp/logs]
```

Place this file in `./data/etc/otel-collector-config.yaml`.

---

### **5. Add OpenTelemetry Collector to Docker Compose**
Add the OpenTelemetry Collector service to your `docker-compose.yaml` file:

```yaml
otel-collector:
  image: otel/opentelemetry-collector-contrib:latest
  command:
    - "--config=/etc/otel-collector-config.yaml"
  volumes:
    - ./data/etc/otel-collector-config.yaml:/etc/otel-collector-config.yaml
  ports:
    - "4317:4317"
    - "4318:4318"
```

---

### **6. Start Services**
Run all services using Docker Compose:
```bash
docker-compose up -d
```

---

### **7. Verify Setup**

#### **Check Keycloak Logs in OpenTelemetry Collector**
Ensure logs are being received by the OpenTelemetry Collector:
```bash
docker logs otel-collector | grep keycloak
```

#### **Check Keycloak Metrics in Prometheus**
Access Keycloak metrics via Prometheus:
```bash
curl http://localhost:8080/metrics
```
This should display metrics similar to the example output you provided.

---

### **8. Troubleshooting**

| Issue                          | Cause                                             | Solution                                                                 |
|--------------------------------|--------------------------------------------------|-------------------------------------------------------------------------|
| Logs not reaching Collector     | Incorrect OTLP endpoint in Keycloak             | Verify Keycloak points to `http://otel-collector:4318`                  |
| Missing trace IDs in logs       | Java Agent not properly configured               | Ensure `JAVA_TOOL_OPTIONS` includes `-javaagent` flag                   |
| Collector not receiving logs    | Incorrect Collector configuration               | Verify `otel-collector-config.yaml` for correct receivers and exporters |
| High resource consumption       | Overhead from Java Agent                        | Optimize JVM settings in Keycloak                                       |

---

### **9. Cleanup**
To stop and remove all services, run:
```bash
docker-compose down --volumes --remove-orphans
```

---

### Additional Details/Corrections Added:

1. **Keycloak Metrics Output**:
   The `curl http://localhost:8080/metrics` command should display metrics similar to the provided example output, including event metrics like `keycloak_event_user_total`.

2. **OpenTelemetry Collector Configuration**:
   The `otel-collector-config.yaml` file is configured to receive logs via OTLP and export them to Loki for log analysis.

3. **Troubleshooting Section**:
   Included common issues and solutions for seamless integration.

This SOP ensures that Keycloak logs are properly sent to OpenTelemetry using the OpenTelemetry Java Agent and Collector, and metrics are accessible via Prometheus.

Citations:
[1] https://github.com/remiges-aniket/serversage/tree/main/keycloak_logs_to_otel_kafka

---