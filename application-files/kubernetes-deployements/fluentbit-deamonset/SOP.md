This SOP outlines the deployment and configuration of Fluent Bit in a Kubernetes environment to collect container logs, forward them to Kafka, and then consume them from Kafka to send to Elasticsearch. Finally, it details how to configure ServerSage to visualize these logs from Elasticsearch.

## Standard Operating Procedure: Kubernetes Log Collection with Fluent Bit, Kafka, Elasticsearch, and ServerSage

### 1\. Overview

This SOP describes a robust logging pipeline for Kubernetes clusters. Fluent Bit is deployed as a DaemonSet to collect logs from all nodes, send them to Kafka, from where another Fluent Bit instance (as a Deployment) consumes them and forwards them to Elasticsearch for storage and indexing. ServerSage is then used to visualize these logs.

**Pipeline Flow:**
`Kubernetes Pod Logs -> Fluent Bit (DaemonSet) -> Kafka -> Fluent Bit (Deployment/Consumer) -> Elasticsearch -> ServerSage (Visualization)`

### 2\. Prerequisites

The following components are assumed to be **pre-installed and pre-configured** in your environment:

  * **Kubernetes Cluster:** A running Kubernetes cluster (v1.18+ recommended).
  * **kubectl:** Configured to interact with your Kubernetes cluster.
  * **Kafka Cluster:** A running and accessible Kafka cluster within your Kubernetes environment. The Fluent Bit configurations assume `kafka.kafka.svc.cluster.local:9092` as the broker address.
  * **Elasticsearch Cluster:** A running and accessible Elasticsearch cluster. The Fluent Bit consumer configuration assumes `elasticsearch.elasticsearch.svc.cluster.local:9200` as the host and port.
  * **ServerSage Instance:** A running and accessible ServerSage instance (v8.0+ recommended).
      * **ServerSage User Permissions:** You must have Administrator or Editor roles in ServerSage.

### 3\. Deployment of Fluent Bit Log Shipper (DaemonSet)

This Fluent Bit instance runs on each Kubernetes node to collect container logs and forward them to Kafka.

#### 3.1. Create Namespace

Ensure the namespace for Fluent Bit deployments exists.

```bash
kubectl create namespace serversage-app
```

#### 3.2. Deploy Fluent Bit ConfigMap (fluent-bit-dep.yaml)

This ConfigMap contains the configuration for the Fluent Bit DaemonSet. It defines input (tailing container logs), filtering (Kubernetes metadata enrichment), and output (sending to Kafka).

1.  **Review the ConfigMap (`fluent-bit-dep.yaml` - ConfigMap section):**

      * **SERVICE:** Basic Fluent Bit service settings. `HTTP_Server On` enables a health endpoint.
      * **INPUT (tail):**
          * `Path: /var/log/containers/*.log` - Tails all container logs.
          * `Exclude_Path`: Specifies paths to ignore. This is crucial as it excludes Fluent Bit's own logs and specific staging application logs.
          * `Parser: docker` - Uses the 'docker' parser defined in `parsers.conf` for JSON-formatted logs.
          * `Tag: kube.*` - Adds a tag prefix to all collected logs for filtering.
      * **FILTER (kubernetes):**
          * Enriches logs with Kubernetes metadata (pod name, namespace, labels, annotations).
          * `Kube_URL`, `Kube_CA_File`, `Kube_Token_File`: Configures communication with the Kubernetes API.
      * **OUTPUT (kafka):**
          * `Brokers: kafka.kafka.svc.cluster.local:9092` - Target Kafka broker address. **Verify this matches your Kafka service.**
          * `Topics: kubernetes-logs` - The Kafka topic where logs will be sent.
      * **parsers.conf:** Defines a `docker` parser for JSON logs and a `multiline` parser.

2.  **Apply the ConfigMap:**

    ```bash
    kubectl apply -f fluent-bit-dep.yaml
    ```

#### 3.3. Deploy Fluent Bit DaemonSet (fluent-bit-dep.yaml)

This DaemonSet ensures that a Fluent Bit pod runs on every Kubernetes node to collect logs.

1.  **Review the DaemonSet (`fluent-bit-dep.yaml` - DaemonSet section):**

      * `image: cr.fluentbit.io/fluent/fluent-bit:2.2.2` - Specifies the Fluent Bit image.
      * `volumeMounts` and `volumes`: Mounts host paths `/var/log` and `/var/lib/docker/containers` where container logs reside, and mounts the `fluent-bit-config` ConfigMap.
      * `tolerations`: Allows Fluent Bit to run on control-plane/master nodes.
      * `serviceAccountName: fluent-bit`: Specifies the service account for Kubernetes API access.

2.  **Apply the DaemonSet, ServiceAccount, ClusterRole, and ClusterRoleBinding:**

    ```bash
    kubectl apply -f fluent-bit-dep.yaml
    ```

    *This single command applies all resources defined in `fluent-bit-dep.yaml`: ConfigMap, DaemonSet, ServiceAccount, ClusterRole, and ClusterRoleBinding.*

#### 3.4. Verification Steps (Log Shipper)

1.  **Check Pod Status:** Verify Fluent Bit DaemonSet pods are running on all nodes.
    ```bash
    kubectl get pods -n serversage-app -l k8s-app=fluent-bit
    ```
    Expected output: All pods in `Running` status.
2.  **Check Logs for Errors:** Inspect logs of a Fluent Bit DaemonSet pod.
    ```bash
    kubectl logs -f -n serversage-app <fluent-bit-daemonset-pod-name>
    ```
    Look for messages indicating successful log collection and connection to Kafka.
3.  **Verify Kafka Topic Creation (Optional):** If `auto.create.topics.enable=true` on your Kafka brokers, the `kubernetes-logs` topic should be created. You can verify its existence using Kafka tools.

### 4\. Deployment of Fluent Bit Kafka Consumer (Deployment)

This Fluent Bit instance consumes logs from Kafka and forwards them to Elasticsearch.

#### 4.1. Deploy Fluent Bit Kafka Consumer ConfigMap (fluent-bit-kafka-consumer.yaml)

This ConfigMap contains the configuration for the Fluent Bit consumer Deployment. It defines input (reading from Kafka) and output (sending to Elasticsearch).

1.  **Review the ConfigMap (`fluent-bit-kafka-consumer.yaml` - ConfigMap section):**

      * **INPUT (kafka):**
          * `Brokers: kafka.kafka.svc.cluster.local:9092` - Kafka broker address.
          * `Topics: kubernetes-logs` - Topic to consume from.
          * `Group_Id: flb-consumer-group` - Kafka consumer group ID.
          * `Format: json` - Expects incoming messages to be JSON.
          * `Tag: kafka.logs` - Tags consumed messages.
      * **OUTPUT (es):**
          * `Host: elasticsearch.elasticsearch.svc.cluster.local` - Elasticsearch host. **Verify this matches your Elasticsearch service.**
          * `Port: 9200` - Elasticsearch port.
          * `Index: kubernetes-logs` - The Elasticsearch index where logs will be stored.
          * `Time_Key: @timestamp` - Specifies the field to use as the timestamp in Elasticsearch.
          * `Replace_Dots: On` - Replaces dots in field names with underscores (e.g., `kubernetes.pod_name` becomes `kubernetes_pod_name`), which is good practice for Elasticsearch.

2.  **Apply the ConfigMap:**

    ```bash
    kubectl apply -f fluent-bit-kafka-consumer.yaml
    ```

#### 4.2. Deploy Fluent Bit Kafka Consumer Deployment (fluent-bit-kafka-consumer.yaml)

This Deployment runs one or more Fluent Bit pods to act as Kafka consumers.

1.  **Review the Deployment (`fluent-bit-kafka-consumer.yaml` - Deployment section):**

      * `replicas: 1` - You can scale this up if your log volume is high and a single consumer cannot keep up.
      * `image: cr.fluentbit.io/fluent/fluent-bit:2.2.2` - Specifies the Fluent Bit image.
      * `volumeMounts` and `volumes`: Mounts the `fluent-bit-kafka-consumer-config` ConfigMap.

2.  **Apply the Deployment:**

    ```bash
    kubectl apply -f fluent-bit-kafka-consumer.yaml
    ```

#### 4.3. Verification Steps (Kafka Consumer)

1.  **Check Pod Status:** Verify the Fluent Bit Kafka consumer pod is running.
    ```bash
    kubectl get pods -n serversage-app -l app=fluent-bit-kafka-consumer
    ```
    Expected output: Pod in `Running` status.
2.  **Check Logs for Errors:** Inspect logs of the Fluent Bit consumer pod.
    ```bash
    kubectl logs -f -n serversage-app <fluent-bit-kafka-consumer-pod-name>
    ```
    Look for messages indicating successful connection to Kafka, consumption of messages, and successful output to Elasticsearch. Pay attention to `Trace_Output On` which will show the records being sent to ES.
3.  **Verify Elasticsearch Index (Optional):** Access your Elasticsearch cluster (e.g., via Kibana or `curl`) to confirm that the `kubernetes-logs-*` index (or just `kubernetes-logs` if you are not using Logstash format or daily indices) is being created and receiving documents.

### 5\. ServerSage Configuration for Elasticsearch Logs

Now, configure ServerSage to connect to Elasticsearch and visualize the collected logs.

#### 5.1. Add Elasticsearch Data Source in ServerSage

1.  **Login to ServerSage:** Access your ServerSage instance via your web browser.
2.  **Add Data Source:**
      * Navigate to **Connections** (or **Configuration** (gear icon) \> **Data Sources** in older ServerSage versions) in the left-hand menu.
      * Click **Add new connection** (or **Add data source**).
      * Search for and select **Elasticsearch**.
3.  **Configure Elasticsearch Data Source:**
      * **Name:** `Kubernetes Logs ES` (or a descriptive name).
      * **URLs:** Enter the URL of your Elasticsearch cluster. For a Kubernetes service, this would be `http://elasticsearch.elasticsearch.svc.cluster.local:9200`. If ServerSage is outside the cluster, use an external endpoint or NodePort/LoadBalancer service.
      * **Auth:** If your Elasticsearch cluster requires authentication, configure it here (e.g., `Basic auth` with username/password).
      * **Elasticsearch details:**
          * **Index name:** `kubernetes-logs` (This must exactly match the `Index` configured in your Fluent Bit Kafka consumer output).
          * **Time field:** `@timestamp` (This must exactly match the `Time_Key` configured in your Fluent Bit Kafka consumer output).
          * **Version:** Select the appropriate version of your Elasticsearch cluster.
      * (Optional) **Min time interval:** Set a value like `10s` or `1m` to guide query time ranges.
4.  **Save & Test:** Click **Save & Test** to ensure ServerSage can successfully connect to Elasticsearch and query the specified index. You should see a "Data source is working" message.

#### 5.2. Create a ServerSage Dashboard for Log Visualization

1.  **Create New Dashboard:**
      * Navigate to **Dashboards** \> **New Dashboard**.
      * Click **Add new panel**.
2.  **Configure Log Panel:**
      * **Visualization:** Select the **Logs** visualization type.
      * **Data Source:** Select the Elasticsearch data source you just created (`Kubernetes Logs ES`).
      * **Query:** In the `LogQL` or `Elasticsearch Query` field, you can start with a simple query to see all logs, or filter them.
          * To see all logs: Leave the query field empty.
          * To filter by Kubernetes namespace: `kubernetes.namespace_name:serversage-app` (assuming `Replace_Dots: On` in Fluent Bit).
          * To filter by pod name: `kubernetes.pod_name:my-app-pod-xyz`
          * You can use Lucene query syntax (common for Elasticsearch).
      * **Time range:** Adjust the dashboard's time range (top right corner) to view logs from the desired period (e.g., `Last 30 minutes`).
      * (Optional) **Panel Options:** Customize column visibility, etc.
3.  **Save Dashboard:** Click the **Save** icon at the top right, give your dashboard a name (e.g., `Kubernetes Application Logs`), and save it to a folder.

#### 5.3. Verification Steps (ServerSage)

1.  **Access Dashboard:** Open your newly created ServerSage dashboard.
2.  **Verify Log Flow:** You should start seeing logs from your Kubernetes containers appearing in the Logs panel.
3.  **Apply Filters:** Use the query field or add ServerSage variables to filter logs by `namespace`, `pod_name`, `container_name`, `severity`, etc., to confirm metadata enrichment is working correctly.
4.  **Check for Missing Logs:** If logs are missing, revisit verification steps for Fluent Bit DaemonSet and Consumer, and check their respective logs for errors or warnings. Also, check Elasticsearch health and index status.

This comprehensive SOP should guide you through setting up a robust log pipeline from Kubernetes to ServerSage via Kafka and Elasticsearch.