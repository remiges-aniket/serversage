# Grafana Installation Guide

This guide provides comprehensive instructions for installing Grafana using various methods, configuring persistent storage, setting up SMTP for email alerts, and installing useful plugins.

##   Table of Contents

* [1. Prerequisites](#1-prerequisites)
* [2. Installation Methods](#2-installation-methods)
    * [2.1. Installing as a Service (Ubuntu/Debian)](#21-installing-as-a-service-ubuntudebian)
    * [2.2. Installing with Docker Compose](#22-installing-with-docker-compose)
    * [2.3. Installing on a Kubernetes Cluster](#23-installing-on-a-kubernetes-cluster)
* [3. Persistent Storage](#3-persistent-storage)
* [4. SMTP Configuration](#4-smtp-configuration)
* [5. Recommended Plugins](#5-recommended-plugins)
    * [5.1. Grafana OnCall](#51-grafana-oncall)
    * [5.2. Grafana LLM App](#52-grafana-llm-app)
* [6. Resource Requirements](#6-resource-requirements)

##   1. Prerequisites

Before installing Grafana, ensure you have the following prerequisites:

* **Operating System:** A compatible operating system (e.g., Linux, Windows). This guide will provide specific examples for Ubuntu/Debian.
* **System Resources:** Adequate RAM and storage. See the [Resource Requirements](#6-resource-requirements) section for details.
* **Networking:** Network connectivity to access Grafana and any data sources.
* **Docker (if using Docker Compose):** Docker Engine and Docker Compose installed.
* **Kubernetes Cluster (if installing on Kubernetes):** A running Kubernetes cluster (e.g., Minikube, EKS, AKS, GKE) and `kubectl` configured.
* **PostgreSQL:** (Optional, but Recommended): It is recommended to use PostgreSQL as the Grafana database for production environments.

##   2. Installation Methods

###   2.1. Installing as a Service (Ubuntu/Debian)

These steps are tailored for Ubuntu/Debian-based systems:

1.  **Install Dependencies:**

    ```bash
    sudo apt-get update
    sudo apt-get install -y apt-transport-https software-properties-common wget
    ```

2.  **Add Grafana Repository:**

    ```bash
    wget -q -O - [https://apt.grafana.com/gpg.key](https://apt.grafana.com/gpg.key) | sudo apt-key add -
    sudo add-apt-repository "deb [https://apt.grafana.com](https://apt.grafana.com) stable main"
    ```

3.  **Install Grafana:**

    ```bash
    sudo apt-get update
    sudo apt-get install -y grafana
    ```

4.  **Start and Enable Grafana:**

    ```bash
    sudo systemctl start grafana-server     // start the service
    sudo systemctl enable grafana-server   // this will make sure it will restart even after system restart
    ```

5.  **Verify Installation:**

    * Open a web browser and navigate to `http://your_server_ip:3000`. (e.g. localhost:3000)
    * The default login is `admin` / `admin`.  You will be prompted to change the password upon first login. You can also configure this while deploying by passing variable (`GF_SECURITY_ADMIN_USER: admin` and `GF_SECURITY_ADMIN_PASSWORD: password` - [reference](https://grafana.com/docs/grafana/latest/setup-grafana/configure-grafana/#override-configuration-with-environment-variables))

###   2.2. Installing with Docker Compose

Docker Compose simplifies the process of setting up Grafana with Docker.

1.  **Create a `docker-compose.yml` File:**

    ```yaml
    version: '3.7'
    services:
      grafana:
        image: grafana/grafana:latest
        container_name: grafana
        ports:
          - 3000:3000
        volumes:
          - grafana_data:/var/lib/grafana
        restart: unless-stopped
        environment:
          - GF_DATABASE_TYPE=postgres
          - GF_DATABASE_HOST=postgres
          - GF_DATABASE_NAME=grafana
          - GF_DATABASE_USER=grafana
          - GF_DATABASE_PASSWORD=grafana
        depends_on:
          - postgres
      postgres:
        image: postgres:14
        volumes:
          - postgres_data:/var/lib/postgresql/data
        environment:
          - POSTGRES_USER=grafana
          - POSTGRES_DB=grafana
          - POSTGRES_PASSWORD=grafana
        restart: unless-stopped
    volumes:
      grafana_data: {}
      postgres_data: {}
    ```
2.  **Start Grafana and PostgreSQL:**

    ```bash
    docker-compose up -d
    ```

3.  **Verify Installation:**

    * Open a web browser and navigate to `http://your_server_ip:3000`.
    * The default login is `admin` / `admin`.

4.  **Refer Installation:**
    * We have used docker compose to host this in a demo project you can check this out [here](https://github.com/remiges-tech/serversage-demo/blob/main/docker-compose.yml).


###   2.3. Installing on a Kubernetes Cluster

Deploying Grafana on Kubernetes provides scalability and resilience.

1.  **Create a Namespace (Optional):**

    ```bash
    kubectl create namespace monitoring
    kubectl config set-context --current --namespace=monitoring
    ```

2.  **Create a Persistent Volume Claim (PVC) for Storage:**
    *This example uses `hostPath`, which is NOT recommended for production.  Use a proper storage provider (e.g., Azure Disk, AWS EBS, NFS).*
    ```yaml
    apiVersion: v1
    kind: PersistentVolumeClaim
    metadata:
      name: grafana-pvc
      namespace: monitoring
    spec:
      accessModes:
        - ReadWriteOnce
      resources:
        requests:
          storage: 10Gi
    ```
    ```bash
    kubectl apply -f grafana-pvc.yaml
    ```

3.  **Deploy Grafana:**

    ```yaml
    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: grafana
      namespace: monitoring
    spec:
      replicas: 1
      selector:
        matchLabels:
          app: grafana
      template:
        metadata:
          labels:
            app: grafana
        spec:
          containers:
            - name: grafana
              image: grafana/grafana:latest
              ports:
                - containerPort: 3000
              volumeMounts:
                - name: grafana-storage
                  mountPath: /var/lib/grafana
              env:
                - name: GF_DATABASE_TYPE
                  value: postgres
                - name: GF_DATABASE_HOST
                  value: grafana-postgres
                - name: GF_DATABASE_NAME
                  value: grafana
                - name: GF_DATABASE_USER
                  value: grafana
                - name: GF_DATABASE_PASSWORD
                  value: grafana
              readinessProbe:
                httpGet:
                  path: /api/health
                  port: 3000
                initialDelaySeconds: 5
                periodSeconds: 5
              livenessProbe:
                httpGet:
                  path: /api/health
                  port: 3000
                initialDelaySeconds: 15
                periodSeconds: 20
          volumes:
            - name: grafana-storage
              persistentVolumeClaim:
                claimName: grafana-pvc
    ---
    apiVersion: v1
    kind: Service
    metadata:
      name: grafana
      namespace: monitoring
    spec:
      selector:
        app: grafana
      ports:
        - port: 3000
          targetPort: 3000
          nodePort: 3000 #remove this for production
      type: NodePort # Use LoadBalancer in production
    ---
    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: grafana-postgres
      namespace: monitoring
    spec:
      replicas: 1
      selector:
        matchLabels:
          app: grafana-postgres
      template:
        metadata:
          labels:
            app: grafana-postgres
        spec:
          containers:
            - name: postgres
              image: postgres:14
              ports:
                - containerPort: 5432
              volumeMounts:
                - name: postgres-storage
                  mountPath: /var/lib/postgresql/data
              env:
                - name: POSTGRES_USER
                  value: grafana
                - name: POSTGRES_DB
                  value: grafana
                - name: POSTGRES_PASSWORD
                  value: grafana
              readinessProbe:
                tcpSocket:
                  port: 5432
                initialDelaySeconds: 5
                periodSeconds: 5
              livenessProbe:
                tcpSocket:
                  port: 5432
                initialDelaySeconds: 15
                periodSeconds: 20
          volumes:
            - name: postgres-storage
              persistentVolumeClaim:
                claimName: grafana-postgres-pvc
    ---
    apiVersion: v1
    kind: PersistentVolumeClaim
    metadata:
      name: grafana-postgres-pvc
      namespace: monitoring
    spec:
      accessModes:
        - ReadWriteOnce
      resources:
        requests:
          storage: 10Gi
    ```

    ```bash
    kubectl apply -f grafana.yaml
    ```

4.  **Verify Installation:**

    * If you used a `NodePort` service, access Grafana via `http://<any-node-ip>:3000`.
    * If you used a `LoadBalancer` service, get the external IP: `kubectl get service grafana -n monitoring` and access Grafana via `http://<load-balancer-ip>:3000`.
    * The default login is `admin` / `admin`.

## 3. Persistent Storage

Grafana stores its configuration, dashboards, and other data. Persistent storage is crucial for preventing data loss in case of server restarts or pod failures.

* **For Service Installations:** The default location is `/var/lib/grafana`.  You can change this in the `grafana.ini` configuration file using the `data_path` setting.
* **For Docker Compose:** The provided `docker-compose.yml` file uses Docker volumes (`grafana_data` and `postgres_data`) to persist data.
* **For Kubernetes:** The example Kubernetes manifests use Persistent Volume Claims (PVCs) to request storage.  You'll need a configured `StorageClass` in your cluster to provision the underlying Persistent Volumes (PVs).  For production, use a cloud provider's storage solution (e.g., `aws-ebs`, `azure-disk`, `gce-pd`).  The `hostPath` example is only for development/testing and will lose data if the node is rescheduled.

##   4. SMTP Configuration

To enable Grafana to send email notifications for alerts, you need to configure an SMTP server.  This is done in the `grafana.ini` file.

1.  **Edit `grafana.ini`:**

    * For service installations, the file is typically located at `/etc/grafana/grafana.ini`.
    * For Docker, you can pass environment variables (as shown in the `docker-compose.yml` example) or mount a custom `grafana.ini` file.
    * For Kubernetes, you can use a ConfigMap to manage the `grafana.ini` file and mount it into the Grafana pod.

2.  **Configure SMTP Settings:**
    Add or modify the `[smtp]` section in `grafana.ini`:

    ```ini
    [smtp]
    enabled = true
    host = your_smtp_host:your_smtp_port
    user = your_smtp_username
    password = your_smtp_password
    from_address = your_grafana_email@example.com
    from_name = Grafana
    #skip_verify = true # Only for testing, don't use in production
    ```

    * Replace the placeholder values with your actual SMTP server details.
    * `skip_verify = true` disables SSL certificate verification.  **This is insecure and should only be used for testing purposes.** Remove or set to `false` in production.

3.  **`Caution`:** In many cases user migh needed to send it only email out side, they don't want any inbond email (e.g. no-reply@provider.com). In such scenarion configure only SMTP `host`, `from_address`, `from_name` keep the other details blank.

4.  **Restart Grafana:** Restart the Grafana server for the changes to take effect.

##   5. Recommended Plugins

Grafana's functionality can be extended with plugins. Here are a couple of useful ones:

You can use this variable whie deploying Grfana ( `GF_PLUGINS_PREINSTALL: grafana-llm-app,grafana-oncall-app` ) or follow examples below.

###   5.1. Grafana OnCall

* **Description:** Grafana OnCall is an on-call management tool that integrates with Grafana.  It helps you manage and escalate alerts, and notify the right people at the right time.
* **Installation:**
    * Download the plugin from the Grafana plugins repository.
    * Install the plugin by placing the downloaded files in the Grafana plugins directory.  The default plugin directory is usually `/var/lib/grafana/plugins`.
    * Restart Grafana.
    * Refer to the official Grafana OnCall documentation for detailed setup and configuration instructions: [https://grafana.com/docs/oncall/latest/](https://grafana.com/docs/oncall/latest/)
    * To install it on kubernates look [here](https://github.com/remiges-aniket/serversage/blob/main/Documentation/serversage/Installation/grafana-oncall-on-k8s.md).

###   5.2. Grafana LLM App

* **Description**: The Grafana LLM App allows you to use Large Language Models (LLMs) to query and visualize your metrics and logs using natural language.
* **Installation**:
    * Install the plugin using the grafana-cli tool:
      ```bash
      grafana-cli plugins install grafana-llm-app
      ```
    * Restart Grafana.
    * Configure the LLM provider and API key in the plugin settings.

##   6. Resource Requirements

The resource requirements for Grafana depend on the number of dashboards, users, and data sources. Here's a general guideline:

* **RAM:**
    * Minimum: 1GB
    * Recommended: 2GB - 4GB
* **Storage:**
    * Minimum: 200MB for Grafana itself.
    * Recommended: 10GB or more for persistent storage, depending on your data retention needs.  Consider using a dedicated database (like PostgreSQL) for larger installations.

**Important Considerations:**

* These are general recommendations.  Monitor your Grafana instance's performance and adjust resources as needed.
* For production environments, it's crucial to allocate sufficient resources to ensure stability and performance.
* When using PostgreSQL, ensure that the PostgreSQL server also has adequate resources.
* The Grafana LLM app may have higher resource requirements, especially RAM, depending on the LLM provider you use.
