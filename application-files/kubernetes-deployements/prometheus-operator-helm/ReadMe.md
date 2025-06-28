
-----

# Step-by-Step Installation of kube-prometheus-stack on Your Cluster

-----

## 1\. **Prerequisites Check**

  - Kubernetes version: **v1.32.2** (your cluster nodes show this, which is compatible).
  - Helm installed (version 3+).
  - `kubectl` configured to access your cluster (you are already running `kubectl get nodes` successfully).
  - Namespace creation permission.

-----

## 2\. **Add Prometheus Community Helm Repository**

Run these commands to add and update the Helm repo:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm search repo prometheus-community/kube-prometheus-stack --versions
```

**Explanation:**

  - `helm search repo prometheus-community/kube-prometheus-stack --versions` is more specific to find versions of the `kube-prometheus-stack` chart itself.

-----

## 3\. **Configure Custom Values (Optional but Recommended)**

It's highly recommended to use a custom `values.yaml` file for production deployments. Let's create `prometheus-values.yaml` with your tailored settings.

Create a file named `prometheus-values.yaml` with the following content:

```yaml
# prometheus-values.yaml for kube-prometheus-stack

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
    repository: aniketxshinde/serversage  # Replace with your custom image repository if needed
    tag: latest                                  # Replace with your custom image tag if needed
    pullPolicy: IfNotPresent
  enabled: true
  adminUser: admin
  adminPassword: prom-operator  # !! CHANGE THIS PASSWORD FOR PRODUCTION USE !!
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
    enabled: true  # Enable persistence for Grafana data (dashboards, users, etc.)
    # storageClassName: "your-storage-class" # Specify your cluster's StorageClass (e.g., standard, gp2). Leave empty "" for default.
    accessModes:
      - ReadWriteOnce
    size: 2Gi                    # Adjust size as needed

prometheus:
  prometheusSpec:
    replicas: 2 # Recommended for high availability
    # Storage settings - use persistent volume for production
    storageSpec:
      volumeClaimTemplate:
        spec:
          accessModes: ["ReadWriteOnce"]
          resources:
            requests:
              storage: 10Gi # Adjust size based on data retention and cluster capacity
    thanos:
      objectStorageConfig:
        name: thanos-objstore-config
        key: objstore.yml
      version: v0.35.1
    resources:
      requests:
        cpu: 200m
        memory: 400Mi
      limits:
        cpu: 500m
        memory: 1Gi
    # Retention period for metrics data
    retention: 30d # Data retention for Prometheus
    # Enable serviceMonitorSelectorNilUsesHelmValues to monitor all ServiceMonitors by default
    serviceMonitorSelectorNilUsesHelmValues: false # Set to true to automatically select ServiceMonitors managed by Helm

alertmanager:
  alertmanagerSpec:
    replicas: 1 # You might want 2 for HA in production
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
              storage: 2Gi # Adjust size as needed

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
```

**Key corrections/improvements in `prometheus-values.yaml`:**

  - Corrected `prometheus.prometheusSpec.storage` to use `storageSpec.volumeClaimTemplate` for defining Prometheus PVCs, which is the correct structure for the `kube-prometheus-stack` chart.
  - Added `prometheus.prometheusSpec.replicas: 2` for high availability of Prometheus.
  - Explicitly mentioned changing `grafana.adminPassword` for production.
  - Clarified `grafana.persistence.storageClassName` usage.
  - Added `alertmanager.alertmanagerSpec.replicas: 1` as a default.

-----

## 4\. **Install kube-prometheus-stack Helm Chart**

First, create the dedicated namespace, and then install the chart using your custom values file.

```bash
kubectl create namespace monitoring

# Get the latest stable version of the chart if you don't want to hardcode
# CHART_VERSION=$(helm search repo prometheus-community/kube-prometheus-stack --versions | grep "kube-prometheus-stack" | awk '{print $2}' | head -n 1)
# echo "Installing kube-prometheus-stack version: $CHART_VERSION"

# Install the chart with your custom values
helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --version 75.4.0 \
  --namespace monitoring \
  --create-namespace \
  -f prometheus-values.yaml
```

**Explanation:**

  - The `--create-namespace` flag will create the `monitoring` namespace if it doesn't exist.
  - Using `-f prometheus-values.yaml` ensures your custom configurations are applied.
  - The Helm release name is chosen as `kube-prometheus-stack` for consistency. The previous `kube-prom-stack` was fine, but `kube-prometheus-stack` matches the chart name which can be less confusing.

Since your cluster is fresh (nodes are just a few minutes old), it might take a few minutes for all pods to start.

-----

## 5\. **Verify the Installation**

Check the status of all pods and Persistent Volume Claims (PVCs) in the `monitoring` namespace:

```bash
kubectl get pods -n monitoring
kubectl get pvc -n monitoring
```

You should see pods for Prometheus, Grafana, Alertmanager, node-exporter, and kube-state-metrics. Wait until all pods show `STATUS=Running` and `READY` is `1/1` or appropriate. Ensure PVCs are `Bound`.

-----

## 6\. **Access Grafana Dashboard**

Forward the Grafana service port to your local machine:

```bash
kubectl -n monitoring port-forward svc/kube-prometheus-stack-grafana 3000:80
```

**Important Note:** The service name for Grafana in `kube-prometheus-stack` is typically `kube-prometheus-stack-grafana` by default (it prefixes the release name to the service name). I've updated the command accordingly.

Then open your browser and visit:

```
http://localhost:3000
```

Default login credentials:

  - Username: `admin`
  - Password: `prom-operator` (as configured in your `prometheus-values.yaml`)

**Remember to change this password in `prometheus-values.yaml` for production deployments\!**

-----

## 7\. **Customize Installation (Upgrade)**

If you want to modify settings after the initial installation, edit your `prometheus-values.yaml` file and then upgrade the Helm release:

```bash
# Make your desired changes to prometheus-values.yaml

helm upgrade kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  -f prometheus-values.yaml
```

**Explanation:**

  - `helm upgrade` is used to apply changes to an existing release.
  - It's good practice to always use your `prometheus-values.yaml` with `helm upgrade` to maintain a single source of truth for your configuration.

-----

## 8\. **Uninstalling the Stack**

To remove the stack when no longer needed:

```bash
helm uninstall kube-prometheus-stack -n monitoring
```

To clean up CRDs manually (optional, but good for a full cleanup):

```bash
kubectl delete crd alertmanagerconfigs.monitoring.coreos.com alertmanagers.monitoring.coreos.com podmonitors.monitoring.coreos.com probes.monitoring.coreos.com prometheusagents.monitoring.coreos.com prometheuses.monitoring.coreos.com prometheusrules.monitoring.coreos.com scrapeconfigs.monitoring.coreos.com servicemonitors.monitoring.coreos.com thanosrulers.monitoring.coreos.com
```

-----

## 9\. **Additional Notes for Your Environment**

  - Your cluster uses **containerd** runtime, which is fully compatible with kube-prometheus-stack.
  - Kubernetes v1.32.2 is recent and supported.
  - Debian 12 nodes are stable and suitable for production-grade monitoring.
  - If you have network policies or firewalls, ensure traffic is allowed between monitoring components.
  - This stack provides comprehensive monitoring for your cluster nodes, pods, and control plane metrics out of the box.

-----

# Summary of Commands for Your Setup

```bash
# Add Helm repo
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# (Optional) Check chart versions
helm search repo prometheus-community/kube-prometheus-stack --versions

# Create custom values file (save the content from Section 3 into prometheus-values.yaml)
# cat <<EOF > prometheus-values.yaml
# # ... (paste content from Section 3 here) ...
# EOF

# Create namespace
kubectl create namespace monitoring

# Install kube-prometheus-stack using your custom values file
helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --version 75.4.0 \
  --namespace monitoring \
  --create-namespace \
  -f prometheus-values.yaml

# Check pods status
kubectl get pods -n monitoring
kubectl get pvc -n monitoring

# Port-forward Grafana to localhost
kubectl port-forward -n monitoring svc/kube-prometheus-stack-grafana 3000:80

# Open http://localhost:3000 in browser and login with admin/prom-operator (change password in production!)

# Optional: Upgrade the stack after modifying prometheus-values.yaml
# helm upgrade kube-prometheus-stack prometheus-community/kube-prometheus-stack -n monitoring -f prometheus-values.yaml

# Optional: uninstall
helm uninstall kube-prometheus-stack -n monitoring

# Optional: delete CRDs for full cleanup
kubectl delete crd alertmanagerconfigs.monitoring.coreos.com alertmanagers.monitoring.coreos.com podmonitors.monitoring.coreos.com probes.monitoring.coreos.com prometheusagents.monitoring.coreos.com prometheuses.monitoring.coreos.com prometheusrules.monitoring.coreos.com scrapeconfigs.monitoring.coreos.com servicemonitors.monitoring.coreos.com thanosrulers.monitoring.coreos.com
```

-----