#!/bin/bash
# Fix Prometheus Operator setup

echo "Step 1: Installing Prometheus CRDs directly..."

kubectl create -f ./crd/ -n monitoring

echo "Done! Check the status with: kubectl get prometheuses -n monitoring"