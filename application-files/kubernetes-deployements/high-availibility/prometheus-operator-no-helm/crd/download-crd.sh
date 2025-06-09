#!/bin/bash
# Fix Prometheus Operator setup

echo "Step 1: Downloading Prometheus CRDs directly..."
curl -o alertmanagerconfigs.yaml https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.71.0/example/prometheus-operator-crd/monitoring.coreos.com_alertmanagerconfigs.yaml
curl -o alertmanagers.yaml https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.71.0/example/prometheus-operator-crd/monitoring.coreos.com_alertmanagers.yaml
curl -o podmonitors.yaml https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.71.0/example/prometheus-operator-crd/monitoring.coreos.com_podmonitors.yaml
curl -o probes.yaml https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.71.0/example/prometheus-operator-crd/monitoring.coreos.com_probes.yaml
curl -o prometheusagents.yaml https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.71.0/example/prometheus-operator-crd/monitoring.coreos.com_prometheusagents.yaml
curl -o prometheuses.yaml https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.71.0/example/prometheus-operator-crd/monitoring.coreos.com_prometheuses.yaml
curl -o prometheusrules.yaml https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.71.0/example/prometheus-operator-crd/monitoring.coreos.com_prometheusrules.yaml
curl -o scrapeconfigs.yaml https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.71.0/example/prometheus-operator-crd/monitoring.coreos.com_scrapeconfigs.yaml
curl -o servicemonitors.yaml https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.71.0/example/prometheus-operator-crd/monitoring.coreos.com_servicemonitors.yaml
curl -o thanosrulers.yaml https://raw.githubusercontent.com/prometheus-operator/prometheus-operator/v0.71.0/example/prometheus-operator-crd/monitoring.coreos.com_thanosrulers.yaml

echo "Done! Downloading crd's ..... "