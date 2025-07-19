# Setup Guide

This guide explains how to set up the log aggregation system in your Kubernetes cluster.

## Prerequisites

- Kubernetes cluster (v1.16+)
- kubectl configured to access your cluster
- Sufficient permissions to create resources in the cluster

## Installation Steps

### 1. Clone the Repository

```bash
git clone <repository-url>
cd log-aggregator
```

### 2. Deploy the Stack

The simplest way to deploy the entire stack is to use the provided deployment script:

```bash
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

This script will:
- Create a `logging` namespace if it doesn't exist
- Deploy Loki (log storage)
- Deploy Fluent Bit (log collector)
- Deploy Grafana (visualization)
- Wait for all components to be ready

### 3. Access Grafana

To access the Grafana dashboard:

```bash
kubectl -n logging port-forward svc/grafana 3000:3000
```

Then open your browser and navigate to http://localhost:3000

Default login credentials:
- Username: admin
- Password: admin

You will be prompted to change the password on first login.

### 4. Test the Setup

To verify that logs are being collected properly, you can deploy a test application:

```bash
kubectl apply -f manifests/test-apps/test-logger.yaml
```

After a few moments, you should see logs from this application in Grafana.

## Manual Installation

If you prefer to deploy components individually:

### Deploy Loki

```bash
kubectl apply -f manifests/loki/
```

### Deploy Fluent Bit

```bash
kubectl apply -f manifests/fluent-bit/
```

### Deploy Grafana

```bash
kubectl apply -f manifests/grafana/
```

## Next Steps

- Configure [alerts](ALERTING.md)
- Customize [configurations](CONFIGURATION.md)
- Learn about [troubleshooting](TROUBLESHOOTING.md)
