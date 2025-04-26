
kind k8s
1) singleNodeCluster

## --------------------- If port 80 is busy then do below in local system ----------------------------------------

sudo systemctl start lighttpd
sudo systemctl stop lighttpd

## --------------------- Install vi in docker container ----------------------------------------
apt-get update
apt-get install vim -y

apt-get install telnet
(telnet my-elasticsearch.default.svc.cluster.local 9200)

apt-get install netcat-traditional
nc -zv my-elasticsearch.default.svc.cluster.local 9200
## --------------------------------------- Auto comands ----------------------------------------
kubectl apply -f fluent-bit-config.yaml
kubectl get pods -n monitoring -o wide
kubectl delete pod $(kubectl get pods -n monitoring -o jsonpath='{.items[0].metadata.name}') -n monitoring
sleep 10
kubectl logs $(kubectl get pods -n monitoring -o jsonpath='{.items[0].metadata.name}') -n monitoring


## --------------------------------------- Auto in My System --------------------------------------------------------

kind delete cluster --name tws-cluster
sleep 5
kind delete cluster
sleep 5
kind create cluster --name tws-cluster --config=/home/aniket/Documents/aniket\ github/personal/k8s/kind-cluster-config/tws-cluster-config.yml
sleep 5
kubectl apply -f /home/aniket/Documents/aniket\ github/Aniket-public-Serversage/Application\ Tests/fluentbit\ to\ es\ k8s/elasticsearch/elastic-dep.yaml
sleep 10
kubectl get pods
cd ../../../Aniket-public-Serversage/Application\ Tests/fluentbit\ to\ es\ k8s/fluentbit-dep/
kubectl apply -f namespace.yaml -f fluent-bit-config.yaml -f fluent-bit-deployment.yaml -f fluent-bit-rbac.yaml
kubectl get pods -n monitoring -o wide
kubectl delete pod $(kubectl get pods -n monitoring -o jsonpath='{.items[0].metadata.name}') -n monitoring
sleep 10
kubectl get pods -n monitoring -o wide
kubectl logs $(kubectl get pods -n monitoring -o jsonpath='{.items[0].metadata.name}') -n monitoring
cd /home/aniket/Documents/aniket\ github/personal/k8s/kind-cluster-config/


## -----------------------------------------------------------------------------------------------

kind delete cluster --name tws-cluster
sleep 5
kind delete cluster
sleep 5
kind create cluster --name tws-cluster --config=/home/aniket/Documents/aniket\ github/personal/k8s/kind-cluster-config/tws-cluster-config.yml
sleep 5
kubectl apply -f /home/aniket/Documents/aniket\ github/Aniket-public-Serversage/Application\ Tests/fluentbit\ to\ es\ k8s/elasticsearch/elastic-dep.yaml
sleep 10
kubectl get pods
cd ../../../Aniket-public-Serversage/Application\ Tests/fluentbit\ to\ es\ k8s/fluentbit-dep/
kubectl apply -f namespace.yaml -f fluent-bit-config.yaml -f fluent-bit-deployment.yaml -f fluent-bit-rbac.yaml
kubectl get pods -n monitoring -o wide
kubectl delete pod $(kubectl get pods -n monitoring -o jsonpath='{.items[0].metadata.name}') -n monitoring
sleep 10
kubectl get pods -n monitoring -o wide
kubectl logs $(kubectl get pods -n monitoring -o jsonpath='{.items[0].metadata.name}') -n monitoring
cd /home/aniket/Documents/aniket\ github/personal/k8s/kind-cluster-config/


kubectl apply -f fluent-bit-config.yaml 
kubectl get pods -n monitoring -o wide
kubectl delete pod $(kubectl get pods -n monitoring -o jsonpath='{.items[0].metadata.name}') -n monitoring
sleep 10
kubectl get pods -n monitoring -o wide
sleep 5
kubectl logs $(kubectl get pods -n monitoring -o jsonpath='{.items[0].metadata.name}') -n monitoring
kubectl port-forward $(kubectl get pods -n monitoring -o jsonpath='{.items[0].metadata.name}') 24224:24224 -n monitoring



-----------------------------------------------------------------------------------------------
kubectl apply -f namespace.yaml -f fluent-bit-config.yaml -f fluent-bit-deployment.yaml -f fluent-bit-rbac.yaml

kubectl apply -f namespace.yaml -f fluent-bit-configmap.yaml -f fluent-bit-daemonset.yaml -f fluent-bit-rbac.yaml





## --------------------------------------------- Port forwarding ----------------
kubectl port-forward svc/my-elasticsearch-coordinating-hl 9200:9200

kubectl port-forward svc/elasticsearch 9200:9200 
kubectl port-forward pods/fluent-bit-7b694c4fd6-5qht2 24224:24224 -n monitoring

kubectl port-forward $(kubectl get pods -n monitoring -o jsonpath='{.items[0].metadata.name}') 24224:24224 -n monitoring
--------------------------- Create / Delete cluster ----------------------------------
kind create cluster --name tws-cluster --config=tws-cluster-config.yml
kind delete cluster --name tws-cluster

kubectl port-forward service/prometheus-service -n prometheus 9090:9091 --address=0.0.0.0


---------------------------------------------------------------------------------------
aniket@Remigesadm:~/Documents/aniket github/personal/k8s/kind-cluster-config$ cat tws-cluster-config.yml 
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4

nodes:
  - role: control-plane
    image: kindest/node:v1.32.2
  - role: worker
    image: kindest/node:v1.32.2
  - role: worker
    image: kindest/node:v1.32.2
  - role: worker
    image: kindest/node:v1.32.2
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP # It's good practice to specify the protocol
      - containerPort: 443
        hostPort: 443
        protocol: TCP # It's good practice to specify the protocol
------




-------------------------------------------------------------
kubectl version --client
kubectl get nodes
uname -m   // print the machine hardware name (x86_64)
kind create cluster --name prometheus
kind get clusters

kubectl cluster-info --context kind-kind

kind delete cluster f8a290b732f3

# get nodes from another context (e.g. kind, minukube)
kubectl get nodes --context kind-tws-cluster

# kubectl set default context to :-
kubectl config use-context  kind-tws-cluster

kubectl get nodes -o wide

# watch command for every 2 sec's
watch kubectl get nodes -o wide
## ---------------------------------- Debug ------------------------------------------------
# Describe pod- with process steps
kubectl describe pod prometheus -n prometheus

# Logs
kubectl logs pod/demo-job-dblbq -n prometheus

------------------------------------------------------------------------------------------------
# Namespaces for isolation
# get all namespaces
kubectl get namespaces/ns

# get all pods from default ns
kubectl get pods

# get pods from specific ns
kubectl get pods -n prometheus
--------------------------------------- Exec ---------------------------------------------------------
# exec into the pod:
kubectl exec -it pod/prometheus -n prometheus -- bash

kubectl exec -it prometheus-pod -n prometheus -- sh
------------------------------------------------------------------------------------------------
# create namespace
kubectl create namespace/ns nginx

# create nginx pod
kubectl run ngnix --image=nginx
# delete pod
kubectl delete pod nginx

# create nginx pod in the namespace
kubectl run ngnix --image=nginx -n ngnix

kubectl run ngnix --image=prometheus

# create pod using yaml file:
kubectl apply -f namespace.yml



# running image container and remove container it after use while keep console connection.
docker run --rm -it prom/prometheus:latest

# Labels and Selector:

# get deployment in a namespace
kubectl get deployment -n prometheus

# scale deployment replicas
kubectl scale deployment/prometheus-deployment -n prometheus --replicas=5

# Update deployment image ( this also known as rolling updates)
kubectl set image deployment/prometheus-deployement -n prometheus prometheus=prom/prometheus:v3.2.1


# job
kubectl apply -f job.yml

kubectl get job -n prometheus

kubectl delete -f job.yml

# cron-job

kubectl apply -f cron-job.yml

kubectl get cron-job -n prometheus

kubectl delete -f cron-job.yml

# persistant volume

kubectl get pv

# StoregeClassname: this is the storage where you can store persistent data. (https://kubernetes.io/docs/concepts/storage/storage-classes/#provisioner)










What These Mean in Elasticsearch on Kubernetes Context
Pods = Elasticsearch Nodes
In Kubernetes, each Elasticsearch node runs inside a Pod. Different node roles (master, data, ingest, coordinating) can be deployed as separate StatefulSets or Deployments, each with their own headless service for discovery.

Headless Services (ClusterIP: None)
These services do not have a cluster IP but provide DNS entries for the Pods, enabling Elasticsearch nodes to discover each other and form a cluster.

Coordinating Nodes
These nodes act as query routers and load balancers but do not hold data or cluster state.

Data Nodes
Responsible for indexing and storing data shards.

Ingest Nodes
Handle pre-processing pipelines for documents before indexing.

Master Nodes
Manage cluster-wide actions such as creating/deleting indices and managing cluster state.

Main Client Service (my-elasticsearch)
Exposes the cluster to clients (applications, Kibana) and load balances requests to coordinating nodes.

































