My cluster is running Kubernetes v1.32.2 on Debian 12 nodes with containerd runtime. Based on this, here is the **updated step-by-step process** to install the **kube-prometheus-stack** Helm chart on your cluster.

---

# Updated Step-by-Step Installation of kube-prometheus-stack on Your Cluster

---

## 1. **Prerequisites Check**

- Kubernetes version: **v1.32.2** (your cluster nodes show this, which is compatible)
- Helm installed (version 3+)
- `kubectl` configured to access your cluster (you are already running `kubectl get nodes` successfully)
- Namespace creation permission

---

## 2. **Add Prometheus Community Helm Repository**

Run these commands to add and update the Helm repo:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
```

---

## 3. **Create Namespace for Monitoring**

Create a dedicated namespace to isolate monitoring components:

```bash
kubectl create namespace kube-prometheus-stack
```

---

## 4. **Install kube-prometheus-stack Helm Chart**

Install the chart into the namespace you created:

```bash
helm install kube-prom-stack prometheus-community/kube-prometheus-stack --namespace kube-prometheus-stack
```

Since your cluster is fresh (nodes are just a few minutes old), it might take a few minutes for all pods to start.

---

## 5. **Verify the Installation**

Check the status of all pods in the `kube-prometheus-stack` namespace:

```bash
kubectl get pods -n kube-prometheus-stack -l "release=kube-prom-stack"
```

You should see pods for Prometheus, Grafana, Alertmanager, node-exporter, and kube-state-metrics. Wait until all pods show `STATUS=Running` and `READY` is `1/1` or appropriate.

---

## 6. **Access Grafana Dashboard**

Forward the Grafana service port to your local machine:

```bash
kubectl port-forward -n kube-prometheus-stack svc/kube-prom-stack-grafana 3000:80
```

Then open your browser and visit:

```
http://localhost:3000
```

Default login credentials are usually:

- Username: `admin`
- Password: `prom-operator`

(If you want to confirm or change this, you can check or override in Helm values.)

---

## 7. **Customize Installation (Optional)**

If you want to customize the installation (e.g., change resource limits, enable/disable components), create a `values.yaml` file with your desired settings.

Example to disable node-exporter if you don’t want it:

```yaml
nodeExporter:
  enabled: false
```

Then install or upgrade with:

```bash
helm upgrade --install kube-prom-stack prometheus-community/kube-prometheus-stack -n kube-prometheus-stack -f values.yaml
```

---

## 8. **Uninstalling the Stack**

To remove the stack when no longer needed:

```bash
helm uninstall kube-prom-stack -n kube-prometheus-stack
```

To clean up CRDs manually (optional):

```bash
kubectl delete crd alertmanagerconfigs.monitoring.coreos.com alertmanagers.monitoring.coreos.com podmonitors.monitoring.coreos.com probes.monitoring.coreos.com prometheusagents.monitoring.coreos.com prometheuses.monitoring.coreos.com prometheusrules.monitoring.coreos.com scrapeconfigs.monitoring.coreos.com servicemonitors.monitoring.coreos.com thanosrulers.monitoring.coreos.com
```

---

## 9. **Additional Notes for Your Environment**

- Your cluster uses **containerd** runtime, which is fully compatible with kube-prometheus-stack.
- Kubernetes v1.32.2 is recent and supported.
- Debian 12 nodes are stable and suitable for production-grade monitoring.
- If you have network policies or firewalls, ensure traffic is allowed between monitoring components.
- You may want to monitor your cluster nodes, pods, and control plane metrics out of the box with this stack.

---

# Summary of Commands for Your Setup

```bash
# Add Helm repo
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Create namespace
kubectl create namespace kube-prometheus-stack

# Install kube-prometheus-stack
helm install kube-prom-stack prometheus-community/kube-prometheus-stack --namespace kube-prometheus-stack

# Check pods status
kubectl get pods -n kube-prometheus-stack -l "release=kube-prom-stack"

# Port-forward Grafana to localhost
kubectl port-forward -n kube-prometheus-stack svc/kube-prom-stack-grafana 3000:80

# Open http://localhost:3000 in browser and login with admin/prom-operator

# Optional: uninstall
helm uninstall kube-prom-stack -n kube-prometheus-stack

# Optional: delete CRDs
kubectl delete crd alertmanagerconfigs.monitoring.coreos.com alertmanagers.monitoring.coreos.com podmonitors.monitoring.coreos.com probes.monitoring.coreos.com prometheusagents.monitoring.coreos.com prometheuses.monitoring.coreos.com prometheusrules.monitoring.coreos.com scrapeconfigs.monitoring.coreos.com servicemonitors.monitoring.coreos.com thanosrulers.monitoring.coreos.com
```

---


Below is a **custom `values.yaml`** tailored for your Kubernetes cluster running v1.32.2 on Debian 12 with containerd. This configuration balances resource usage and enables essential components for monitoring, with some sensible defaults and comments to guide you.

---

## Custom `values.yaml` for kube-prometheus-stack

```yaml
# values.yaml for kube-prometheus-stack

# Namespace where the stack is installed (set via Helm CLI, not here)
# namespace: kube-prometheus-stack

# Enable or disable components
kubeStateMetrics:
  enabled: true

nodeExporter:
  enabled: true
  # Reduce resource requests/limits for node-exporter if your nodes are small
  resources:
    requests:
      cpu: 50m
      memory: 50Mi
    limits:
      cpu: 100m
      memory: 100Mi

grafana:
  image:
    repository: aniketxshinde/serversage  # Replace with your custom image repository
    tag: latest                                  # Replace with your custom image tag
    pullPolicy: IfNotPresent
  enabled: true
  adminUser: admin
  adminPassword: prom-operator  # Change this password for production use!
  service:
    type: ClusterIP
  ingress:
    enabled: false  # Enable and configure ingress if you want external access
  resources:
    requests:
      cpu: 100m
      memory: 200Mi
    limits:
      cpu: 200m
      memory: 400Mi
  persistence:
    enabled: true  # Enable or Disable if you want to persist dashboards/configs
    storageClassName: ""           # Use your cluster's default StorageClass or specify one
    accessModes:
      - ReadWriteOnce
    size: 10Gi                    # Adjust size as needed

prometheus:
  prometheusSpec:
    # Storage settings - use persistent volume for production
    storageSpec:
      volumeClaimTemplate:
        spec:
          accessModes: ["ReadWriteOnce"]
          resources:
            requests:
              storage: 10Gi
          storageClassName: ""
    resources:
      requests:
        cpu: 200m
        memory: 400Mi
      limits:
        cpu: 500m
        memory: 1Gi
    # Retention period for metrics data
    retention: 30d
    # Enable serviceMonitorSelectorNilUsesHelmValues to monitor all ServiceMonitors by default
    serviceMonitorSelectorNilUsesHelmValues: false

alertmanager:
  alertmanagerSpec:
    resources:
      requests:
        cpu: 100m
        memory: 200Mi
      limits:
        cpu: 200m
        memory: 400Mi
    # Storage for alerts
    storage:
      volumeClaimTemplate:
        spec:
          accessModes: ["ReadWriteOnce"]
          resources:
            requests:
              storage: 2Gi

# Prometheus Operator settings
prometheusOperator:
  admissionWebhooks:
    enabled: true
    patch:
      enabled: true
  resources:
    requests:
      cpu: 100m
      memory: 200Mi
    limits:
      cpu: 200m
      memory: 400Mi

# RBAC settings (usually enabled)
rbac:
  create: true

# Service accounts
serviceAccounts:
  prometheus:
    create: true
  alertmanager:
    create: true
  grafana:
    create: true
  nodeExporter:
    create: true
  kubeStateMetrics:
    create: true

# Additional scrape configs or extra service monitors can be added here
# extraScrapeConfigs: []
# additionalServiceMonitors: []

# Disable components you don't want by setting enabled: false above

```
---
## Additional Notes

- **StorageClassName:** If you have a specific StorageClass in your cluster (e.g., `standard`, `fast`, `gp2`), specify it; otherwise, leave it blank to use the default.
- **AccessModes:** `ReadWriteOnce` is typical for block storage.
- **PVC Size:** Adjust `size` based on your expected data retention and cluster capacity.
- After updating `values.yaml`, upgrade your Helm release:

- This mounts a PVC at Grafana’s data directory (/var/lib/grafana), persisting dashboards, user data, and configuration.

- Verify PVCs are created and bound:

```bash
kubectl get pvc -n kube-prometheus-stack
```


---

## How to Use This `values.yaml`

1. Save the above content into a file named `values.yaml` in your working directory.

2. Install or upgrade your Helm release with:

```bash
helm upgrade --install kube-prom-stack prometheus-community/kube-prometheus-stack \
  --namespace kube-prometheus-stack --create-namespace -f values.yaml
```

3. Monitor the pods and services as usual:

```bash
kubectl get pods -n kube-prometheus-stack -l "release=kube-prom-stack"
```

---

## Notes

- **Passwords:** Change `grafana.adminPassword` before deploying in production.
- **Persistence:** Persistence is disabled for Grafana by default to keep it simple; enable it if you want to save dashboards across restarts.
- **Storage:** Prometheus and Alertmanager use persistent volumes (PVCs) with 10Gi and 2Gi respectively. Adjust sizes as per your cluster storage capacity.
- **Resources:** Resource requests and limits are conservative but can be tuned based on your cluster node sizes.
- **Ingress:** Disabled by default. If you want to expose Grafana externally, configure ingress or load balancer accordingly.
- **Retention:** Prometheus retains metrics for 15 days, adjustable in `retention`.

---


---

