Here's the corrected configuration for auto-instrumenting Tomcat apps with OpenTelemetry:

---

### **Step 1: Download Java Agent**
```bash
wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
```

---

### **Step 2: Configure `setenv.sh` (Corrected Version)**
Path: `tomcat/bin/setenv.sh`

```sh
#!/bin/sh
export JRE_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# OpenTelemetry Configuration
export OTEL_SERVICE_NAME=SurePay
export OTEL_TRACES_EXPORTER=otlp
export OTEL_METRICS_EXPORTER=prometheus
export OTEL_LOGS_EXPORTER=logging
export OTEL_EXPORTER_OTLP_ENDPOINT=http://10.250.10.11:4317
export OTEL_EXPORTER_OTLP_PROTOCOL=grpc

# Java Agent Configuration
CATALINA_OPTS="$CATALINA_OPTS -javaagent:/opt/opentelemetry-javaagent.jar"
```

---

### **Key Fixes Made**:
1. **Removed Redundant System Properties**: 
   The OpenTelemetry Java agent automatically reads environment variables. No need to duplicate them as `-D` system properties.

2. **Fixed Configuration Conflicts**:
   - Removed `OTEL_EXPORTER_PROMETHEUS_ENDPOINT` (not needed for basic Prometheus setup)
   - Simplified configuration to use only environment variables

3. **Proper Agent Path**:
   Ensure the path in `-javaagent:` matches your actual agent location (`/opt/` in this example).

---

### **Step 3: Remove Startup.sh Modification**
❌ **Don't modify `startup.sh`** - The `setenv.sh` file is the correct place to configure Tomcat options. Adding the agent to both files would cause duplicate instrumentation.

---

### **Verification**:
1. Check if metrics are available at:
   ```bash
   curl http://localhost:9464/metrics
   ```
2. Validate traces are reaching your OTLP endpoint (e.g., Jaeger/Collector at 10.250.10.11:4317)

---

### **Recommended Additions** (Optional):
```sh
# Advanced configuration
export OTEL_RESOURCE_ATTRIBUTES=deployment.environment=production
export OTEL_METRICS_EXPORT_INTERVAL=60000
export OTEL_PROPAGATORS=tracecontext,baggage
```

---

### **Why This Works**:
- The Java agent auto-discovers environment variables starting with `OTEL_`
- Simplified configuration reduces potential errors
- Proper separation of concerns between Tomcat config and instrumentation

This configuration will automatically collect:
- HTTP metrics and traces
- JVM metrics
- Database call traces
- Log context propagation

Citations:
[1] https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

---