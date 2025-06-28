
-------
Run otel demo app for demo in k8s:


kubectl create --namespace otel-demo -f https://raw.githubusercontent.com/open-telemetry/opentelemetry-demo/main/kubernetes/opentelemetry-demo.yaml



kubectl create --namespace otel-demo -f otel-demo-dep.yaml


kubectl --namespace default port-forward svc/frontend-proxy 8080:8080

With the frontend-proxy port-forward set up, you can access:

Web store: http://localhost:8080/
Grafana: http://localhost:8080/grafana/
Load Generator UI: http://localhost:8080/loadgen/
Jaeger UI: http://localhost:8080/jaeger/ui/
Flagd configurator UI: http://localhost:8080/feature


------------------------

docker grafana to serversage build:
docker build -t aniketxshinde/serversage:latest .

#Push it to Docker Hub 
docker push aniketxshinde/serversage:latest
