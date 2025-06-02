#!/bin/bash
# Fix Prometheus Operator setup

echo "Step 1: Installing Prometheus CRDs directly..."

./0.init-crd.sh

kubectl create namespace prometheus
kubectl create namespace database
kubectl create namespace monitoring

kubectl label namespace prometheus monitoring=prometheus
kubectl label namespace database monitoring=prometheus
kubectl label namespace monitoring monitoring=prometheus


kubectl apply -f minio-deployment.yaml

kubectl apply -f 1.prometheus-dep.yaml
kubectl apply -f 2.pv-pvc-prometheus.yaml
kubectl apply -f 3.service-monitors.yaml
kubectl apply -f additional-scrap-config.yaml


echo "Thanos installed : http://localhost:30017"
echo "Prometheus installed : http://localhost:30018"


kubectl apply -f grafana-simple-dep.yaml

echo "Grafana installed : http://localhost:30016"


kubectl apply -f kube-metrics-server.yaml
kubectl apply -f kube-state-metrics.yaml

kubectl get all -A -o wide
#kubectl apply -f kube-metrics-server.yaml

#kubectl apply -f kube-metrics-server.yaml

echo "Setup is up!!!!"