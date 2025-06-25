
-----

## ServerSage Monitoring & Observability Documentation Hub

This document serves as a central hub for Standard Operating Procedures (SOPs) and deployment guides related to setting up and managing your monitoring, alerting, and observability stack within Kubernetes environments.

-----

### I. Core Monitoring & Alerting Infrastructure

This section covers the foundational components for your observability stack, including visualization, high-availability, and on-call management.

1.  **Grafana Installation SOP**

      * **Description:** Comprehensive guide for deploying and configuring Grafana, your data visualization and dashboarding platform.
      * **Link:** [Grafana Installation SOP](https://github.com/remiges-aniket/serversage/blob/main/docs/Installation/all-in-one/grafana.md)

2.  **Prometheus High-Availability (HA) Setup SOP**

      * **Description:** Detailed SOP for deploying Prometheus in a high-availability configuration using the Prometheus Operator (without Helm) to ensure robust metrics collection.
      * **Link:** [Prometheus H.A. Setup SOP](https://www.google.com/search?q=https://github.com/remiges-aniket/serversage/tree/main/application-files/kubernetes-deployements/high-availibility/prometheus-operator\(no-helm\))

3.  **Grafana OnCall Installation SOP**

      * **Description:** Guide for deploying Grafana OnCall on Kubernetes for incident management, alerting, and on-call schedule automation.
      * **Link:** [OnCall Installation SOP](https://github.com/remiges-aniket/serversage/blob/main/docs/Installation/kubernetes/grafana-oncall-on-k8s.md)

4.  **Alerts Configuration SOP**

      * **Description:** A standard operating procedure detailing how to configure and manage alerts within your monitoring system.
      * **Link:** [Alerts Configured SOP](https://github.com/remiges-aniket/serversage/blob/main/docs/aletrs/alerts-SOP.md)

-----

### II. Exporters & Data Collection Agents

This section outlines the deployment and configuration of various agents and exporters responsible for collecting metrics and logs from your applications and infrastructure, making them available to Prometheus.

5.  **OpenTelemetry (Otel) DaemonSet Deployment**

      * **Description:** Kubernetes manifest for deploying an OpenTelemetry Collector as a DaemonSet, enabling node-level telemetry data collection.
      * **Link:** [Otel DaemonSet Deployment](https://github.com/remiges-aniket/serversage/blob/main/application-files/kubernetes-deployements/otel/deamonset-otel.yaml)

6.  **APISIX Metrics to Prometheus**

      * **Description:** Guide for exposing and integrating Apache APISIX gateway metrics with Prometheus.
      * **Link:** [APISIX Metrics to Prometheus](https://github.com/remiges-aniket/serversage/blob/main/docs/Installation/kubernetes/apisix-metrics-to-prometheus.md)

7.  **PostgreSQL Exporter**

      * **Description:** SOP for deploying and configuring the Prometheus PostgreSQL Exporter to monitor your PostgreSQL databases.
      * **SOP Link:** [Postgres Exporter SOP](https://github.com/remiges-aniket/serversage/blob/main/application-files/postgres-exporter/SOP.md)
      * **Deployment Link:** [Postgres Exporter Deployment](https://github.com/remiges-aniket/serversage/blob/main/application-files/kubernetes-deployements/agents-exporters/postgres-exporter.yaml)

8.  **Elasticsearch Exporter Deployment**

      * **Description:** Kubernetes manifest for deploying an exporter to collect metrics from your Elasticsearch cluster.
      * **Link:** [Elastic-Exporter Deployment](https://github.com/remiges-aniket/serversage/blob/main/application-files/kubernetes-deployements/agents-exporters/elastic-exporter.yaml)

9.  **Kubernetes Metrics to Prometheus (Kube-State-Metrics)**

      * **Description:** Kubernetes manifest for deploying `kube-state-metrics`, essential for gathering cluster-level metrics (e.g., deployment status, pod counts) for Prometheus.
      * **Link:** [K8s Metrics (Kube-State-Metrics) Deployment](https://github.com/remiges-aniket/serversage/blob/main/application-files/kubernetes-deployements/agents-exporters/kube-state-metrics.yaml)

10. **Node Exporter DaemonSet**

      * **Description:** Kubernetes DaemonSet manifest for deploying Node Exporter to collect host-level metrics (CPU, memory, disk I/O) from all Kubernetes nodes.
      * **Link:** [Node-Exporter DaemonSet](https://github.com/remiges-aniket/serversage/blob/main/application-files/kubernetes-deployements/agents-exporters/node-exporter-deamonset.yaml)

11. **Kafka Exporter SOP with Deployment**

      * **Description:** SOP and deployment guide for the Prometheus Kafka Exporter to monitor your Apache Kafka brokers and topics.
      * **Link:** [Kafka Exporter SOP with Deployment](https://github.com/remiges-aniket/serversage/blob/main/application-files/kubernetes-deployements/agents-exporters/kafka-exporter/SOP.md)

12. **MinIO Metrics SOP**

      * **Description:** Standard Operating Procedure for enabling and collecting metrics from your MinIO object storage instances.
      * **Link:** [MinIO Metrics SOP](https://github.com/remiges-aniket/serversage/tree/main/application-files/kubernetes-deployements/databases/minio)

13. **Fluent Bit Kubernetes Logs SOP**

      * **Description:** SOP for deploying and configuring Fluent Bit as a DaemonSet on Kubernetes for efficient log collection and forwarding.
      * **Link:** [Fluentbit Kubernetes Logs SOP](https://github.com/remiges-aniket/serversage/blob/main/application-files/kubernetes-deployements/fluentbit-deamonset/SOP.md)

-----

### III. Integrations & Supporting Documents

This section covers specific integrations and other supporting documentation for enhancing your monitoring ecosystem.

14. **Keycloak & Grafana Integration**
      * **Description:** Guides for integrating Keycloak, an open-source identity and access management solution, with Grafana for centralized authentication and authorization.
      * **Integration Guide 1:** [Integrating Keycloak with Grafana](https://github.com/remiges-aniket/serversage/blob/main/docs/other-supporting-docs/Integrating%20Keycloak%20with%20Grafana.md)
      * **Integration Guide 2:** [Integrating Keycloak with Grafana Example](https://github.com/remiges-aniket/serversage/blob/main/docs/other-supporting-docs/Integrating%20Keycloak%20with%20Grafana%20Example.md)

-----