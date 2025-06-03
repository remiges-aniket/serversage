#!/bin/bash
# Fix Prometheus Operator setup

echo "Step 1: Installing Prometheus CRDs directly..."

kubectl create -f ./crd/ -n serversage

echo "Done! Check the status with: kubectl get prometheuses -n serversage"