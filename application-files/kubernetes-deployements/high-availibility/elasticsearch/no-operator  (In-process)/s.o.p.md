# Deploying a Highly Available Elasticsearch Cluster on Kubernetes

This documentation provides a step-by-step guide to deploying a highly available Elasticsearch cluster on Kubernetes using the provided YAML manifests. This setup includes dedicated master, data, and client nodes, along with pre-configured services and storage.

## Table of Contents

1.  [Prerequisites](https://www.google.com/search?q=%23prerequisites)
2.  [Understanding the Architecture](https://www.google.com/search?q=%23understanding-the-architecture)
3.  [Configurable Parameters](https://www.google.com/search?q=%23configurable-parameters)
      * [Namespace](https://www.google.com/search?q=%23namespace)
      * [Replicas](https://www.google.com/search?q=%23replicas)
      * [Resource Limits and Requests](https://www.google.com/search?q=%23resource-limits-and-requests)
      * [Persistent Volume Sizes](https://www.google.com/search?q=%23persistent-volume-sizes)
      * [Elasticsearch Configuration](https://www.google.com/search?q=%23elasticsearch-configuration)
4.  [Deployment Procedure](https://www.google.com/search?q=%23deployment-procedure)
      * [Step 1: Create Namespace](https://www.google.com/search?q=%23step-1-create-namespace)
      * [Step 2: Apply ConfigMap](https://www.google.com/search?q=%23step-2-apply-configmap)
      * [Step 3: Apply Services](https://www.google.com/search?q=%23step-3-apply-services)
      * [Step 4: Create Storage Class and Persistent Volumes (Manual Provisioning)](https://www.google.com/search?q=%23step-4-create-storage-class-and-persistent-volumes-manual-provisioning)
      * [Step 5: Deploy Master Nodes](https://www.google.com/search?q=%23step-5-deploy-master-nodes)
      * [Step 6: Deploy Data Nodes](https://www.google.com/search?q=%23step-6-deploy-data-nodes)
      * [Step 7: Deploy Client Nodes](https://www.google.com/search?q=%23step-7-deploy-client-nodes)
5.  [Verification](https://www.google.com/search?q=%23verification)
6.  [Troubleshooting](https://www.google.com/search?q=%23troubleshooting)
7.  [Cleanup](https://www.google.com/search?q=%23cleanup)

## 1\. Prerequisites

Before you begin, ensure you have the following:

  * A running Kubernetes cluster (e.g., Minikube, Kubeadm, GKE, EKS, AKS).
  * `kubectl` installed and configured to communicate with your cluster.
  * Basic understanding of Kubernetes concepts (Pods, Services, StatefulSets, Deployments, ConfigMaps, Persistent Volumes, Storage Classes).
  * Permissions to create and manage resources within the Kubernetes cluster.

## 2\. Understanding the Architecture

This deployment sets up an Elasticsearch cluster with distinct node roles for high availability and efficient resource utilization:

  * **Master Nodes:** Responsible for cluster management tasks like metadata changes, index creation/deletion, and node tracking. They do not store data or handle search requests directly. There are 2 master nodes in this configuration, though a robust production setup typically uses 3 for quorum.
  * **Data Nodes:** Store the actual data and handle search and aggregation requests. They are optimized for storage and I/O.
  * **Client Nodes (Ingest Nodes):** Act as coordinating nodes. They receive client requests, route them to appropriate data nodes, and aggregate results. They also handle ingest pipelines if configured.
  * **Services:**
      * `elasticsearch` (Headless Service): Used by StatefulSets for stable network identities of the master and data nodes.
      * `elasticsearch-client` (ClusterIP Service): Provides a stable endpoint for applications to connect to the Elasticsearch cluster.
      * `elasticsearch-master` (Headless Service): Used for internal communication and discovery among master nodes.
      * `elasticsearch-data` (Headless Service): Used for internal communication and discovery among data nodes.
  * **Storage:** `hostPath` Persistent Volumes are used for demonstration purposes. In a production environment, you would typically use a dynamic provisioner like NFS, Rook, or cloud provider storage classes (e.g., AWS EBS, Azure Disk, GCP Persistent Disk) for persistent storage.

## 3\. Configurable Parameters

Before deploying, review and adjust the following parameters to suit your environment and requirements.

### Namespace

The namespace where all Elasticsearch components will be deployed.

  * **Default:** `elastic`

  * **To change:** Modify `namespace: elastic` in all YAML manifests to your desired namespace.

    ```yaml
    # Example: Changing namespace in ConfigMap
    apiVersion: v1
    kind: ConfigMap
    metadata:
      name: elasticsearch-config
      namespace: your-new-namespace # <--- Change this
      labels:
        app: elasticsearch
    ```

### Replicas

The number of instances for each node type.

  * **Master Nodes (`elasticsearch-master` StatefulSet):**

      * **Default:** `2`

      * **Recommendation:** For production, it's recommended to have an odd number of master nodes (e.g., 3 or 5) to maintain a quorum and prevent split-brain scenarios.

      * **To change:** Modify `replicas` in the `elasticsearch-master` StatefulSet.

        ```yaml
        # StatefulSet for Master Nodes
        apiVersion: apps/v1
        kind: StatefulSet
        metadata:
          name: elasticsearch-master
          namespace: elastic
          labels:
            app: elasticsearch
            role: master
        spec:
          serviceName: elasticsearch-master
          replicas: 3 # <--- Change this for 3 master nodes
        ```

  * **Data Nodes (`elasticsearch-data` StatefulSet):**

      * **Default:** `2`

      * **To change:** Modify `replicas` in the `elasticsearch-data` StatefulSet.

        ```yaml
        # StatefulSet for Data Nodes
        apiVersion: apps/v1
        kind: StatefulSet
        metadata:
          name: elasticsearch-data
          namespace: elastic
          labels:
            app: elasticsearch
            role: data
        spec:
          serviceName: elasticsearch-data
          replicas: 3 # <--- Change this for more data nodes
        ```

  * **Client Nodes (`elasticsearch-client` Deployment):**

      * **Default:** `1`

      * **To change:** Modify `replicas` in the `elasticsearch-client` Deployment.

        ```yaml
        # Deployment for Client Nodes
        apiVersion: apps/v1
        kind: Deployment
        metadata:
          name: elasticsearch-client
          namespace: elastic
          labels:
            app: elasticsearch
            role: client
        spec:
          replicas: 2 # <--- Change this for more client nodes
        ```

### Resource Limits and Requests

Define CPU and memory allocations for each node type.

  * **Master Nodes (`elasticsearch-master` StatefulSet):**

      * **Current:**

          * `limits.cpu`: `800m`
          * `limits.memory`: `2Gi`
          * `requests.cpu`: `500m`
          * `requests.memory`: `1Gi`

      * **JVM Heap Size (`ES_JAVA_OPTS`):** `-Xms1g -Xmx1g`

      * **Recommendation:** Master nodes require less memory than data nodes. Ensure `ES_JAVA_OPTS` for master nodes is set to half of the `requests.memory` for optimal performance. In the provided configuration, `requests.memory` is `1Gi` and `ES_JAVA_OPTS` is set to `-Xms1g -Xmx1g`. This is good.

      * **To change:** Modify the `resources` section within the `elasticsearch-master` container and the `ES_JAVA_OPTS` environment variable.

        ```yaml
        # ... inside elasticsearch-master container spec
        resources:
          limits:
            cpu: 1000m # <--- Adjust CPU limit
            memory: 3Gi # <--- Adjust memory limit
          requests:
            cpu: 700m # <--- Adjust CPU request
            memory: 1.5Gi # <--- Adjust memory request
        # ...
        env:
        - name: ES_JAVA_OPTS
          value: "-Xms768m -Xmx768m" # <--- Adjust JVM heap (half of requests.memory for master)
        ```

  * **Data Nodes (`elasticsearch-data` StatefulSet):**

      * **Current:**

          * `limits.cpu`: `800m`
          * `limits.memory`: `3Gi`
          * `requests.cpu`: `500m`
          * `requests.memory`: `2Gi`

      * **JVM Heap Size (`ES_JAVA_OPTS`):** `-Xms2g -Xmx2g`

      * **Recommendation:** Data nodes are memory-intensive. Allocate as much memory as possible, typically up to 50% of the node's available memory, with the remaining for the operating system and other processes. Ensure `ES_JAVA_OPTS` is set to half of the `requests.memory`. In the provided configuration, `requests.memory` is `2Gi` and `ES_JAVA_OPTS` is set to `-Xms2g -Xmx2g`. This is good.

      * **To change:** Modify the `resources` section within the `elasticsearch-data` container and the `ES_JAVA_OPTS` environment variable.

        ```yaml
        # ... inside elasticsearch-data container spec
        resources:
          limits:
            cpu: 1500m # <--- Adjust CPU limit
            memory: 8Gi # <--- Adjust memory limit
          requests:
            cpu: 1000m # <--- Adjust CPU request
            memory: 4Gi # <--- Adjust memory request
        # ...
        env:
        - name: ES_JAVA_OPTS
          value: "-Xms4g -Xmx4g" # <--- Adjust JVM heap (half of requests.memory for data)
        ```

  * **Client Nodes (`elasticsearch-client` Deployment):**

      * **Current:**

          * `limits.cpu`: `800m`
          * `limits.memory`: `2Gi`
          * `requests.cpu`: `500m`
          * `requests.memory`: `1Gi`

      * **JVM Heap Size (`ES_JAVA_OPTS`):** `-Xms1g -Xmx1g`

      * **Recommendation:** Client nodes require enough resources to handle incoming requests. Ensure `ES_JAVA_OPTS` is set to half of the `requests.memory`. In the provided configuration, `requests.memory` is `1Gi` and `ES_JAVA_OPTS` is set to `-Xms1g -Xmx1g`. This is good.

      * **To change:** Modify the `resources` section within the `elasticsearch-client` container and the `ES_JAVA_OPTS` environment variable.

        ```yaml
        # ... inside elasticsearch-client container spec
        resources:
          limits:
            cpu: 1000m # <--- Adjust CPU limit
            memory: 3Gi # <--- Adjust memory limit
          requests:
            cpu: 700m # <--- Adjust CPU request
            memory: 1.5Gi # <--- Adjust memory request
        # ...
        env:
        - name: ES_JAVA_OPTS
          value: "-Xms768m -Xmx768m" # <--- Adjust JVM heap (half of requests.memory for client)
        ```

### Persistent Volume Sizes

The storage allocated for each node type.

  * **Master Nodes (`elasticsearch-master-data` PersistentVolumeClaim):**

      * **Current:** `1Gi`

      * **To change:** Modify `storage` in the `volumeClaimTemplates` for `elasticsearch-master` StatefulSet.

        ```yaml
        # ... inside elasticsearch-master StatefulSet, volumeClaimTemplates
        volumeClaimTemplates:
        - metadata:
            name: elasticsearch-master-data
          spec:
            accessModes: [ "ReadWriteOnce" ]
            storageClassName: elasticsearch-storage
            resources:
              requests:
                storage: 5Gi # <--- Adjust storage for master nodes
        ```

  * **Data Nodes (`elasticsearch-data-data` PersistentVolumeClaim):**

      * **Current:** `2Gi`

      * **To change:** Modify `storage` in the `volumeClaimTemplates` for `elasticsearch-data` StatefulSet.

        ```yaml
        # ... inside elasticsearch-data StatefulSet, volumeClaimTemplates
        volumeClaimTemplates:
        - metadata:
            name: elasticsearch-data-data
          spec:
            accessModes: [ "ReadWriteOnce" ]
            storageClassName: elasticsearch-storage
            resources:
              requests:
                storage: 20Gi # <--- Adjust storage for data nodes
        ```

### Elasticsearch Configuration

The `ConfigMap` (`elasticsearch-config`) contains the core Elasticsearch configuration (`elasticsearch.yml` and `jvm.options`).

  * **`elasticsearch.yml`:**
      * `cluster.name`: `es-cluster` (should be consistent across all nodes)
      * `node.master`, `node.data`, `node.ingest`: These are dynamically set via environment variables in the StatefulSets/Deployment, overriding the ConfigMap values.
      * `discovery.seed_hosts`: Crucial for cluster bootstrapping. It lists the service names for master nodes. **Ensure this matches your master node service names.**
      * `cluster.initial_master_nodes`: Lists the initial master node names for bootstrapping. **Ensure this matches the names of your master node pods (e.g., `elasticsearch-master-0`, `elasticsearch-master-1`, etc.).**
      * `xpack.security.enabled`: `false` (security is disabled in this configuration). For production, it's highly recommended to enable X-Pack security.
      * `xpack.monitoring.collection.enabled`: `true` (monitoring is enabled).
  * **`jvm.options`:** Configures JVM settings for Elasticsearch.
      * `-Xms` and `-Xmx`: These are overridden by the `ES_JAVA_OPTS` environment variable in each StatefulSet/Deployment.
      * `bootstrap.memory_lock: true`: Recommended to prevent Elasticsearch from swapping memory.

**Important Note on `hostPath` and Persistent Volumes:**

The provided YAML uses `hostPath` for Persistent Volumes. This means the data will be stored directly on the Kubernetes node's filesystem. **This is suitable for local development and testing but NOT recommended for production environments.** For production, consider using a dynamic provisioner with a robust storage solution that provides high availability and data durability.

If you choose to use `hostPath` in a multi-node cluster, you must ensure the paths `/data/elasticsearch/master-0`, `/data/elasticsearch/master-1`, etc., are created on the **specific nodes** where the master/data pods are scheduled. This requires manual intervention and goes against the dynamic nature of Kubernetes.

## 4\. Deployment Procedure

Follow these steps to deploy the Elasticsearch cluster.

### Step 1: Create Namespace

First, create the namespace for your Elasticsearch deployment.

```bash
kubectl create namespace elastic
```

### Step 2: Apply ConfigMap

Apply the ConfigMap that holds the Elasticsearch configuration.

```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: elasticsearch-config
  namespace: elastic
  labels:
    app: elasticsearch
data:
  elasticsearch.yml: |-
    cluster.name: es-cluster
    node.master: ${NODE_MASTER}
    node.data: ${NODE_DATA}
    node.ingest: ${NODE_INGEST}
    network.host: 0.0.0.0
    discovery.seed_hosts: ["elasticsearch-master-0.elasticsearch-master.elastic.svc.cluster.local", "elasticsearch-master-1.elasticsearch-master.elastic.svc.cluster.local", "elasticsearch-master-2.elasticsearch-master.elastic.svc.cluster.local"]
    cluster.initial_master_nodes: ["elasticsearch-master-0", "elasticsearch-master-1", "elasticsearch-master-2"]
    bootstrap.memory_lock: true
    
    # # Critical settings for master election
    # discovery.zen.fd.ping_timeout: 30s
    # discovery.zen.fd.ping_retries: 5
    # discovery.zen.publish_timeout: 60s
    
    # Lower ping timeouts to detect node failures faster
    transport.ping_schedule: 5s
    
    # Enable automatic recovery and fault tolerance
    gateway.recover_after_nodes: 2
    gateway.expected_nodes: 3
    gateway.recover_after_time: 1m
    
    # Increase cluster stability
    cluster.routing.allocation.node_initial_primaries_recoveries: 4
    cluster.routing.allocation.node_concurrent_recoveries: 2
    
    xpack.security.enabled: false
    xpack.monitoring.collection.enabled: true
  jvm.options: |-
    -Xms1g
    -Xmx1g
    -XX:+UseConcMarkSweepGC
    -XX:CMSInitiatingOccupancyFraction=75
    -XX:+UseCMSInitiatingOccupancyOnly
    -XX:+AlwaysPreTouch
    -server
    -Djava.awt.headless=true
    -Dfile.encoding=UTF-8
    -Djna.nosys=true
    -XX:-OmitStackTraceInFastThrow
    -XX:+ShowCodeDetailsInExceptionMessages
    -Dio.netty.noUnsafe=true
    -Dio.netty.noKeySetOptimization=true
    -Dio.netty.recycler.maxCapacityPerThread=0
    -Dlog4j.shutdownHookEnabled=false
    -Dlog4j2.disable.jmx=true
    -Djava.io.tmpdir=${ES_TMPDIR}
    -XX:+HeapDumpOnOutOfMemoryError
    -XX:HeapDumpPath=data
    -XX:ErrorFile=logs/hs_err_pid%p.log
    -Xlog:gc*,gc+age=trace,safepoint:file=logs/gc.log:utctime,pid,tags:filecount=32,filesize=64m
EOF
```

### Step 3: Apply Services

Apply all the services required for inter-node communication and client access.

```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: elasticsearch
  namespace: elastic
  labels:
    app: elasticsearch
spec:
  clusterIP: None
  selector:
    app: elasticsearch
  ports:
  - port: 9200
    name: http
    protocol: TCP
  - port: 9300
    name: transport
    protocol: TCP

---
apiVersion: v1
kind: Service
metadata:
  name: elasticsearch-client
  namespace: elastic
  labels:
    app: elasticsearch
    role: client
spec:
  type: ClusterIP
  selector:
    app: elasticsearch
    role: client
  ports:
  - port: 9200
    name: http
    targetPort: 9200
  - port: 9300
    name: transport
    targetPort: 9300

---
apiVersion: v1
kind: Service
metadata:
  name: elasticsearch-master
  namespace: elastic
  labels:
    app: elasticsearch
    role: master
spec:
  clusterIP: None
  selector:
    app: elasticsearch
    role: master
  ports:
  - port: 9300
    name: transport

---
apiVersion: v1
kind: Service
metadata:
  name: elasticsearch-data
  namespace: elastic
  labels:
    app: elasticsearch
    role: data
spec:
  clusterIP: None
  selector:
    app: elasticsearch
    role: data
  ports:
  - port: 9300
    name: transport
EOF
```

### Step 4: Create Storage Class and Persistent Volumes (Manual Provisioning)

**Crucial Step for `hostPath`:** Before applying the Persistent Volumes, you **must** ensure the corresponding directories exist on the Kubernetes nodes where these PVs are expected to be bound. If you have a multi-node cluster, you might need to create these directories manually on each node.

For example, if `elasticsearch-master-pv-0` is intended for `node1`, you'd need to `ssh` into `node1` and run:

```bash
# On your Kubernetes worker node(s)
sudo mkdir -p /data/elasticsearch/master-0
sudo chmod 777 /data/elasticsearch/master-0 # Adjust permissions as needed for Elasticsearch user (UID 1000)
sudo mkdir -p /data/elasticsearch/master-1
sudo chmod 777 /data/elasticsearch/master-1
sudo mkdir -p /data/elasticsearch/master-2
sudo chmod 777 /data/elasticsearch/master-2

sudo mkdir -p /data/elasticsearch/data-0
sudo chmod 777 /data/elasticsearch/data-0
sudo mkdir -p /data/elasticsearch/data-1
sudo chmod 777 /data/elasticsearch/data-1
sudo mkdir -p /data/elasticsearch/data-2
sudo chmod 777 /data/elasticsearch/data-2
```

Now, apply the StorageClass and PersistentVolumes.

```bash
kubectl apply -f - <<EOF
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  namespace: elastic
  name: elasticsearch-storage
provisioner: kubernetes.io/no-provisioner
volumeBindingMode: WaitForFirstConsumer
reclaimPolicy: Retain

---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: elasticsearch-master-pv-0
  namespace: elastic
  labels:
    app: elasticsearch
    role: master
spec:
  capacity:
    storage: 1Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: elasticsearch-storage
  hostPath:
    path: /data/elasticsearch/master-0
    type: DirectoryOrCreate

---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: elasticsearch-master-pv-1
  namespace: elastic
  labels:
    app: elasticsearch
    role: master
spec:
  capacity:
    storage: 1Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: elasticsearch-storage
  hostPath:
    path: /data/elasticsearch/master-1
    type: DirectoryOrCreate

---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: elasticsearch-master-pv-2
  namespace: elastic
  labels:
    app: elasticsearch
    role: master
spec:
  capacity:
    storage: 1Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: elasticsearch-storage
  hostPath:
    path: /data/elasticsearch/master-2
    type: DirectoryOrCreate

---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: elasticsearch-data-pv-0
  namespace: elastic
  labels:
    app: elasticsearch
    role: data
spec:
  capacity:
    storage: 2Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: elasticsearch-storage
  hostPath:
    path: /data/elasticsearch/data-0
    type: DirectoryOrCreate

---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: elasticsearch-data-pv-1
  namespace: elastic
  labels:
    app: elasticsearch
    role: data
spec:
  capacity:
    storage: 2Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: elasticsearch-storage
  hostPath:
    path: /data/elasticsearch/data-1
    type: DirectoryOrCreate

---
apiVersion: v1
kind: PersistentVolume
metadata:
  name: elasticsearch-data-pv-2
  namespace: elastic
  labels:
    app: elasticsearch
    role: data
spec:
  capacity:
    storage: 2Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: elasticsearch-storage
  hostPath:
    path: /data/elasticsearch/data-2
    type: DirectoryOrCreate
EOF
```

### Step 5: Deploy Master Nodes

Deploy the StatefulSet for Elasticsearch master nodes.

```bash
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: elasticsearch-master
  namespace: elastic
  labels:
    app: elasticsearch
    role: master
spec:
  serviceName: elasticsearch-master
  replicas: 2 # Adjust as per your configuration
  selector:
    matchLabels:
      app: elasticsearch
      role: master
  template:
    metadata:
      labels:
        app: elasticsearch
        role: master
    spec:
      # affinity:
      #   podAntiAffinity:
      #     preferredDuringSchedulingIgnoredDuringExecution:
      #     - weight: 100
      #       podAffinityTerm:
      #         labelSelector:
      #           matchExpressions:
      #           - key: role
      #             operator: In
      #             values:
      #             - master
      affinity:
        podAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: kubernetes.io/hostname
                  operator: In
                  values:
                  - my-cluster-worker3 # Consider removing or generalizing this for broader cluster use
              topologyKey: kubernetes.io/hostname
      initContainers:
      - name: init-sysctl
        image: busybox:1.27.2
        command:
        - sysctl
        - -w
        - vm.max_map_count=262144
        securityContext:
          privileged: true
      - name: fix-permissions
        image: busybox:1.27.2
        command: ["sh", "-c", "chown -R 1000:1000 /usr/share/elasticsearch/data"]
        securityContext:
          privileged: true
        volumeMounts:
        - name: elasticsearch-master-data
          mountPath: /usr/share/elasticsearch/data
      containers:
      - name: elasticsearch-master
        image: bitnami/elasticsearch:8.11.1
        resources:
          limits:
            cpu: 800m
            memory: 2Gi
          requests:
            cpu: 500m
            memory: 1Gi
        ports:
        - containerPort: 9200
          name: http
        - containerPort: 9300
          name: transport
        env:
        - name: node.name
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: cluster.name
          value: es-cluster
        - name: NODE_MASTER
          value: "true"
        - name: NODE_DATA
          value: "false"
        - name: NODE_INGEST
          value: "false"
        - name: bootstrap.memory_lock
          value: "true"
        - name: ES_JAVA_OPTS
          value: "-Xms1g -Xmx1g"
        - name: ES_TMPDIR
          value: /tmp
        volumeMounts:
        - name: elasticsearch-master-data
          mountPath: /usr/share/elasticsearch/data
        - name: config
          mountPath: /usr/share/elasticsearch/config/elasticsearch.yml
          subPath: elasticsearch.yml
        - name: config
          mountPath: /usr/share/elasticsearch/config/jvm.options
          subPath: jvm.options
        readinessProbe:
          exec:
            command:
            - bash
            - -c
            - |
              #!/usr/bin/env bash
              set -e
              curl --silent --fail localhost:9200/_cluster/health?wait_for_status=yellow
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          successThreshold: 1
          failureThreshold: 5
      volumes:
      - name: config
        configMap:
          name: elasticsearch-config
  volumeClaimTemplates:
  - metadata:
      name: elasticsearch-master-data
    spec:
      accessModes: [ "ReadWriteOnce" ]
      storageClassName: elasticsearch-storage
      resources:
        requests:
          storage: 1Gi # Adjust as per your configuration
EOF
```

**Note on Affinity:** The `podAffinity` rule for master nodes in your provided YAML explicitly ties them to `my-cluster-worker3`. In a general production setup, you would typically use `podAntiAffinity` to spread master nodes across different nodes for higher availability (as commented out in your original YAML). If you are deploying to a specific node, keep your current affinity, otherwise, consider using `podAntiAffinity` or removing it for Kubernetes to schedule freely.

### Step 6: Deploy Data Nodes

Deploy the StatefulSet for Elasticsearch data nodes.

```bash
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: elasticsearch-data
  namespace: elastic
  labels:
    app: elasticsearch
    role: data
spec:
  serviceName: elasticsearch-data
  replicas: 2 # Adjust as per your configuration
  selector:
    matchLabels:
      app: elasticsearch
      role: data
  template:
    metadata:
      labels:
        app: elasticsearch
        role: data
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: role
                  operator: In
                  values:
                  - data
              topologyKey: kubernetes.io/hostname
      initContainers:
      - name: init-sysctl
        image: busybox:1.27.2
        command:
        - sysctl
        - -w
        - vm.max_map_count=262144
        securityContext:
          privileged: true
      - name: fix-permissions
        image: busybox:1.27.2
        command: ["sh", "-c", "chown -R 1000:1000 /usr/share/elasticsearch/data"]
        securityContext:
          privileged: true
        volumeMounts:
        - name: elasticsearch-data-data
          mountPath: /usr/share/elasticsearch/data
      containers:
      - name: elasticsearch-data
        image: bitnami/elasticsearch:8.11.1
        resources:
          limits:
            cpu: 800m
            memory: 3Gi
          requests:
            cpu: 500m
            memory: 2Gi
        ports:
        - containerPort: 9300
          name: transport
        env:
        - name: node.name
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: cluster.name
          value: es-cluster
        - name: NODE_MASTER
          value: "false"
        - name: NODE_DATA
          value: "true"
        - name: NODE_INGEST
          value: "true"
        - name: bootstrap.memory_lock
          value: "true"
        - name: ES_JAVA_OPTS
          value: "-Xms2g -Xmx2g"
        - name: ES_TMPDIR
          value: /tmp
        - name: discovery.seed_hosts
          value: "elasticsearch-master-0.elasticsearch-master.elastic.svc.cluster.local,elasticsearch-master-1.elasticsearch-master.elastic.svc.cluster.local,elasticsearch-master-2.elasticsearch-master.elastic.svc.cluster.local"
        volumeMounts:
        - name: elasticsearch-data-data
          mountPath: /usr/share/elasticsearch/data
        - name: config
          mountPath: /usr/share/elasticsearch/config/elasticsearch.yml
          subPath: elasticsearch.yml
        - name: config
          mountPath: /usr/share/elasticsearch/config/jvm.options
          subPath: jvm.options
        readinessProbe:
          exec:
            command:
            - bash
            - -c
            - |
              #!/usr/bin/env bash
              set -e
              curl --silent --fail localhost:9200/_cluster/health?wait_for_status=yellow
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          successThreshold: 1
          failureThreshold: 5
      volumes:
      - name: config
        configMap:
          name: elasticsearch-config
  volumeClaimTemplates:
  - metadata:
      name: elasticsearch-data-data
    spec:
      accessModes: [ "ReadWriteOnce" ]
      storageClassName: elasticsearch-storage
      resources:
        requests:
          storage: 2Gi # Adjust as per your configuration
EOF
```

### Step 7: Deploy Client Nodes

Deploy the Deployment for Elasticsearch client (ingest) nodes.

```bash
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: elasticsearch-client
  namespace: elastic
  labels:
    app: elasticsearch
    role: client
spec:
  replicas: 1 # Adjust as per your configuration
  selector:
    matchLabels:
      app: elasticsearch
      role: client
  template:
    metadata:
      labels:
        app: elasticsearch
        role: client
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: role
                  operator: In
                  values:
                  - client
              topologyKey: kubernetes.io/hostname
      initContainers:
      - name: init-sysctl
        image: busybox:1.27.2
        command:
        - sysctl
        - -w
        - vm.max_map_count=262144
        securityContext:
          privileged: true
      containers:
      - name: elasticsearch-client
        image: bitnami/elasticsearch:8.11.1
        resources:
          limits:
            cpu: 800m
            memory: 2Gi
          requests:
            cpu: 500m
            memory: 1Gi
        ports:
        - containerPort: 9200
          name: http
        - containerPort: 9300
          name: transport
        env:
        - name: node.name
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: cluster.name
          value: es-cluster
        - name: NODE_MASTER
          value: "false"
        - name: NODE_DATA
          value: "false"
        - name: NODE_INGEST
          value: "true"
        - name: bootstrap.memory_lock
          value: "true"
        - name: ES_JAVA_OPTS
          value: "-Xms1g -Xmx1g"
        - name: ES_TMPDIR
          value: /tmp
        - name: discovery.seed_hosts
          value: "elasticsearch-master-0.elasticsearch-master.elastic.svc.cluster.local,elasticsearch-master-1.elasticsearch-master.elastic.svc.cluster.local,elasticsearch-master-2.elasticsearch-master.elastic.svc.cluster.local"
        volumeMounts:
        - name: config
          mountPath: /usr/share/elasticsearch/config/elasticsearch.yml
          subPath: elasticsearch.yml
        - name: config
          mountPath: /usr/share/elasticsearch/config/jvm.options
          subPath: jvm.options
        readinessProbe:
          httpGet:
            path: /_cluster/health
            port: 9200
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          successThreshold: 1
          failureThreshold: 5
      volumes:
      - name: config
        configMap:
          name: elasticsearch-config
EOF
```

## 5\. Verification

After applying all the manifests, verify the deployment:

1.  **Check Pods:**

    ```bash
    kubectl get pods -n elastic -o wide
    ```

    You should see pods for master, data, and client nodes in a `Running` state.
    Example output:

    ```
    NAME                         READY   STATUS    RESTARTS   AGE   IP             NODE                NOMINATED NODE   READINESS GATES
    elasticsearch-client-abcde   1/1     Running   0          2m    10.42.0.10     worker-node-1       <none>           <none>
    elasticsearch-data-0         1/1     Running   0          2m    10.42.1.20     worker-node-2       <none>           <none>
    elasticsearch-data-1         1/1     Running   0          2m    10.42.2.30     worker-node-3       <none>           <none>
    elasticsearch-master-0       1/1     Running   0          3m    10.42.0.5      master-node-1       <none>           <none>
    elasticsearch-master-1       1/1     Running   0          3m    10.42.1.15     master-node-2       <none>           <none>
    ```

2.  **Check Services:**

    ```bash
    kubectl get svc -n elastic
    ```

    Verify that all Elasticsearch services are created.

3.  **Check Persistent Volume Claims (PVCs) and Persistent Volumes (PVs):**

    ```bash
    kubectl get pvc -n elastic
    kubectl get pv -n elastic
    ```

    Ensure your PVCs are bound to the corresponding PVs.

4.  **Check Elasticsearch Cluster Health:**
    Access the Elasticsearch cluster health endpoint through the client service. You can use port-forwarding:

    ```bash
    kubectl port-forward svc/elasticsearch-client 9200:9200 -n elastic
    ```

    Then, open another terminal and query the health:

    ```bash
    curl -XGET "localhost:9200/_cluster/health?pretty"
    ```

    You should see a `status` of `yellow` (if you have replicas configured but not enough data for all shards to be allocated) or `green` (all shards are allocated and healthy). The `number_of_nodes` should reflect your total deployed master, data, and client nodes.

    To check specific nodes:

    ```bash
    curl -XGET "localhost:9200/_cat/nodes?v&pretty"
    ```

## 6\. Troubleshooting

  * **Pods stuck in `Pending` state:**

      * Check `kubectl describe pod <pod-name> -n elastic` for events.
      * Could be due to insufficient resources (CPU/Memory). Adjust resource requests/limits.
      * If using `hostPath` PVs, ensure the directories on the host nodes exist and have correct permissions.
      * If PVs are unbound, ensure the PVs exist and their `storageClassName` matches the PVC's.

  * **Pods restarting frequently (`CrashLoopBackOff`):**

      * Check logs: `kubectl logs <pod-name> -n elastic`.
      * Common issues: JVM heap size misconfiguration (`ES_JAVA_OPTS`), `vm.max_map_count` not set, permission issues on data directories.
      * Ensure `vm.max_map_count` is set on the host nodes (the `initContainer` should handle this, but verify if issues persist).

  * **Cluster health is `red`:**

      * Indicates unassigned primary shards, often due to insufficient data nodes or persistent volume issues.
      * Check pod logs for specific errors.
      * Verify connectivity between nodes (check `discovery.seed_hosts` in `elasticsearch.yml`).

  * **Master node election issues:**

      * Ensure `discovery.seed_hosts` and `cluster.initial_master_nodes` are correctly configured in `elasticsearch.yml` within the ConfigMap.
      * Verify the number of master nodes (odd numbers are best for quorum).

  * **Readiness probes failing:**

      * Check network connectivity within the cluster.
      * Ensure Elasticsearch is actually listening on port 9200 (for HTTP).
      * Review Elasticsearch logs for startup errors.

## 7\. Cleanup

To remove the Elasticsearch cluster from your Kubernetes environment:

```bash
kubectl delete deployment elasticsearch-client -n elastic
kubectl delete statefulset elasticsearch-data -n elastic
kubectl delete statefulset elasticsearch-master -n elastic
kubectl delete service elasticsearch-client -n elastic
kubectl delete service elasticsearch-data -n elastic
kubectl delete service elasticsearch-master -n elastic
kubectl delete service elasticsearch -n elastic
kubectl delete configmap elasticsearch-config -n elastic
kubectl delete pvc -l app=elasticsearch -n elastic # Deletes PVCs created by StatefulSets
kubectl delete pv elasticsearch-master-pv-0 elasticsearch-master-pv-1 elasticsearch-master-pv-2 elasticsearch-data-pv-0 elasticsearch-data-pv-1 elasticsearch-data-pv-2 -n elastic
kubectl delete storageclass elasticsearch-storage -n elastic
kubectl delete namespace elastic
```

**Important `hostPath` Cleanup:** After deleting the PVs, the actual data on the host nodes will still remain in the `/data/elasticsearch` directories. If you want to completely remove all data, you will need to manually delete these directories from your Kubernetes worker nodes.

```bash
# On your Kubernetes worker node(s)
sudo rm -rf /data/elasticsearch/master-0
sudo rm -rf /data/elasticsearch/master-1
sudo rm -rf /data/elasticsearch/master-2
sudo rm -rf /data/elasticsearch/data-0
sudo rm -rf /data/elasticsearch/data-1
sudo rm -rf /data/elasticsearch/data-2
```