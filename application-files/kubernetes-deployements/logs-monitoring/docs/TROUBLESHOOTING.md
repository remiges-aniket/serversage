# Troubleshooting Guide

This guide provides solutions for common issues with the log aggregation system.

## Checking Component Status

First, check if all components are running:

```bash
kubectl get pods -n logging
```

All pods should be in the `Running` state. If not, check the pod details:

```bash
kubectl describe pod -n logging <pod-name>
```

## Common Issues

### Fluent Bit Issues

#### Fluent Bit Not Collecting Logs

**Symptoms:**
- No logs appearing in Grafana
- Fluent Bit pods showing errors

**Solutions:**

1. Check Fluent Bit logs:
   ```bash
   kubectl logs -n logging -l app=fluent-bit
   ```

2. Verify the configuration:
   ```bash
   kubectl get configmap -n logging fluent-bit-config -o yaml
   ```

3. Ensure Fluent Bit has proper permissions:
   ```bash
   kubectl describe clusterrolebinding fluent-bit
   ```

4. Check if the log paths are correct in the Fluent Bit configuration.

#### High CPU/Memory Usage

If Fluent Bit is using too many resources:

1. Adjust the `Mem_Buf_Limit` in the Fluent Bit configuration
2. Consider filtering out unnecessary logs
3. Increase resource limits in the DaemonSet specification

### Loki Issues

#### Loki Not Receiving Logs

**Symptoms:**
- Fluent Bit is running but no logs in Loki
- Loki showing connection errors

**Solutions:**

1. Check Loki logs:
   ```bash
   kubectl logs -n logging -l app=loki
   ```

2. Verify the Loki service is running:
   ```bash
   kubectl get svc -n logging loki
   ```

3. Check if Fluent Bit can resolve and connect to Loki:
   ```bash
   kubectl exec -it -n logging <fluent-bit-pod> -- nslookup loki.logging.svc.cluster.local
   kubectl exec -it -n logging <fluent-bit-pod> -- wget -O- http://loki:3100/ready
   ```

#### Loki Out of Memory

If Loki is crashing with OOM errors:

1. Increase memory limits in the Loki deployment
2. Adjust retention settings to store fewer logs
3. Consider using object storage instead of local storage

### Grafana Issues

#### Cannot Access Grafana

**Symptoms:**
- Port-forwarding works but UI doesn't load
- Authentication issues

**Solutions:**

1. Check Grafana logs:
   ```bash
   kubectl logs -n logging -l app=grafana
   ```

2. Verify the Grafana service:
   ```bash
   kubectl get svc -n logging grafana
   ```

3. Reset admin password if needed:
   ```bash
   kubectl exec -it -n logging <grafana-pod> -- grafana-cli admin reset-admin-password admin
   ```

#### No Data in Grafana

If Grafana is running but shows no data:

1. Check if the Loki data source is configured correctly
2. Verify Loki is accessible from Grafana:
   ```bash
   kubectl exec -it -n logging <grafana-pod> -- wget -O- http://loki:3100/ready
   ```

3. Test queries directly in the Explore view

## Diagnostic Commands

### Check All Components

```bash
./scripts/verify.sh
```

### Test Log Generation

```bash
kubectl apply -f manifests/test-apps/test-logger.yaml
kubectl logs -l app=test-logger
```

### Check Loki Metrics

```bash
kubectl port-forward -n logging svc/loki 3100:3100
curl http://localhost:3100/metrics
```

## Getting Support

If you're still experiencing issues:

1. Collect diagnostic information:
   ```bash
   kubectl get all -n logging
   kubectl describe pods -n logging
   kubectl logs -n logging -l app=fluent-bit --tail=100
   kubectl logs -n logging -l app=loki --tail=100
   kubectl logs -n logging -l app=grafana --tail=100
   ```

2. Check for resource constraints:
   ```bash
   kubectl top pods -n logging
   ```

3. Reach out to the community or support with this information
