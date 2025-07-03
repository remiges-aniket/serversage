#!/bin/bash
# Fix Elasticsearch Operator setup

echo "Step 1: Installing Elasticsearch CRDs directly..."

kubectl create -f ./crd/ -n elastic-system

echo "Done! Check the status with: kubectl -n elastic-system logs -f statefulset.apps/elastic-operator"