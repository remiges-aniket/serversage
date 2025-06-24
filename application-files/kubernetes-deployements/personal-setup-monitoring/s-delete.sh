#!/bin/bash
# Fix Prometheus Operator setup

echo "Step 1: Installing Prometheus CRDs directly..."

./6.delete-crd.sh

kubectl delete -f minio-deployment.yaml

kubectl delete -f 1.prometheus-dep.yaml
kubectl delete -f 2.pv-pvc-prometheus.yaml
kubectl delete -f 3.service-monitors.yaml
kubectl delete -f 4.additional-scrap-config.yaml

kubectl delete -f grafana-simple-dep.yaml

# echo "Grafana installed : http://localhost:30016"


kubectl delete -f kube-metrics-server.yaml
kubectl delete -f kube-state-metrics.yaml
kubectl apply -f prometheus-deployement.yaml

kubectl delete namespace prometheus
kubectl delete namespace database
kubectl delete namespace monitoring

kubectl get all -A -o wide
#kubectl delete -f kube-metrics-server.yaml

#kubectl delete -f kube-metrics-server.yaml

echo "Setup is Deleted!!!!"