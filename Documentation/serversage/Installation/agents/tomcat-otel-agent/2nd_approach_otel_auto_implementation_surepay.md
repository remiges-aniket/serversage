Here's the complete and corrected documentation for OpenTelemetry auto-instrumentation with Tomcat:

---

# **OpenTelemetry Auto-Instrumentation for Tomcat**

### **1. Download Java Agent**
```bash
wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.29.0/opentelemetry-javaagent.jar
mv opentelemetry-javaagent.jar /opt/otel/
```

---

### **2. Configure Tomcat (`setenv.sh`)**
Path: `tomcat/bin/setenv.sh`
```sh
#!/bin/sh
# OpenTelemetry Configuration
export OTEL_SERVICE_NAME=SurePay
export OTEL_RESOURCE_ATTRIBUTES=service.namespace=Finance,deployment.environment=prod

# Java Agent Configuration
CATALINA_OPTS="$CATALINA_OPTS -Dotel.service.name=$OTEL_SERVICE_NAME"
CATALINA_OPTS="$CATALINA_OPTS -javaagent:/opt/otel/opentelemetry-javaagent.jar"
```

---

### **3. OpenTelemetry Collector Configuration**
Create `otel-config.yaml`:
```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

exporters:
  prometheus:
    endpoint: "0.0.0.0:9464"
  
  logging:
    loglevel: debug

  jaeger:
    endpoint: "jaeger:14250"
    tls:
      insecure: true

processors:
  batch:

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [jaeger, logging]
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [prometheus, logging]
    logs:
      receivers: [otlp]
      processors: [batch]
      exporters: [logging]
```

---

### **4. Start Components**
#### **Start OpenTelemetry Collector**
```bash
docker run -d \
  -p 4317:4317 \
  -p 4318:4318 \
  -p 9464:9464 \
  -v $(pwd)/otel-config.yaml:/etc/otelcol-contrib/config.yaml \
  otel/opentelemetry-collector-contrib:0.82.0
```

#### **Start Tomcat**
```bash
cd tomcat/bin
./startup.sh
```

---

### **5. Verification**
1. Check agent attachment:
   ```bash
   ps aux | grep java | grep opentelemetry-javaagent
   ```

2. Verify Prometheus metrics:
   ```bash
   curl http://localhost:9464/metrics
   ```

3. Check collector logs:
   ```bash
   docker logs 
   ```

---

### **Key Configuration Notes**
1. **Agent Path**: Ensure `/opt/otel/` exists and contains the downloaded JAR
2. **Protocol Support**: No need to give endpoint of otel if both are in same system it will send it on 0.0.0.0 automatically, if required use gRpc on 4317
3. **Resource Attributes**: Add custom attributes for better observability
#### Advanced configuration
```sh
export OTEL_RESOURCE_ATTRIBUTES=deployment.environment=production
export OTEL_METRICS_EXPORT_INTERVAL=60000
export OTEL_PROPAGATORS=tracecontext,baggage
```
4. **Port Mapping**:
   - 4317: OTLP gRPC
   - 4318: OTLP HTTP
   - 9464: Prometheus metrics endpoint

---

### **Architecture Flow**
```
Tomcat App → OpenTelemetry Java Agent → OTLP Collector → Prometheus/Jaeger
```

---

This configuration provides full-stack observability with metrics, traces, and logs while maintaining compatibility with Java 8.

Citations:
[1] https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.29.0/opentelemetry-javaagent.jar

---
