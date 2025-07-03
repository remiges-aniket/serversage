#!/bin/bash
# Fix Elasticsearch Operator setup

echo "Step 1: Downloading Elasticsearch CRDs directly..."
curl -o crds.yaml https://download.elastic.co/downloads/eck/3.0.0/crds.yaml
curl -o operator.yaml https://download.elastic.co/downloads/eck/3.0.0/operator.yaml

echo "Done! Downloading crd's ..... "
