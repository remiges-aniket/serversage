# Standard Operating Procedure (SOP) for Elasticsearch Deployment

## Overview
This SOP provides instructions for deploying a production-ready Elasticsearch cluster on Kubernetes. The deployment consists of master nodes, data nodes, and client nodes working together to provide a scalable search and analytics solution.

## Components
1. **Master Nodes**: Manage cluster-wide actions like creating/deleting indices and tracking cluster nodes
2. **Data Nodes**: Store data and execute search/indexing operations
3. **Client Nodes**: Handle API requests and forward operations to data nodes

## Deployment Workflow

### 1. Prerequisites
- A running Kubernetes cluster
- `kubectl` CLI tool installed and configured
- Storage provisioning capability for persistent volumes
- Sufficient resources as specified in the configuration
---

## Configurable Parameters

### Cluster Configuration

| Parameter | Current Value | Description | Location |
|-----------|--------------|-------------|----------|
| Cluster Name | `es-cluster` | Name of the Elasticsearch cluster | ConfigMap: `cluster.name` |
| Master Node Count | 1 (replicas) | Number of master nodes | StatefulSet: `elasticsearch-master` replicas |
| Data Node Count | 2 (replicas) | Number of data nodes | StatefulSet: `elasticsearch-data` replicas |
| Client Node Count | 1 (replicas) | Number of client nodes | Deployment: `elasticsearch-client` replicas |

### Resource Allocation : (Modify this according to your choice)

| Component | CPU Request | CPU Limit | Memory Request | Memory Limit |
|-----------|------------|-----------|----------------|--------------|
| Master Nodes | 500m | 800m | 1Gi | 2Gi |
| Data Nodes | 500m | 800m | 2Gi | 3Gi |
| Client Nodes | 500m | 800m | 1Gi | 2Gi |

### Storage Configuration : (Modify this according to your storage)

| Component | Storage Size | Storage Class |
|-----------|-------------|---------------|
| Master Nodes | 1Gi | elasticsearch-storage |  
| Data Nodes | 2Gi | elasticsearch-storage |

### JVM Configuration

| Component | JVM Heap Size | Location |
|-----------|--------------|----------|
| Master Nodes | -Xms1g -Xmx1g | Environment variable: `ES_JAVA_OPTS` |
| Data Nodes | -Xms2g -Xmx2g | Environment variable: `ES_JAVA_OPTS` |
| Client Nodes | -Xms1g -Xmx1g | Environment variable: `ES_JAVA_OPTS` |

### Network Configuration

| Parameter | Value | Description |
|-----------|-------|-------------|
| Discovery Seed Hosts | elasticsearch-master-[0-2].elasticsearch-master.elastic.svc.cluster.local | Elasticsearch node discovery endpoints |
| Minimum Master Nodes | 1 | Minimum number of master nodes to form a quorum |
| Transport Ping Schedule | 5s | How often nodes ping each other |

### Security Configuration

| Parameter | Value | Description |
|-----------|-------|-------------|
| X-Pack Security | Disabled | Security features status |
| X-Pack Monitoring | Enabled | Monitoring features status |


---
### 2. Deployment Steps

#### Step 1: Create the Namespace
```bash
kubectl apply -f namespace.yaml
```
This creates the `elastic` namespace with Prometheus monitoring enabled.

#### Step 2: Verify Namespace Creation
```bash
kubectl get namespace elastic
```
#### Step 3: Deployment of cluster
```bash
kubectl apply -f elastic.yaml
```

#### Step 4: Monitor Deployment Progress
```bash
kubectl get pods -n elastic -w
```

#### Step 5: Verify Services are Running
```bash
kubectl get services -n elastic
```

#### Step 6: Check Cluster Health
```bash
kubectl exec -it elasticsearch-client-[pod-id] -n elastic -- curl -X GET "localhost:9200/_cluster/health?pretty"
```

#### Step 7: Access Elasticsearch
```bash
# Port forwarding for local access
kubectl port-forward svc/elasticsearch-client 9200:9200 -n elastic
```
Access Elasticsearch via: http://localhost:9200

## Architecture and Workflow Explanation

1. **Initialization Flow**:
   - Namespace and ConfigMap are created
   - Persistent volumes are provisioned
   - Master nodes are deployed first
   - Data nodes connect to master nodes
   - Client nodes establish connections to both

2. **Request Handling Flow**:
   - External requests are received by client nodes
   - Client nodes route search requests to data nodes
   - Data nodes process searches and return results
   - Master nodes coordinate cluster state changes

3. **Data Storage Flow**:
   - Incoming data is received by client nodes
   - Data is distributed across data nodes based on sharding
   - Each shard has replicas for redundancy 
   - Master nodes track shard allocation

## Configurable Parameters

## Shards and Replicas

### Default Configuration
- **Primary Shards**: The configuration doesn't explicitly set the number of primary shards, so Elasticsearch will use its default (1 primary shard per index)
- **Replica Shards**: The configuration doesn't explicitly set replica count, so Elasticsearch will use its default (1 replica per primary shard)

### Relevant Settings
- For a production environment, you would typically configure index settings with proper shard counts:
  ```
  PUT /my_index
  {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1
    }
  }
  ```

## Detailed Workflow Explanation

1. **Startup Process**:
   - Init containers run with privileged access to set kernel parameters and fix permissions
   - Master nodes start first and establish a cluster with election of a leader
   - Data nodes join the cluster using the master nodes as seed hosts
   - Client nodes connect to provide the API gateway to the cluster

2. **Data Indexing**:
   - Documents are sent to client nodes
   - Client nodes determine which shard should hold the document
   - Document is routed to appropriate data node
   - Data is persisted to disk on the data node
   - If configured, replicas are synchronized across other data nodes

3. **Search Process**:
   - Search query arrives at client node
   - Client node broadcasts to relevant shards across data nodes
   - Each data node performs local search on its shards
   - Results are aggregated by the coordinating client node
   - Final result returned to the requester

4. **Cluster Maintenance**:
   - Master nodes monitor health of all nodes
   - If a data node fails, master nodes reassign its shards to other nodes
   - The cluster continuously rebalances to ensure even data distribution

## Common Issues and Troubleshooting

1. **Cluster Health Is Red**:
   ```bash
   kubectl exec -it elasticsearch-client-[pod-id] -n elastic -- curl localhost:9200/_cluster/health
   ```
   Check for unassigned shards or node failures.

2. **Memory Pressure**:
   ```bash
   kubectl top pods -n elastic
   ```
   Consider increasing JVM heap or Kubernetes resource limits.

3. **Storage Issues**:
   ```bash
   kubectl get pv -n elastic
   ```
   Ensure persistent volumes are properly provisioned.

-----------------------------------------------------------------------

FAQ:

**Here i've answered few answers that one of my coliuge have asked me, like:**
- Explain the requirements for the mentioned manifest file to run. Also, explain what the difference is between the master node, data node, and client node given in the shared YAML file, and also why it is needed. Are there any replications also? And tell me your view on that ! 

## Point 1: Setup & Explanation

Basic Flow:

![alt text](elastic-cluster-flow.jpg)

Okay, let's break down that Elasticsearch High Availability (HA) cluster manifest file.

Based on the YAML i've shared, here's an explanation of the requirements to run it, the differences between the node types, why they're needed, and my thoughts on replication:

**Requirements to Run the Manifest:**

To successfully deploy this Elasticsearch HA cluster using this manifest file, you'll need the following:

1.  **A Running Kubernetes Cluster:** This manifest is designed for Kubernetes. You'll need a functional Kubernetes cluster with at least three nodes (to accommodate the three master nodes with anti-affinity rules). This could be Minikube for local testing, a managed Kubernetes service like AKS, EKS, GKE, or a self-hosted Kubernetes setup.

2.  **`kubectl` Installed and Configured:** You need the `kubectl` command-line tool installed on your local machine and configured to communicate with your Kubernetes cluster.

3.  **Sufficient Resources:** Your Kubernetes nodes will need enough CPU, memory, and storage to run the Elasticsearch pods. The resource requests and limits defined in the manifest (`resources` section) will give you an idea of the requirements per pod.

4.  **Persistent Volume Provisioner:** The manifest uses `PersistentVolumeClaim`s (`data-elasticsearch-es-cdm-0`, `data-elasticsearch-es-cdm-1`, `data-elasticsearch-es-cdm-2`) to request persistent storage. Your Kubernetes cluster needs a provisioner (like `hostPath` for local testing, or cloud-based volume provisioners for production) that can dynamically create these volumes.

5.  **Network Connectivity:** Ensure that the nodes in your Kubernetes cluster can communicate with each other on the necessary ports. Elasticsearch uses port `9200` for HTTP and `9300` for transport (inter-node communication). Kubernetes will manage the internal networking through Services.

6.  **RBAC Permissions (Potentially):** Depending on your Kubernetes cluster's Role-Based Access Control (RBAC) settings, the ServiceAccount (`elasticsearch`) might need appropriate roles and role bindings to allow the Elasticsearch pods to function correctly (though this specific manifest doesn't explicitly define extensive custom RBAC rules).

**Differences Between Master Node, Data Node, and Client Node:**

The provided YAML file defines three distinct `StatefulSet`s, each representing a different type of Elasticsearch node:

* **`es-cdm` (Master Nodes):**
    * **Purpose:** These nodes are responsible for cluster management and orchestration. They maintain the cluster state (metadata about indices, shards, nodes), make decisions about shard allocation, and handle administrative requests like creating or deleting indices.
    * **Key Characteristics:**
        * `node.master: "true"`: This Elasticsearch configuration explicitly designates these nodes as master-eligible.
        * `node.data: "false"`: They are not intended to hold any data.
        * `node.ingest: "false"`: They do not perform data ingestion tasks.
        * Typically require more CPU and memory for managing cluster state but less disk space.
        * Having an odd number (in this case, 3) is crucial for quorum and preventing split-brain scenarios in distributed systems.
        * The `podAntiAffinity` rule ensures that these three master nodes are scheduled on different Kubernetes nodes for high availability.

* **`es-data` (Data Nodes):**
    * **Purpose:** These nodes are the workhorses of the Elasticsearch cluster. They store the actual data (indices and their shards) and perform data-related operations like searching, indexing, and aggregations.
    * **Key Characteristics:**
        * `node.master: "false"`
        * `node.data: "true"`: This is their primary role.
        * `node.ingest: "false"`
        * Require significant disk space, CPU, and I/O resources depending on the data volume and query load.
        * Scaling the data tier (adding more data nodes) is the primary way to increase storage capacity and improve query performance.

* **`es-client` (Client/Coordinating Nodes):**
    * **Purpose:** These nodes act as smart load balancers and request routers. They receive client requests (search, index, etc.) and forward them to the appropriate data nodes. They also gather and process the results before returning them to the client.
    * **Key Characteristics:**
        * `node.master: "false"`
        * `node.data: "false"`
        * `node.ingest: "false"`
        * `node.remote_cluster.seed_hosts`: This configuration suggests they might be aware of and potentially interact with a remote Elasticsearch cluster.
        * Help to offload the data nodes from handling client communication and result aggregation, improving overall cluster performance.
        * Can be scaled independently based on the client request volume.

**Why These Different Node Types Are Needed:**

Separating Elasticsearch nodes into these roles provides several critical benefits for stability, performance, and scalability:

* **Stability:** Isolating the master nodes from data and query operations makes them more stable. If data nodes become overloaded due to heavy I/O or search queries, it won't directly impact the master nodes' ability to manage the cluster state. A healthy and responsive master quorum is essential for the overall health of the Elasticsearch cluster.

* **Performance:** By dedicating nodes to specific tasks, you can optimize resource allocation. Data nodes can be equipped with fast storage, while client nodes can have more network bandwidth. This specialization leads to better performance for both data operations and client interactions.

* **Scalability:** You can scale each tier independently based on its specific needs. If you need more storage or search capacity, you can add more data nodes without affecting the master or client nodes. If you have a high volume of client requests, you can scale the client nodes.

* **Resource Optimization:** You can tailor the hardware and software configurations of each node type to its specific workload, leading to more efficient resource utilization and cost savings.

**Replication (Sharding and Replicas):**

Yes, Elasticsearch inherently supports replication through the concepts of **shards** and **replicas**. While this manifest doesn't explicitly configure the number of shards or replicas for indices, the fact that it's setting up a multi-node cluster strongly implies that you would leverage these features when creating indices:

* **Sharding:** Indices are divided into multiple smaller pieces called shards. This allows you to distribute data across multiple nodes, improving horizontal scalability and search performance.

* **Replicas:** Each primary shard can have one or more replica shards. Replica shards are exact copies of the primary shard and serve two main purposes:
    * **High Availability:** If a node containing a primary shard fails, a replica shard can be promoted to become the new primary, ensuring no data loss and continued operation.
    * **Read Scalability:** Search requests can be distributed across all primary and replica shards, improving read performance.

**My View on This HA Cluster Setup:**

This manifest represents a well-architected approach to deploying a highly available and scalable Elasticsearch cluster on Kubernetes. Here are my key observations and thoughts:

* **Clear Role Separation:** The explicit separation of master, data, and client nodes is a best practice for production Elasticsearch deployments. It promotes stability, performance, and independent scalability.

* **Master Quorum:** Deploying three master nodes with `podAntiAffinity` is crucial for maintaining a stable quorum and preventing split-brain scenarios. An odd number of masters ensures that a majority can always be formed.

* **StatefulSets for Data Persistence:** Using `StatefulSet`s for the data nodes (`es-data`) is the correct approach for stateful applications like Elasticsearch that require stable network identities and persistent storage. Each data node gets its own `PersistentVolumeClaim`, ensuring data durability across pod restarts.

* **Services for Access:** The manifest defines two `Service`s: `elasticsearch-es-http` for client access on port 9200 and `elasticsearch-es-transport` for inter-node communication on port 9300. These services provide stable endpoints for accessing the cluster and for internal communication.

* **Resource Management:** The `resources` section in each `StatefulSet` is important for setting requests and limits for CPU and memory. This helps Kubernetes manage resource allocation and prevents individual pods from consuming excessive resources. You'll need to adjust these values based on your expected workload.

* **Potential Improvements/Considerations (Not Explicitly in the Manifest):**
    * **Index Lifecycle Management (ILM):** For managing data retention and optimizing storage over time.
    * **Security:** Implementing security features like authentication, authorization, and TLS encryption is crucial for production environments. This manifest doesn't include these configurations.
    * **Monitoring:** Setting up monitoring for the Elasticsearch cluster and the Kubernetes infrastructure is essential for identifying and resolving issues.
    * **Ingest Nodes:** For heavy data transformation pipelines, dedicated ingest nodes could be beneficial (though this manifest doesn't include them).
    * **Node Selectors/Taints and Tolerations:** For more advanced node placement strategies based on hardware or other characteristics.

In summary, this manifest provides a solid foundation for a production-ready Elasticsearch HA cluster on Kubernetes by adhering to best practices for node roles and leveraging Kubernetes features for state management and high availability. Remember to configure sharding and replication at the index level to fully realize the benefits of this distributed architecture.


## Point 2: How to configure sharding and replication at the index level 

While the deployment manifest sets up the infrastructure for a distributed Elasticsearch cluster, the actual distribution and redundancy of your data are controlled at the **index level** through sharding and replication.

Since I can't directly modify your Elasticsearch cluster or create indices, I can provide you with the API calls you would use with the Elasticsearch REST API (typically via `curl` or an Elasticsearch client library) to configure sharding and replication when creating an index.

Here's how you can do it:

When creating an index, you can specify the number of primary shards (`index.number_of_shards`) and the number of replica shards (`index.number_of_replicas`).

**Example API Call using `curl`:**

Let's say you want to create an index named `my_data` with the following configuration:

* **5 primary shards:** This will distribute your data across multiple data nodes. The number of primary shards is generally set at index creation and is difficult to change later. Choose this number based on your expected data volume and the number of data nodes you have.
* **1 replica shard per primary shard:** This will create one copy of each primary shard, providing redundancy and improving read performance.

```bash
curl -XPUT 'localhost:9200/my_data' -H 'Content-Type: application/json' -d'
{
  "settings": {
    "index": {
      "number_of_shards": 5,
      "number_of_replicas": 1
    }
  },
  "mappings": {
    "properties": {
      "field1": { "type": "text" },
      "field2": { "type": "keyword" }
      // ... your other field mappings
    }
  }
}
'
```

**Explanation:**

* `curl -XPUT 'localhost:9200/my_data'`: This sends an HTTP PUT request to your Elasticsearch cluster (assuming it's accessible at `localhost:9200`) to create an index named `my_data`. You might need to adjust the hostname and port based on how you've exposed your Elasticsearch HTTP service in Kubernetes (e.g., through the `elasticsearch-es-http` service and its associated port).
* `-H 'Content-Type: application/json'`: This specifies that the request body is in JSON format.
* `-d'...'`: This contains the JSON request body.
* `"settings": { ... }`: This section defines the index-level settings.
* `"index": { ... }`: This nested section contains the specific index settings.
* `"number_of_shards": 5`: This sets the number of primary shards for the `my_data` index to 5.
* `"number_of_replicas": 1`: This sets the number of replica shards to 1 for each primary shard. This means each primary shard will have one copy.
* `"mappings": { ... }`: This section defines the schema of your data within the index. You'll need to define your fields and their data types here.

**Important Considerations:**

* **Number of Primary Shards:** Carefully consider the number of primary shards when creating an index. You generally cannot change this number after the index is created without re-indexing your data. Choose a number that anticipates your future data growth and allows for distribution across your data nodes. A common strategy is to have a number of primary shards that is a multiple of the number of data nodes.
* **Number of Replica Shards:** You can change the number of replicas for an existing index using the Update Index Settings API:

    ```bash
    curl -XPUT 'localhost:9200/my_data/_settings' -H 'Content-Type: application/json' -d'
    {
      "index": {
        "number_of_replicas": 2
      }
    }
    '
    ```

    This would update the `my_data` index to have 2 replica shards per primary shard.
* **Routing:** Elasticsearch uses a routing algorithm (by default, based on the document ID) to determine which shard a document belongs to. You can customize this routing if needed for advanced use cases.
* **Shard Allocation Awareness:** Kubernetes, along with Elasticsearch's awareness of node roles, helps ensure that primary and replica shards are ideally placed on different nodes. The `podAntiAffinity` rule for master nodes in your manifest contributes to the overall resilience of the cluster.

By configuring the `number_of_shards` and `number_of_replicas` when creating your indices, you will fully leverage the distributed architecture of your Elasticsearch cluster deployed on Kubernetes, gaining benefits in terms of data distribution, query performance, and high availability. Remember to tailor these settings to your specific `data volume`, `query patterns`, and the `size` of your Elasticsearch cluster.

✅ **Elasticsearch should connect to the `client`.**

Specifically:

* Use the **`elasticsearch-client`** service to connect from:

  * **Kibana**
  * **Kafka Connect**
  * **Any external tool**

---

**Do NOT connect to `data` or `master` nodes directly.**

Those are internal roles for the cluster and not meant for external access or ingestion and `service/elasticsearch` is for devOps & debbuging only.

---
