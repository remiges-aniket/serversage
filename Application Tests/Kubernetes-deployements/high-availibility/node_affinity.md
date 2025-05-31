Yes, you can definitely add `affinity` to your Kubernetes deployments to control where your pods are scheduled, addressing both of your requirements: preventing pods from running on master nodes and directing pods to specific nodes based on resources or other labels.

Here's how you can modify the provided YAML to include `affinity` for each of your Deployments and DaemonSets. I'll add `nodeSelector` and `affinity` rules to exclude master nodes and provide an example of how to target specific nodes.

**Key Concepts for `affinity`:**

* **`nodeSelector`**: A simpler way to constrain pods to nodes with specific labels. All of the specified labels must be present on the node for the pod to be scheduled there.
* **`affinity`**: A more powerful and flexible way to express scheduling preferences and hard requirements.
    * **`nodeAffinity`**: Constrains which nodes your pod is eligible to be scheduled on, based on node labels.
        * **`requiredDuringSchedulingIgnoredDuringExecution`**: A hard requirement. The pod will *not* be scheduled on nodes that don't satisfy the rules.
        * **`preferredDuringSchedulingIgnoredDuringExecution`**: A "soft" requirement. The scheduler tries to find nodes that satisfy the rules, but if none are available, it will still schedule the pod elsewhere.
    * **`podAffinity` / `podAntiAffinity`**: Constrains which nodes your pod is eligible to be scheduled on, based on labels of *other pods already running on the node*. Useful for co-locating or spreading out related pods. (Not directly needed for your current request but good to know).

---

**1. Excluding Master Nodes (for all Deployments and DaemonSets)**

Kubernetes master nodes typically have a `node-role.kubernetes.io/master` or `node-role.kubernetes.io/control-plane` taint (depending on the Kubernetes version and how the cluster was set up). To prevent pods from scheduling on them, you can use `nodeSelector` or `nodeAffinity` with a `does not exist` operator.

**General approach for excluding master nodes:**

Add this to the `spec.template.spec` of each Deployment and DaemonSet:

```yaml
    spec:
      # ... other pod spec configurations
      tolerations:
      - key: "node-role.kubernetes.io/control-plane"
        operator: "Exists"
        effect: "NoSchedule"
      - key: "node-role.kubernetes.io/master" # For older clusters or different setups
        operator: "Exists"
        effect: "NoSchedule"
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: node-role.kubernetes.io/control-plane
                operator: DoesNotExist
              - key: node-role.kubernetes.io/master # For older clusters or different setups
                operator: DoesNotExist
```

**Explanation:**

* **`tolerations`**: This allows the pods to be scheduled on nodes that have the `NoSchedule` taint associated with control-plane/master roles. Without these tolerations, even with the `nodeAffinity` rule, if your worker nodes also had these taints, the pods wouldn't schedule. It essentially says, "I can tolerate these specific taints."
* **`nodeAffinity`**: This is the core part that says, "only schedule me on nodes that *do not* have the `node-role.kubernetes.io/control-plane` label and *do not* have the `node-role.kubernetes.io/master` label."

---

**2. Targeting Specific Nodes based on Resources/Labels**

To tell a pod on which node to deploy, you would use `nodeSelector` or more complex `nodeAffinity` rules with specific node labels.

**Example: Targeting a node with a label `node-type: high-cpu`**

Let's say you label some of your nodes with `kubectl label node <node-name> node-type=high-cpu`.

You can then add this to the `spec.template.spec` of the relevant Deployment/DaemonSet:

**Using `nodeSelector` (simpler, exact match):**

```yaml
    spec:
      # ... other pod spec configurations
      nodeSelector:
        node-type: high-cpu
```

**Using `nodeAffinity` (more flexible, e.g., for multiple options or preferred scheduling):**

```yaml
    spec:
      # ... other pod spec configurations
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: node-type
                operator: In
                values:
                - high-cpu
          # You could also use preferredDuringSchedulingIgnoredDuringExecution
          # if you want it to try for high-cpu but fall back if none are available
          # preferredDuringSchedulingIgnoredDuringExecution:
          # - weight: 100
          #   preference:
          #     matchExpressions:
          #     - key: node-type
          #       operator: In
          #       values:
          #       - high-cpu
```

---

**Applying `affinity` to your YAML:**

I will now modify your provided YAML to include the `tolerations` and `nodeAffinity` rules to exclude master nodes for all relevant resources (`Deployment`, `StatefulSet`, `DaemonSet`).

For demonstrating node-specific deployment, I'll add an example `nodeSelector` to the `thanos-store-gateway` StatefulSet, assuming you want to run it on a node labeled `node-type: storage`. **Remember to apply actual labels to your nodes (`kubectl label node <node-name> node-type=storage`) for this to work.**

```yaml
# Create MinIO bucket for Thanos
apiVersion: batch/v1
kind: Job
metadata:
  name: create-thanos-bucket
  namespace: serversage
spec:
  template:
    spec:
      containers:
      - name: mc
        image: minio/mc:RELEASE.2025-04-16T18-13-26Z
        command:
        - /bin/sh
        - -c
        - |
          mc config host add myminio http://minio.database.svc.cluster.local:9000  minio minio123 && \
          mc mb --ignore-existing myminio/thanos
      restartPolicy: OnFailure
      # Add tolerations and nodeAffinity to prevent running on master
      tolerations:
      - key: "node-role.kubernetes.io/control-plane"
        operator: "Exists"
        effect: "NoSchedule"
      - key: "node-role.kubernetes.io/master"
        operator: "Exists"
        effect: "NoSchedule"
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: node-role.kubernetes.io/control-plane
                operator: DoesNotExist
              - key: node-role.kubernetes.io/master
                operator: DoesNotExist

---
# Thanos Configuration Secret
apiVersion: v1
kind: Secret
metadata:
  name: thanos-objstore-config
  namespace: serversage
type: Opaque
stringData:
  objstore.yaml: |
    type: S3
    config:
      bucket: thanos
      endpoint: minio.database.svc.cluster.local:9000
      access_key: minio
      secret_key: minio123
      insecure: true
      signature_version2: false

---
# Prometheus instance (CRD) - Production HA Setup
apiVersion: monitoring.coreos.com/v1
kind: Prometheus
metadata:
  name: prometheus
  namespace: serversage
spec:
  replicas: 1  # HA setup with 2 replicas for production
  serviceAccountName: prometheus
  serviceMonitorSelector: {}
  podMonitorSelector: {}
  probeSelector: {}
  scrapeInterval: "15s"
  evaluationInterval: "15s"
  retention: "30d"
  externalLabels:
    cluster: ha-cluster
  securityContext:
    # fsGroup: 2000
    runAsUser: 1000
    # runAsGroup: 2000
    # runAsNonRoot: true
  storage:
    volumeClaimTemplate:
      spec:
        storageClassName: prometheus-storage
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 1Gi
  thanos:
    baseImage: quay.io/thanos/thanos
    version: v0.38.0
    objectStorageConfig:
      key: objstore.yaml
      name: thanos-objstore-config
  # Prometheus Operator manages Prometheus pods, so its own pod spec will inherit these
  # We cannot directly add nodeAffinity/tolerations here for the Prometheus pods,
  # as Prometheus Operator controls the Prometheus StatefulSet.
  # The Prometheus Operator deployment itself will need affinity.
  # If you need to constrain Prometheus pods, you might need to look at Prometheus Operator
  # configuration options for `nodeSelector` or `tolerations` on the Prometheus CRD,
  # or rely on `podAntiAffinity` to spread them across worker nodes.

---
# Prometheus Operator Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: prometheus-operator
  namespace: serversage
  labels:
    app.kubernetes.io/name: prometheus-operator
spec:
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: prometheus-operator
  template:
    metadata:
      labels:
        app.kubernetes.io/name: prometheus-operator
    spec:
      serviceAccountName: prometheus-operator
      containers:
      - name: prometheus-operator
        image: quay.io/prometheus-operator/prometheus-operator:v0.71.0
        args:
        - --kubelet-service=kube-system/kubelet
        - --log-level=debug
        - --prometheus-config-reloader=quay.io/prometheus-operator/prometheus-config-reloader:v0.71.0
        ports:
        - containerPort: 8080
          name: http
      # Add tolerations and nodeAffinity to prevent running on master
      tolerations:
      - key: "node-role.kubernetes.io/control-plane"
        operator: "Exists"
        effect: "NoSchedule"
      - key: "node-role.kubernetes.io/master"
        operator: "Exists"
        effect: "NoSchedule"
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: node-role.kubernetes.io/control-plane
                operator: DoesNotExist
              - key: node-role.kubernetes.io/master
                operator: DoesNotExist

---
# Prometheus Operator Service Account
apiVersion: v1
kind: ServiceAccount
metadata:
  name: prometheus-operator
  namespace: serversage

---
# Prometheus Operator ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: prometheus-operator
rules:
- apiGroups: ["monitoring.coreos.com"]
  resources:
  - alertmanagers
  - alertmanagers/finalizers
  - prometheuses
  - prometheuses/finalizers
  - thanosrulers
  - thanosrulers/finalizers
  - servicemonitors
  - podmonitors
  - probes
  - prometheusrules
  verbs: ["*"]
- apiGroups: ["apps"]
  resources:
  - statefulsets
  verbs: ["*"]
- apiGroups: [""]
  resources:
  - configmaps
  - secrets
  verbs: ["*"]
- apiGroups: [""]
  resources:
  - pods
  verbs: ["list", "delete"]
- apiGroups: [""]
  resources:
  - services
  - services/finalizers
  - endpoints
  verbs: ["get", "create", "update", "delete"]
- apiGroups: [""]
  resources:
  - nodes
  verbs: ["list", "watch"]
- apiGroups: [""]
  resources:
  - namespaces
  verbs: ["get", "list", "watch"]

---
# Prometheus Operator ClusterRoleBinding
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: prometheus-operator
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: prometheus-operator
subjects:
- kind: ServiceAccount
  name: prometheus-operator
  namespace: serversage

---
# Prometheus Service Account
apiVersion: v1
kind: ServiceAccount
metadata:
  name: prometheus
  namespace: serversage

---
# Prometheus ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: prometheus
rules:
- apiGroups: [""]
  resources:
  - nodes
  - nodes/metrics
  - services
  - endpoints
  - pods
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources:
  - configmaps
  verbs: ["get"]
- apiGroups: ["networking.k8s.io"]
  resources:
  - ingresses
  verbs: ["get", "list", "watch"]
- nonResourceURLs: ["/metrics"]
  verbs: ["get"]

---
# Prometheus ClusterRoleBinding
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: prometheus
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: prometheus
subjects:
- kind: ServiceAccount
  name: prometheus
  namespace: serversage

---
# Node Exporter DaemonSet
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: node-exporter
  namespace: serversage
  labels:
    app.kubernetes.io/name: node-exporter
spec:
  selector:
    matchLabels:
      app.kubernetes.io/name: node-exporter
  template:
    metadata:
      labels:
        app.kubernetes.io/name: node-exporter
    spec:
      hostNetwork: true
      hostPID: true
      containers:
      - name: node-exporter
        image: quay.io/prometheus/node-exporter:v1.6.1
        args:
        - --path.procfs=/host/proc
        - --path.sysfs=/host/sys
        - --path.rootfs=/host/root
        - --collector.filesystem.mount-points-exclude=^/(dev|proc|sys|var/lib/docker/.+|var/lib/kubelet/.+)($|/)
        - --collector.filesystem.fs-types-exclude=^(autofs|binfmt_misc|bpf|cgroup2?|configfs|debugfs|devpts|devtmpfs|fusectl|hugetlbfs|iso9660|mqueue|nsfs|overlay|proc|procfs|pstore|rpc_pipefs|securityfs|selinuxfs|squashfs|sysfs|tracefs)$
        ports:
        - containerPort: 9100
          name: metrics
        resources:
          limits:
            cpu: 250m
            memory: 180Mi
          requests:
            cpu: 102m
            memory: 180Mi
        volumeMounts:
        - name: proc
          mountPath: /host/proc
          readOnly: true
        - name: sys
          mountPath: /host/sys
          readOnly: true
        - name: root
          mountPath: /host/root
          readOnly: true
          mountPropagation: HostToContainer
        securityContext:
          runAsNonRoot: true
          runAsUser: 65534
          capabilities:
            add: ["SYS_TIME"]
      volumes:
      - name: proc
        hostPath:
          path: /proc
      - name: sys
        hostPath:
          path: /sys
      - name: root
        hostPath:
          path: /
      # Add tolerations and nodeAffinity to prevent running on master
      tolerations:
      - key: "node-role.kubernetes.io/control-plane"
        operator: "Exists"
        effect: "NoSchedule"
      - key: "node-role.kubernetes.io/master"
        operator: "Exists"
        effect: "NoSchedule"
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: node-role.kubernetes.io/control-plane
                operator: DoesNotExist
              - key: node-role.kubernetes.io/master
                operator: DoesNotExist

---
# Thanos Querier Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: thanos-querier
  namespace: serversage
spec:
  replicas: 1  # HA setup for production
  selector:
    matchLabels:
      app: thanos-querier
  template:
    metadata:
      labels:
        app: thanos-querier
    spec:
      containers:
      - name: thanos
        image: quay.io/thanos/thanos:v0.38.0
        args:
        - query
        - --http-address=0.0.0.0:9090
        - --grpc-address=0.0.0.0:10901
        - --query.replica-label=prometheus_replica
        - --query.replica-label=replica
        - --endpoint=prometheus-operated.serversage.svc.cluster.local:10901
        - --endpoint=thanos-store-gateway.serversage.svc.cluster.local:10901
        - --query.auto-downsampling
        - --query.partial-response
        ports:
        - containerPort: 9090
          name: http
          protocol: TCP
        - containerPort: 10901
          name: grpc
          protocol: TCP
        livenessProbe:
          httpGet:
            path: /-/healthy
            port: http
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
        readinessProbe:
          httpGet:
            path: /-/ready
            port: http
          initialDelaySeconds: 15
          periodSeconds: 10
          timeoutSeconds: 5
        resources:
          requests:
            cpu: 200m
            memory: 512Mi
          limits:
            cpu: 1000m
            memory: 2Gi
      # Add tolerations and nodeAffinity to prevent running on master
      tolerations:
      - key: "node-role.kubernetes.io/control-plane"
        operator: "Exists"
        effect: "NoSchedule"
      - key: "node-role.kubernetes.io/master"
        operator: "Exists"
        effect: "NoSchedule"
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: node-role.kubernetes.io/control-plane
                operator: DoesNotExist
              - key: node-role.kubernetes.io/master
                operator: DoesNotExist

---
# Thanos Store Gateway StatefulSet
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: thanos-store-gateway
  namespace: serversage
spec:
  replicas: 1
  serviceName: thanos-store-gateway
  selector:
    matchLabels:
      app: thanos-store-gateway
  template:
    metadata:
      labels:
        app: thanos-store-gateway
    spec:
      securityContext:
        runAsUser: 0
      containers:
      - name: thanos
        image: quay.io/thanos/thanos:v0.38.0
        args:
        - store
        - --log.level=info
        - --data-dir=/var/thanos/store
        - --grpc-address=0.0.0.0:10901
        - --http-address=0.0.0.0:10902
        - --objstore.config-file=/etc/thanos/objstore.yaml
        - --store.index-header-posting-offsets-in-mem-sampling=128
        - --store.enable-index-header-lazy-reader
        - --sync-block-duration=10m
        - --chunk-pool-size=6GB
        - --index-cache-size=2GB
        ports:
        - containerPort: 10901
          name: grpc
        - containerPort: 10902
          name: http
        volumeMounts:
        - name: data
          mountPath: /var/thanos/store
        - name: config
          mountPath: /etc/thanos
        livenessProbe:
          httpGet:
            path: /-/healthy
            port: http
          initialDelaySeconds: 30
          periodSeconds: 15
        readinessProbe:
          httpGet:
            path: /-/ready
            port: http
          initialDelaySeconds: 15
          periodSeconds: 10
        resources:
          requests:
            memory: "3Gi"
            cpu: "500m"
          limits:
            memory: "8Gi"
            cpu: "2000m"
      volumes:
      - name: config
        secret:
          secretName: thanos-objstore-config
      - name: data
        persistentVolumeClaim:
          claimName: thanos-store-gateway-data
      # Add tolerations and nodeAffinity to prevent running on master
      tolerations:
      - key: "node-role.kubernetes.io/control-plane"
        operator: "Exists"
        effect: "NoSchedule"
      - key: "node-role.kubernetes.io/master"
        operator: "Exists"
        effect: "NoSchedule"
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: node-role.kubernetes.io/control-plane
                operator: DoesNotExist
              - key: node-role.kubernetes.io/master
                operator: DoesNotExist
              # Example: Target a node with 'node-type: storage' label for Thanos Store Gateway
              - key: node-type
                operator: In
                values:
                - storage # Make sure you label your storage node like: kubectl label node <your-node-name> node-type=storage

---
# Services
apiVersion: v1
kind: Service
metadata:
  name: thanos-querier
  namespace: serversage
spec:
  selector:
    app: thanos-querier
  ports:
  - name: http
    protocol: TCP
    port: 9090
    targetPort: 9090
  - name: grpc
    protocol: TCP
    port: 10901
    targetPort: 10901
  type: ClusterIP

---
apiVersion: v1
kind: Service
metadata:
  name: thanos-store-gateway
  namespace: serversage
spec:
  clusterIP: None
  ports:
  - name: grpc
    port: 10901
    targetPort: 10901
  - name: http
    port: 10902
    targetPort: 10902
  selector:
    app: thanos-store-gateway

---
apiVersion: v1
kind: Service
metadata:
  name: node-exporter
  namespace: serversage
  labels:
    app.kubernetes.io/name: node-exporter
spec:
  ports:
  - name: metrics
    port: 9100
    targetPort: metrics
  selector:
    app.kubernetes.io/name: node-exporter

---
apiVersion: v1
kind: Service
metadata:
  name: prometheus-operated
  namespace: serversage
  labels:
    operated-prometheus: "true"
spec:
  ports:
  - name: web
    port: 9090
    targetPort: web
  - name: thanos-sidecar
    port: 10901
    targetPort: 10901
  selector:
    app.kubernetes.io/name: prometheus
```

**Important Notes:**

1.  **Prometheus CRD:** The `Prometheus` Custom Resource is managed by the Prometheus Operator. While you can define some scheduling constraints within the `Prometheus` CRD (e.g., `nodeSelector`, `tolerations`), `affinity` is not directly exposed in the `thanos` section as of typical Prometheus Operator versions. The Prometheus Operator itself, and other native Kubernetes workloads, are where you'd primarily apply `affinity`.
    * If you need to strictly control where your Prometheus StatefulSet pods run, you would typically add `nodeSelector` and `tolerations` directly to the `spec` of the `Prometheus` CRD, similar to how it's done for a standard Deployment or StatefulSet. The Prometheus Operator will then apply these to the Prometheus StatefulSet it creates. I have not added `affinity` to the `Prometheus` CRD in the example above because it's not a standard field like `nodeSelector` and `tolerations` are for the `Prometheus` object.
2.  **Node Labels:** For targeting specific nodes, ensure you have correctly labeled your nodes in your Kubernetes cluster. You can check existing labels with `kubectl get nodes --show-labels` and add new ones with `kubectl label node <node-name> <label-key>=<label-value>`.
3.  **Testing:** After applying these changes, carefully monitor your pods using `kubectl get pods -n serversage -o wide` and `kubectl describe pod <pod-name> -n serversage` to ensure they are scheduling on the desired nodes and not failing due to unfulfilled `affinity` rules.
4.  **`minio.database.svc.cluster.local:9000`**: Ensure this service name and port are correct and accessible from your `serversage` namespace.