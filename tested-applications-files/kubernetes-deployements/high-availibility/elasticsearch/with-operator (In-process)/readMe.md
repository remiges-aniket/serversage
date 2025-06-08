# Create namespace for ECK operator
kubectl create namespace elastic-system

# Install ECK custom resource definitions
kubectl apply -f https://download.elastic.co/downloads/eck/2.8.0/crds.yaml

# Install the operator with RBAC rules
kubectl apply -f https://download.elastic.co/downloads/eck/2.8.0/operator.yaml



kubectl delete -f https://download.elastic.co/downloads/eck/2.8.0/crds.yaml 
kubectl delete -f https://download.elastic.co/downloads/eck/2.8.0/operator.yaml


32gb - 8core

64gb - 16core


2-replica



16-gb - 4
32-gb - 8 
core