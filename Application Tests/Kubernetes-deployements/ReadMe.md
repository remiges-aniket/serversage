
## create & delete cluster:

```sh
kind create cluster --name first-cluster --config=./first-cluster.yml
kind delete cluster --name first-cluster
```

## create namespaces:

```sh
kubectl create namespace database
kubectl create namespace logging
kubectl create namespace monitoring
kubectl create namespace logharbour
kubectl create namespace elastic
kubectl create namespace grafana

```
## Open ports list for cluster `first-cluster`:

```sh
    hostPort: 85, 445, 30012 to 30019  # ports open from cluster to outside
    protocol: TCP 
       
```

## Ports in use my system:

```sh
elastic -   30012
redis -     30013
postgres -  30014
Minio -     30015
Grafana -   30016

empty ports:
30017
30018
30019
```


