apiVersion: v1
kind: ConfigMap
metadata:
  name: fluent-bit-config
  namespace: monitoring
data:
  fluent-bit.conf: |
    [SERVICE]
        Flush        1
        Log_Level    info
        Daemon       off

    [INPUT]
        Name              tail
        Path              /var/log/containers/*.log
        Parser            docker
        Tag               kube.*
        Mem_Buf_Limit     5MB
        Skip_Long_Lines   On

    [FILTER]
        Name              kubernetes
        Match             kube.*
        Kube_URL          https://kubernetes.default.svc:443
        Kube_CA_File      /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
        Kube_Token_File   /var/run/secrets/kubernetes.io/serviceaccount/token
        Kube_Tag_Prefix   kube.var.log.containers.
        Merge_Log         On
        Keep_Log          Off

    [FILTER]
        Name   modify
        Match  kube.*
        Rename @timestamp when


    [OUTPUT]
        Name              es
        Match             kube.*
        Host              elasticsearch.default.svc.cluster.local
        Port              9200
        Index             kubernetes-logs
        #Logstash_Format   On
        #Logstash_Prefix   kubernetes-logs
        Retry_Limit       False
        Suppress_Type_Name On

  parsers.conf: |
    [PARSER]
        Name        docker
        Format      json
        Time_Key    time
        Time_Format %Y-%m-%dT%H:%M:%S.%L
        Time_Keep   On

