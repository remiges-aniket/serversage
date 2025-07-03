## Deploying a Highly Available Elasticsearch Cluster on Kubernetes Using the Operator

To achieve high availability (HA) for Elasticsearch on Kubernetes, the recommended approach is to use the official Elastic Cloud on Kubernetes (ECK) operator. This operator automates the deployment, scaling, and management of Elasticsearch clusters, ensuring best practices for resiliency and redundancy are followed[1][2].

Below is a step-by-step guide and a sample manifest to help you deploy a highly available Elasticsearch cluster using the ECK operator.

### **Prerequisites**

- A Kubernetes cluster with at least 3 nodes (each with at least 4GB RAM and 10GB storage recommended for production HA)[3][4].
- `kubectl` CLI configured for your cluster.
- ECK operator installed in your cluster.

### **Step 1: Install the ECK Operator**

Apply the ECK operator manifest:

```bash
kubectl apply -f https://download.elastic.co/downloads/eck/2.14.0/all-in-one.yaml
```

Wait for the operator pod to be running in the `elastic-system` namespace:

```bash
kubectl -n elastic-system get pods
```

### **Step 2: Deploy a Highly Available Elasticsearch Cluster**

Create a manifest file named `elasticsearch-ha.yaml` with the following content:

```yaml
apiVersion: elasticsearch.k8s.elastic.co/v1
kind: Elasticsearch
metadata:
  name: elasticsearch-ha
spec:
  version: 8.16.1
  nodeSets:
    - name: master-nodes
      count: 3
      config:
        node.roles: ["master"]
        node.store.allow_mmap: false
      podTemplate:
        spec:
          affinity:
            podAntiAffinity:
              requiredDuringSchedulingIgnoredDuringExecution:
                - labelSelector:
                    matchLabels:
                      elasticsearch.k8s.elastic.co/nodeSet: master-nodes
                  topologyKey: "kubernetes.io/hostname"
    - name: data-nodes
      count: 3
      config:
        node.roles: ["data"]
        node.store.allow_mmap: false
      podTemplate:
        spec:
          affinity:
            podAntiAffinity:
              requiredDuringSchedulingIgnoredDuringExecution:
                - labelSelector:
                    matchLabels:
                      elasticsearch.k8s.elastic.co/nodeSet: data-nodes
                  topologyKey: "kubernetes.io/hostname"
    - name: ingest-nodes
      count: 2
      config:
        node.roles: ["ingest"]
        node.store.allow_mmap: false
      podTemplate:
        spec:
          affinity:
            podAntiAffinity:
              requiredDuringSchedulingIgnoredDuringExecution:
                - labelSelector:
                    matchLabels:
                      elasticsearch.k8s.elastic.co/nodeSet: ingest-nodes
                  topologyKey: "kubernetes.io/hostname"
```

Apply the manifest:

```bash
kubectl apply -f elasticsearch-ha.yaml
```

### **Key HA Features in This Deployment**

- **Multiple Node Types:** Separate master, data, and ingest nodes for better scalability and resilience[4].
- **Pod Anti-Affinity:** Ensures pods of the same node type are scheduled on different Kubernetes nodes, reducing the risk of losing multiple nodes to a single node failure[2][4].
- **Minimum 3 Master Nodes:** Prevents split-brain scenarios and ensures cluster quorum[4].
- **Persistent Storage:** Each node set should use persistent volume claims (PVCs) for data durability (you can add `volumeClaimTemplates` to each nodeSet for production).
- **Resource Requests:** Adjust `resources` in the `podTemplate` for CPU/memory as needed for your workload.

### **Monitoring and Validation**

Check the cluster status:

```bash
kubectl get elasticsearch
```

Check pods and their distribution:

```bash
kubectl get pods -l elasticsearch.k8s.elastic.co/cluster-name=elasticsearch-ha -o wide
```

### **Additional Recommendations**

- **Backups:** Implement snapshot and restore for disaster recovery[5].
- **Security:** Enable RBAC, TLS, and authentication as per your security requirements[5][2].
- **Load Balancing:** Use a service or ingress for client traffic to distribute requests across ingest nodes[6].

This approach ensures a production-grade, highly available Elasticsearch deployment on Kubernetes using the operator pattern, following best practices for redundancy, resiliency, and scalability[2][4].

[1] https://www.elastic.co/docs/deploy-manage/deploy/cloud-on-k8s/elasticsearch-deployment-quickstart
[2] https://www.elastic.co/blog/high-availability-elasticsearch-on-kubernetes-with-eck-and-gke
[3] https://sematext.com/blog/elasticsearch-operator-on-kubernetes/
[4] https://faun.pub/https-medium-com-thakur-vaibhav23-ha-es-k8s-7e655c1b7b61?gi=8c81a19b753f
[5] https://nextbrick.com/how-to-run-and-deploy-elasticsearch-operator-on-kubernetes/
[6] https://kubedb.com/articles/deploy-elasticsearch-via-kubernetes-elasticsearch-operator/
[7] https://gcore.com/learning/first-steps-with-the-kubernetes-operator
[8] https://coralogix.com/blog/running-elk-on-kubernetes-with-eck-part-3/
[9] https://www.elastic.co/docs/deploy-manage/deploy/cloud-on-k8s/advanced-elasticsearch-node-scheduling
[10] https://portworx.com/elasticsearch-kubernetes/
[11] https://www.elastic.co/guide/en/cloud-on-k8s/2.5/k8s-advanced-node-scheduling.html
[12] https://github.com/husniadil/elasticsearch-kubernetes
[13] https://www.searchhub.io/how-to-deploy-elasticsearch-in-kubernetes-using-the-cloud-on-k8s-elasticsearch-operator
[14] https://sematext.com/blog/kubernetes-elasticsearch/
[15] https://engineering.udacity.com/high-performance-elk-with-kubernetes-part-1-1d09f41a4ce2?gi=2ba8e4c80a1e
[16] https://www.elastic.co/docs/deploy-manage/tools