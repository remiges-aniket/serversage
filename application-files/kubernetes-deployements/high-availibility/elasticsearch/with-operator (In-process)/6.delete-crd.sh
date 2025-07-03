#!/bin/bash
# Fix Elasticsearch Operator setup

echo "Step 1: Deleting Elasticsearch CRDs directly..."

kubectl delete -f ./crd/ -n elastic-system

echo "Done! Deleting ..."