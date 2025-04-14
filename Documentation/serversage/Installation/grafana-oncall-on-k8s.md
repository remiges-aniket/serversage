Okay, let's translate the Grafana OnCall related parts of your Docker Compose setup into Kubernetes manifests.

You have three core OnCall components in your Docker Compose:
1.  `oncall`: The main web application.
2.  `celery`: The background task worker.
3.  `oncall_db_migration`: A one-time job to initialize/migrate the database.
4.  `redis`: A dependency for the message broker used by `oncall` and `celery`.

We'll create Kubernetes resources for these:
*   `Deployment` for `oncall`.
*   `Deployment` for `celery`.
*   `Job` for `oncall_db_migration`.
*   `Deployment` for `redis`.
*   `Service` for `oncall` to expose it internally (and potentially externally later).
*   `Service` for `redis` to be reachable by `oncall` and `celery`.
*   `PersistentVolumeClaim` (PVC) for storing OnCall data (replacing the bind mount `./data/oncall`).
*   `PersistentVolumeClaim` (PVC) for storing Redis data.
*   `Secret` to securely store the `SECRET_KEY`.
*   `ConfigMap` (optional but good practice) to store non-sensitive configuration.

**Important Considerations:**

1.  **Database (SQLite):** Your Docker Compose uses `DATABASE_TYPE: sqlite3`. **This is NOT recommended for Kubernetes deployments with multiple replicas or even for reliable single-instance setups.** SQLite relies on file locking on a shared filesystem, which behaves poorly or is unsupported with many Kubernetes volume types (especially network storage) and doesn't scale.
    *   **Recommendation:** Switch to PostgreSQL or MySQL for OnCall in Kubernetes. You already have Postgres in your stack for Keycloak, so using that would be ideal.
    *   **For this translation:** I will initially stick to SQLite to directly map your compose file, but **strongly advise you to switch**. I'll include notes on where to change things for Postgres.
2.  **Networking:** Services in Kubernetes communicate via their `Service` names within the same namespace (e.g., `redis-service`, `grafana-service`). Environment variables referencing other services need to use these Kubernetes service names.
3.  **Persistent Storage:** Kubernetes uses PersistentVolumes (PV) and PersistentVolumeClaims (PVC). You need a StorageClass configured in your cluster that can dynamically provision volumes (most managed Kubernetes offerings have this). We'll use PVCs assuming dynamic provisioning. The `accessModes` for SQLite *must* be `ReadWriteOnce` (RWO) because only one pod can safely write to it at a time.
4.  **Secrets:** Never hardcode secrets in manifests. We'll create a Kubernetes `Secret`.
5.  **Grafana URL:** The `GRAFANA_API_URL` must point to the Kubernetes service name and port for your Grafana deployment (e.g., `http://grafana-service.your-namespace.svc.cluster.local:3000` or simpler `http://grafana-service:3000` if in the same namespace). Make sure you have a Service for Grafana.
6.  **BASE_URL:** This should be the externally accessible URL for OnCall (e.g., how users access it via an Ingress or LoadBalancer), not the internal service name.

---

**Step 1: Create a Namespace (Recommended)**

```bash
kubectl create namespace oncall
# Or use the namespace where the rest of your observability stack resides
```

**Step 2: Create the Secret**

Create a file `oncall-secret.yaml`:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: oncall-secrets
  namespace: oncall # Or your target namespace
type: Opaque
stringData:
  # IMPORTANT: Replace this with a strong, randomly generated key!
  ONCALL_SECRET_KEY: "change-me-to-a-very-long-and-random-secret-key-32-chars-plus"
  # If you switch to Postgres, add DB credentials here:
  # POSTGRES_USER: "oncalluser"
  # POSTGRES_PASSWORD: "replace-with-strong-password"
```

Apply it:

```bash
kubectl apply -f oncall-secret.yaml -n oncall # Or your target namespace
```

**Step 3: Create ConfigMap (Optional, but Recommended)**

Create a file `oncall-configmap.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: oncall-config
  namespace: oncall # Or your target namespace
data:
  # --- Core Settings ---
  # !! WARNING !! SQLite is NOT recommended for Kubernetes. Switch to 'postgres' or 'mysql'.
  DATABASE_TYPE: "sqlite3"
  # If using Postgres:
  # DATABASE_TYPE: "postgres"
  # POSTGRES_HOST: "your-postgres-service-name" # e.g., postgres-service, or the service name of your existing postgres
  # POSTGRES_PORT: "5432"
  # POSTGRES_DB: "oncall" # Choose a database name

  BROKER_TYPE: "redis"
  REDIS_URI: "redis://redis-service:6379/0" # Points to the Redis Service we'll create

  # !! IMPORTANT !! Update this to the URL users will use to access OnCall
  BASE_URL: "http://oncall.example.com" # Replace with your actual external URL (e.g., via Ingress)

  DJANGO_SETTINGS_MODULE: "settings.hobby" # Or settings.production if configured

  # --- Grafana Integration ---
  # !! IMPORTANT !! Update this to your Grafana Service name and port
  GRAFANA_API_URL: "http://grafana-service:3000" # Replace grafana-service if yours is named differently

  # --- Celery Settings (Match your docker-compose) ---
  CELERY_WORKER_QUEUE: "default,critical,long,slack,telegram,webhook,retry,celery,grafana"
  CELERY_WORKER_CONCURRENCY: "1" # Adjust based on node resources if needed
  CELERY_WORKER_MAX_TASKS_PER_CHILD: "100"
  CELERY_WORKER_SHUTDOWN_INTERVAL: "65m"
  CELERY_WORKER_BEAT_ENABLED: "True" # Run beat scheduler within Celery worker

  # --- Other Features ---
  FEATURE_PROMETHEUS_EXPORTER_ENABLED: "false" # Set to true if needed
  PROMETHEUS_EXPORTER_SECRET: "prometheus" # Matches default from compose
```

Apply it:

```bash
kubectl apply -f oncall-configmap.yaml -n oncall # Or your target namespace
```

**Step 4: Create PersistentVolumeClaims**

Create a file `oncall-pvcs.yaml`:

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: oncall-data-pvc
  namespace: oncall # Or your target namespace
spec:
  accessModes:
    # ReadWriteOnce is required for SQLite.
    # If using Postgres, OnCall app might work with ReadWriteMany if needed,
    # but the DB itself will likely need ReadWriteOnce.
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi # Adjust size as needed
  # Optional: Specify a StorageClass if you don't want the default
  # storageClassName: "your-storage-class"
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: redis-data-pvc
  namespace: oncall # Or your target namespace
spec:
  accessModes:
    - ReadWriteOnce # Suitable for single-instance Redis
  resources:
    requests:
      storage: 1Gi # Adjust size as needed
  # Optional: Specify a StorageClass
  # storageClassName: "your-storage-class"
```

Apply it:

```bash
kubectl apply -f oncall-pvcs.yaml -n oncall # Or your target namespace
```

**Step 5: Create Redis Deployment and Service**

Create a file `redis-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis-deployment
  namespace: oncall # Or your target namespace
  labels:
    app: redis
spec:
  replicas: 1 # Redis setup here is basic, not HA
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7.0.15 # Match your docker-compose version
        ports:
        - containerPort: 6379
          name: redis
        volumeMounts:
        - name: redis-data
          mountPath: /data
        resources: # Add resource limits/requests
          requests:
            memory: "256Mi"
            cpu: "100m"
          limits:
            memory: "500Mi" # Match your docker-compose limit
            cpu: "500m"    # Match your docker-compose limit
        readinessProbe:
          tcpSocket:
            port: 6379
          initialDelaySeconds: 5
          periodSeconds: 10
        livenessProbe:
          tcpSocket:
            port: 6379
          initialDelaySeconds: 15
          periodSeconds: 20
      volumes:
      - name: redis-data
        persistentVolumeClaim:
          claimName: redis-data-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: redis-service
  namespace: oncall # Or your target namespace
  labels:
    app: redis
spec:
  ports:
  - port: 6379
    targetPort: redis
    name: redis
  selector:
    app: redis
  type: ClusterIP # Only needs to be reachable internally
```

Apply it:

```bash
kubectl apply -f redis-deployment.yaml -n oncall # Or your target namespace
```

**Step 6: Create the Database Migration Job**

Create a file `oncall-migration-job.yaml`:

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: oncall-db-migration
  namespace: oncall # Or your target namespace
spec:
  template:
    spec:
      containers:
      - name: oncall-migration
        image: grafana/oncall:latest # Use the same image version as your app/celery
        # user: root # Not needed unless specific file permissions require it
        command: ["python", "manage.py", "migrate", "--noinput"]
        envFrom:
          - configMapRef:
              name: oncall-config
          - secretRef:
              name: oncall-secrets
        # If using Postgres, ensure the DB is ready before migrating.
        # An initContainer could wait for the Postgres service/port.
        volumeMounts:
        - name: oncall-data
          mountPath: /var/lib/oncall # Mount the volume where SQLite DB resides
          # If using Postgres, this volume mount might not be needed for the *migration job* itself.
      volumes:
      - name: oncall-data
        persistentVolumeClaim:
          claimName: oncall-data-pvc
      restartPolicy: OnFailure # Retry if migration fails
  backoffLimit: 4 # Number of retries
```

Apply it:

```bash
kubectl apply -f oncall-migration-job.yaml -n oncall # Or your target namespace
```

**Wait for the migration job to complete successfully** before proceeding:

```bash
kubectl get job oncall-db-migration -n oncall --watch
# Wait until SUCCEEDED is 1
```
*(You might need Ctrl+C to exit the watch)*

**Step 7: Create OnCall Deployment and Service**

Create a file `oncall-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: oncall-deployment
  namespace: oncall # Or your target namespace
  labels:
    app: oncall
spec:
  # Using SQLite? replicas MUST be 1.
  # Using Postgres/MySQL? You can increase replicas.
  replicas: 1
  selector:
    matchLabels:
      app: oncall
  template:
    metadata:
      labels:
        app: oncall
    spec:
      # initContainers: # Optional: Wait for migration Job completion explicitly if needed
      #   - name: wait-for-migration
      #     image: busybox:1.28
      #     command: ['sh', '-c', 'until kubectl get job oncall-db-migration -n oncall -o jsonpath="{.status.succeeded}" | grep -q 1; do echo "Waiting for migration job..."; sleep 5; done;']
      containers:
      - name: oncall
        image: grafana/oncall:latest # Match migration job version
        # user: root # If needed, but try without first
        command: ["sh", "-c", "uwsgi --ini uwsgi.ini"]
        ports:
        - containerPort: 8080
          name: http
        envFrom:
          - configMapRef:
              name: oncall-config
          - secretRef:
              name: oncall-secrets
        volumeMounts:
        - name: oncall-data
          mountPath: /var/lib/oncall # Mount volume for SQLite DB and potential other state
        resources: # Add resource limits/requests
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "1"
        readinessProbe:
          httpGet:
            path: /healthz/ # OnCall has a health check endpoint
            port: http
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        livenessProbe:
          httpGet:
            path: /healthz/
            port: http
          initialDelaySeconds: 60
          periodSeconds: 20
          timeoutSeconds: 5
          failureThreshold: 3
      volumes:
      - name: oncall-data
        persistentVolumeClaim:
          claimName: oncall-data-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: oncall-service
  namespace: oncall # Or your target namespace
  labels:
    app: oncall
spec:
  ports:
  - port: 80 # External port for the service (can be anything)
    targetPort: http # Matches containerPort name 'http' (or 8080)
    protocol: TCP
    name: http
  selector:
    app: oncall
  # Type: ClusterIP (default) - Only reachable inside the cluster.
  # Use LoadBalancer for cloud provider LB, NodePort for specific node access,
  # or configure an Ingress controller to expose it via the BASE_URL.
  type: ClusterIP
```

Apply it:

```bash
kubectl apply -f oncall-deployment.yaml -n oncall # Or your target namespace
```

**Step 8: Create Celery Worker Deployment**

Create a file `celery-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: celery-deployment
  namespace: oncall # Or your target namespace
  labels:
    app: celery
spec:
  # Using SQLite? replicas SHOULD ideally be 1 unless tasks are idempotent and don't touch DB heavily.
  # Using Postgres/MySQL? You can increase replicas.
  replicas: 1
  selector:
    matchLabels:
      app: celery
  template:
    metadata:
      labels:
        app: celery
    spec:
      containers:
      - name: celery
        image: grafana/oncall:latest # Match other components
        # user: root # If needed
        command: ["sh", "-c", "./celery_with_exporter.sh"] # Command from docker-compose
        envFrom:
          - configMapRef:
              name: oncall-config
          - secretRef:
              name: oncall-secrets
        volumeMounts:
        - name: oncall-data # Mount data volume if celery tasks need access (e.g., to SQLite DB)
          mountPath: /var/lib/oncall
        resources: # Add resource limits/requests
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "1"
        # Liveness/Readiness probes for Celery are more complex.
        # Could check for process existence or use custom health checks if available.
        # Skipping basic probes for now.
      volumes:
      - name: oncall-data
        persistentVolumeClaim:
          claimName: oncall-data-pvc
```

Apply it:

```bash
kubectl apply -f celery-deployment.yaml -n oncall # Or your target namespace
```

---

**Summary & Next Steps:**

1.  **Apply Manifests:** Apply all the created YAML files (`oncall-secret.yaml`, `oncall-configmap.yaml`, `oncall-pvcs.yaml`, `redis-deployment.yaml`, `oncall-migration-job.yaml`, `oncall-deployment.yaml`, `celery-deployment.yaml`) using `kubectl apply -f <filename> -n oncall`. Remember to wait for the migration job.
2.  **Check Pods:** Verify all pods are running: `kubectl get pods -n oncall`. Check logs if needed: `kubectl logs <pod-name> -n oncall -f`.
3.  **Access OnCall:**
    *   Since the service type is `ClusterIP`, OnCall is only reachable *within* the cluster (e.g., from your Grafana pod at `http://oncall-service.oncall.svc.cluster.local:80` or `http://oncall-service:80` if Grafana is in the `oncall` namespace).
    *   To access it from your browser, you'll need to:
        *   **Port-forward (for testing):** `kubectl port-forward svc/oncall-service 8080:80 -n oncall` (Access via `http://localhost:8080`).
        *   **Change Service Type:** Edit `oncall-service` to `type: LoadBalancer` (if your cloud provider supports it) or `type: NodePort`.
        *   **Configure Ingress:** This is the recommended production approach. Set up an Ingress controller (like Nginx Ingress) and create an Ingress resource pointing to `oncall-service` using the hostname defined in `BASE_URL`.
4.  **Grafana Plugin Configuration:** Ensure the OnCall plugin within Grafana is configured to point to the *internal* Kubernetes service URL: `http://oncall-service.oncall.svc.cluster.local:80` (or the appropriate namespace/port). The `GRAFANA_API_URL` in the OnCall config should point to the Grafana *internal* service URL.
5.  **!!! SWITCH FROM SQLite !!!:** Seriously consider migrating to PostgreSQL. You would need to:
    *   Create a dedicated Postgres database and user for OnCall (or use your existing instance).
    *   Update the `oncall-config` ConfigMap with `DATABASE_TYPE: "postgres"` and the `POSTGRES_*` variables.
    *   Add the Postgres user/password to the `oncall-secrets` Secret.
    *   Ensure the OnCall pods can reach the Postgres service.
    *   You could potentially remove the `oncall-data-pvc` volume mount from the Deployments/Job *if* OnCall stores no other essential state on the filesystem once using Postgres (verify this in OnCall docs).
    *   You can then safely increase replica counts for `oncall-deployment` and `celery-deployment`.

This provides a solid starting point for running Grafana OnCall on Kubernetes based on your Docker Compose setup. Remember to adapt networking, storage, and especially the database choice for a production-ready environment.