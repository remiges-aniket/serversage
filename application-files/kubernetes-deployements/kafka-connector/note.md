The ElasticsearchSinkConnector is bundled with confluentinc/cp-kafka-connect images.

You must have created the Kafka topics _connect-configs, _connect-offsets, and _connect-status beforehand:


kafka-topics.sh --bootstrap-server kafka.kafka.svc.cluster.local:9092 --create --topic _connect-configs --replication-factor 1 --partitions 1
kafka-topics.sh --bootstrap-server kafka.kafka.svc.cluster.local:9092 --create --topic _connect-offsets --replication-factor 1 --partitions 1
kafka-topics.sh --bootstrap-server kafka.kafka.svc.cluster.local:9092 --create --topic _connect-status --replication-factor 1 --partitions 1


