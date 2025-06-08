
wget https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/1.0.1/jmx_prometheus_javaagent-1.0.1.jar

# After that, you need to set an environment variable so that when the Kafka broker starts, Kafka knows that the JMX Exporter is present.
export KAFKA_OPTS="-javaagent:/opt/jmx_exporter/jmx_prometheus_javaagent.jar=7071:/opt/jmx_exporter/kafka-metrics.yml"


wget -P /opt/bitnami/jmx-exporter/ https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/1.0.1/jmx_prometheus_javaagent-1.0.1.jar

https://github.com/prometheus/jmx_exporter/releases/download/1.3.0/jmx_prometheus_standalone-1.3.0.jar

https://github.com/prometheus/jmx_exporter/releases/download/1.3.0/jmx_prometheus_javaagent-1.3.0.jar

kubectl port-forward -n kafka service/kafka-headless 9404:9404


