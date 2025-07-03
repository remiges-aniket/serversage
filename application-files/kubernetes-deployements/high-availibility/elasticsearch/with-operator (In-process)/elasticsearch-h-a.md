## Deploying a Highly Available Elasticsearch Cluster on Kubernetes using the Official Operator (ECK) without Helm

For a robust and highly available Elasticsearch cluster on Kubernetes without relying on Helm, the recommended approach is to leverage the official Elasticsearch Operator, Elastic Cloud on Kubernetes (ECK). This operator simplifies the deployment, management, and scaling of Elasticsearch clusters, ensuring resilience and operational efficiency.

This guide provides a comprehensive walkthrough of deploying a production-ready, highly available Elasticsearch cluster using ECK with YAML manifests.

### 1. Understanding the Architecture for High Availability

A highly available Elasticsearch cluster on Kubernetes requires a thoughtful architecture that mitigates single points of failure. Key components of this architecture include:

* **Elastic Cloud on Kubernetes (ECK) Operator:** This acts as the control plane for managing your Elasticsearch clusters within Kubernetes. It automates complex tasks like provisioning, scaling, and upgrades.
* **Dedicated Node Roles:** For larger, production-grade clusters, it is best practice to separate node roles to ensure stability and performance. The primary roles are:
    * **Master Nodes:** Responsible for cluster management tasks like creating indices, tracking nodes, and electing a master. A minimum of three master-eligible nodes is crucial for high availability and to prevent "split-brain" scenarios.
    * **Data Nodes:** These nodes store the indexed data and handle data-related operations like search and aggregation. They are the workhorses of the cluster and require persistent storage.
    * **Coordinating-Only Nodes (Optional but Recommended):** These nodes act as smart load balancers, routing client requests to the appropriate data nodes. They offload the coordination overhead from data and master nodes, improving overall cluster performance and stability.
* **StatefulSets for Data Nodes:** Data nodes are stateful and require stable network identifiers and persistent storage. Kubernetes `StatefulSets` are ideal for deploying these nodes, ensuring that each pod maintains a persistent identity and is bound to its own persistent volume.
* **Persistent Storage:** To ensure data durability, data nodes must be backed by persistent storage. This is achieved through Kubernetes `PersistentVolumeClaims` (PVCs) that dynamically provision `PersistentVolumes` (PVs) from your underlying storage infrastructure.
* **Anti-Affinity Rules:** To prevent multiple master or data nodes from being scheduled on the same Kubernetes worker node, anti-affinity rules should be configured. This ensures that the failure of a single worker node does not take down your entire cluster or a majority of its critical components.

### 2. Deploying the ECK Operator

The first step is to deploy the ECK operator into your Kubernetes cluster. You can do this by applying the official YAML manifest from Elastic.

```bash
kubectl apply -f https://download.elastic.co/downloads/eck/2.13.0/crds.yaml
kubectl apply -f https://download.elastic.co/downloads/eck/2.13.0/operator.yaml

kubectl create -f https://download.elastic.co/downloads/eck/3.0.0/crds.yaml
kubectl apply -f https://download.elastic.co/downloads/eck/3.0.0/operator.yaml
```

These commands will create the necessary Custom Resource Definitions (CRDs) for Elasticsearch and other Elastic Stack components, and then deploy the operator itself in the `elastic-system` namespace.

You can monitor the operator's startup progress with the following command:

```bash
kubectl -n elastic-system logs -f statefulset.apps/elastic-operator
```

### 3. Deploying a Highly Available Elasticsearch Cluster

Once the operator is running, you can define your highly available Elasticsearch cluster in a YAML file. The following is an example of a cluster with three master nodes, three data nodes, and two coordinating-only nodes.

**`elasticsearch-ha-cluster.yaml`**

```yaml
apiVersion: elasticsearch.k8s.elastic.co/v1
kind: Elasticsearch
metadata:
  name: "ha-elasticsearch-cluster"
spec:
  version: "8.13.4" # Specify your desired Elasticsearch version
  nodeSets:
    - name: master
      count: 3
      config:
        node.roles: ["master"]
        cluster.initial_master_nodes: # Bootstrap the initial master nodes
          - ha-elasticsearch-cluster-es-master-0
          - ha-elasticsearch-cluster-es-master-1
          - ha-elasticsearch-cluster-es-master-2
      podTemplate:
        spec:
          affinity:
            podAntiAffinity:
              requiredDuringSchedulingIgnoredDuringExecution:
              - labelSelector:
                  matchLabels:
                    elasticsearch.k8s.elastic.co/cluster-name: "ha-elasticsearch-cluster"
                    elasticsearch.k8s.elastic.co/statefulset-name: "ha-elasticsearch-cluster-es-master"
                topologyKey: "kubernetes.io/hostname"
          containers:
          - name: elasticsearch
            resources:
              requests:
                memory: "4Gi"
                cpu: "1"
              limits:
                memory: "4Gi"
                cpu: "2"
      volumeClaimTemplates:
        - metadata:
            name: elasticsearch-data # The name of the PersistentVolumeClaim
          spec:
            accessModes:
            - ReadWriteOnce
            resources:
              requests:
                storage: 50Gi # Adjust the storage size as needed
            storageClassName: "your-storage-class" # Replace with your storage class

    - name: data
      count: 3
      config:
        node.roles: ["data"]
      podTemplate:
        spec:
          affinity:
            podAntiAffinity:
              requiredDuringSchedulingIgnoredDuringExecution:
              - labelSelector:
                  matchLabels:
                    elasticsearch.k8s.elastic.co/cluster-name: "ha-elasticsearch-cluster"
                    elasticsearch.k8s.elastic.co/statefulset-name: "ha-elasticsearch-cluster-es-data"
                topologyKey: "kubernetes.io/hostname"
          containers:
          - name: elasticsearch
            resources:
              requests:
                memory: "8Gi"
                cpu: "2"
              limits:
                memory: "8Gi"
                cpu: "4"
      volumeClaimTemplates:
        - metadata:
            name: elasticsearch-data
          spec:
            accessModes:
            - ReadWriteOnce
            resources:
              requests:
                storage: 200Gi # Adjust the storage size as needed
            storageClassName: "your-storage-class" # Replace with your storage class

    - name: coordinating
      count: 2
      config:
        node.roles: ["ingest", "remote_cluster_client"] # Coordinating nodes do not have master or data roles
      podTemplate:
        spec:
          containers:
          - name: elasticsearch
            resources:
              requests:
                memory: "2Gi"
                cpu: "1"
              limits:
                memory: "2Gi"
                cpu: "2"
```

**Key Configuration Points for High Availability:**

* **`spec.version`**: Specifies the version of Elasticsearch to deploy.
* **`spec.nodeSets`**: This is where you define the different groups of nodes in your cluster.
* **`name`**: A unique name for each node set (e.g., `master`, `data`, `coordinating`).
* **`count`**: The number of pods (nodes) in the set.
* **`config.node.roles`**:  Defines the roles for the nodes in the set.
* **`cluster.initial_master_nodes`**: This is a crucial setting for the initial bootstrap of the cluster and should list the names of the initial master-eligible nodes.
* **`podTemplate.spec.affinity.podAntiAffinity`**: This ensures that pods from the same node set are scheduled on different Kubernetes worker nodes, preventing a single node failure from impacting multiple Elasticsearch nodes of the same type.
* **`volumeClaimTemplates`**: This section defines the `PersistentVolumeClaim` for the data nodes, ensuring that each data node gets its own persistent storage.
* **`resources`**: It is critical to set appropriate resource requests and limits for memory and CPU for each node type based on your expected workload.

**Deploy the cluster by applying this YAML file:**

```bash
kubectl apply -f elasticsearch-ha-cluster.yaml
```

The ECK operator will now create the necessary `StatefulSets`, `Services`, `Pods`, and `PersistentVolumeClaims` to bring up your highly available Elasticsearch cluster.

### 4. Accessing the Cluster

The ECK operator automatically creates a `ClusterIP` service for your Elasticsearch cluster. You can find the service name and expose it for external access if needed.

To get the default `elastic` user's password, which is stored in a Kubernetes secret, you can use the following command:

```bash
kubectl get secret ha-elasticsearch-cluster-es-elastic-user -o=jsonpath='{.data.elastic}' | base64 --decode
```

For internal access within the Kubernetes cluster, you can use the following service endpoint:

`https://ha-elasticsearch-cluster-es-http.default.svc:9200`

For external access, you can use a `NodePort`, `LoadBalancer`, or `Ingress` controller to expose the `ha-elasticsearch-cluster-es-http` service.

By following these steps, you can successfully deploy a resilient and highly available Elasticsearch cluster on Kubernetes using the official ECK operator without the need for Helm, providing a solid foundation for your search and analytics workloads.