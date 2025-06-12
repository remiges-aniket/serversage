#!/bin/bash
# Fix Prometheus Operator setup

echo "Step 1: Deleting Prometheus CRDs directly..."

kubectl delete -f ./crd/ -n monitoring

echo "Done! Deleting ..."