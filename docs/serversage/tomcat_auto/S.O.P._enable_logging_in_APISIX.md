Here's a Kubernetes-focused SOP to enable logging from Apache APISIX to Elasticsearch:

---

## **Standard Operating Procedure: APISIX to Elasticsearch Logging (Kubernetes)**

### **1. Deploy Elasticsearch Cluster**
```yaml
# elasticsearch-values.yaml (Helm Chart)
clusterName: "apisix-logs"
nodeGroup: "master"
replicas: 3
persistence:
  enabled: true
  size: 100Gi
```
```bash
helm repo add elastic https://helm.elastic.co
helm install elasticsearch elastic/elasticsearch -f elasticsearch-values.yaml
```
*Creates a 3-node Elasticsearch cluster with persistent storage[7][8]*

---

### **2. Configure APISIX Elasticsearch Logger Plugin**
```yaml
# apisix-plugin-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: apisix-elasticsearch-config
data:
  config.yaml: |
    plugins:
      - elasticsearch-logger
    plugin_attrs:
      elasticsearch-logger:
        endpoint_addr: "http://elasticsearch-master:9200" # Kubernetes Service DNS
        auth:
          username: "${ES_USERNAME}"
          password: "${ES_PASSWORD}"
        ssl_verify: false # Enable for production with valid certs
        batch_max_size: 1000
        buffer_duration: 60
        inactive_timeout: 5
        log_format:
          host: "$host"
          timestamp: "$time_iso8601"
          client_ip: "$remote_addr"
          latency: "$latency"
```

```bash
# Create secret for Elasticsearch credentials
kubectl create secret generic es-credentials \
  --from-literal=ES_USERNAME=elastic \
  --from-literal=ES_PASSWORD=changeme
```

---

### **3. Deploy APISIX with Plugin Configuration**
```yaml
# apisix-deployment-patch.yaml
spec:
  template:
    spec:
      containers:
      - name: apisix
        envFrom:
        - secretRef:
            name: es-credentials
        volumeMounts:
        - name: plugin-config
          mountPath: /usr/local/apisix/conf/config.yaml
          subPath: config.yaml
      volumes:
      - name: plugin-config
        configMap:
          name: apisix-elasticsearch-config
```

```bash
helm upgrade apisix apisix/apisix \
  --set gateway.tls.enabled=true \
  --set etcd.enabled=true \
  -f apisix-deployment-patch.yaml
```

---

### **4. Create Logging Route Rule**
```bash
kubectl exec -it deploy/apisix -- curl -X PUT \
  http://127.0.0.1:9180/apisix/admin/routes/es-logging \
  -H "X-API-KEY: $ADMIN_KEY" \
  -d '{
    "uri": "/*",
    "plugins": {
      "elasticsearch-logger": {
        "field": {
          "index": "apisix-logs",
          "type": "_doc"
        }
      }
    },
    "upstream": {
      "type": "roundrobin",
      "nodes": {
        "your-backend-service:80": 1
      }
    }
  }'
```

---

### **5. Verify Log Ingestion**
```bash
# Check Elasticsearch indices
kubectl exec -it elasticsearch-master-0 -- curl -X GET "http://localhost:9200/_cat/indices?v"

# Sample log entry verification
kubectl exec -it elasticsearch-master-0 -- curl -X GET \
  "http://localhost:9200/apisix-logs/_search?pretty" \
  -u elastic:changeme
```

---

### **6. Configure Kibana Dashboard**
```yaml
# kibana-values.yaml
elasticsearchHosts: "http://elasticsearch-master:9200"
```
```bash
helm install kibana elastic/kibana -f kibana-values.yaml
```
1. Access Kibana via port-forward:
```bash
kubectl port-forward svc/kibana-kibana 5601:5601
```
2. Create index pattern `apisix-logs-*`
3. Build dashboards using APISIX log fields[1]

---

### **Optional Scaling Considerations**
```yaml
# For high-throughput systems
batch_max_size: 5000
buffer_duration: 30
max_retry_count: 5
retry_delay: 3

# Add log processing pipeline
plugins:
  - kafka-logger
  - http-logger
```
*Use Kafka/HTTP logger for buffering before Elasticsearch in large deployments[1][4]*

---

**Key Kubernetes-Specific Notes**:
1. Service Discovery: Use Kubernetes DNS names (`..svc.cluster.local`)
2. Secrets Management: Always use Kubernetes Secrets for credentials
3. Storage: Configure persistent volumes for Elasticsearch data nodes
4. Resource Limits:
```yaml
resources:
  limits:
    cpu: 2
    memory: 4Gi
  requests:
    cpu: 1
    memory: 2Gi
```
5. Network Policies: Restrict traffic between APISIX and Elasticsearch

Troubleshooting Tip: Check APISIX error logs with:
```bash
kubectl logs -l app.kubernetes.io/name=apisix -c apisix --tail=100
```

Citations:
[0] https://apisix.apache.org/docs/apisix/plugins/elasticsearch-logger/#enable-plugin
[1] https://navendu.me/posts/apisix-logs-elk/
[2] https://apisix.apache.org/docs/apisix/plugins/elasticsearch-logger/
[3] https://apisix.apache.org/docs/apisix/3.10/plugins/elasticsearch-logger/
[4] https://medium.com/@varunpalekar/logging-kubernetes-pods-using-elasticsearch-aef9ea988893
[5] https://www.cnblogs.com/oneslide/p/13215933.html
[6] https://portworx.com/elasticsearch-kubernetes/
[7] https://dev.to/adityapratapbh1/logging-and-monitoring-in-kubernetes-53o2
[8] https://medium.com/swlh/running-and-deploying-elasticsearch-on-kubernetes-7effb49780c2

---