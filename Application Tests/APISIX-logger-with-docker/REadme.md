# Payload to enable api filter on port 9080 ( working ):
```
curl -X PUT http://localhost:9180/apisix/admin/routes/1 \
  -H 'X-API-KEY: admin123' \
  -H 'Content-Type: application/json' \
  -d '{
    "uri": "/init",
    "upstream": {
      "type": "roundrobin",
      "nodes": {
        "spring-boot:8081": 1
      }
    }
  }'
````

API to hit:
GET - http://localhost:9080/init   // success, port is from apisix
GET - http://localhost:9080/total_order   // failed


Enable log to es / opensearch:

# Global Logs for all in elastic (working):
```
curl http://localhost:9180/apisix/admin/global_rules/1 \
-H "X-API-KEY: admin123" -X PUT -d '
{
    "plugins": {
        "elasticsearch-logger": {
            "endpoint_addr": "http://elasticsearch:9200",
            "field": {
                "index": "apisix-uat-aws-logs"
            }
        }
    }
}'
````


# Enable bare minimun es logger for specific upstream ( working ):
```
curl http://localhost:9180/apisix/admin/routes/4 \
-H "X-API-KEY: admin123" -X PUT -d '
{
    "plugins":{
        "elasticsearch-logger":{
            "name":"elastic-logger",
            "endpoint_addr":"http://elasticsearch:9200",
            "field":{
                "index":"apisix-uat-aws-logs"
            }
        }
    },
    "upstream":{
        "type":"roundrobin",
        "nodes":{
            "spring-boot:8081":1
        }
    },
    "uri":"/*"
}'

```

Optional httpLogger :

curl http://127.0.0.1:9180/apisix/admin/routes/1 \
-H "X-API-KEY: admin123" -X PUT -d '
{
  "uri": "/*",
  "name": "http-logger",
  "plugins": {
    "http-logger": {
      "batch_max_size": 1,
      "concat_method": "json",
      "inactive_timeout": 1,
      "include_req_body": false,
      "include_resp_body": false,
      "max_retry_count": 0,
      "name": "http logger",
      "retry_delay": 1,
      "ssl_verify": false,
      "timeout": 3,
      "uri": "http://fluent-bit:24224"
    }
  },
  "upstream": {
    "nodes": [
      {
        "host": "spring-boot",
        "port": 8081,
        "weight": 1
      }
    ],
    "timeout": {
      "connect": 6,
      "send": 6,
      "read": 6
    },
    "type": "roundrobin",
    "scheme": "http",
    "pass_host": "pass",
    "keepalive_pool": {
      "idle_timeout": 60,
      "requests": 1000,
      "size": 320
    }
  },
  "status": 1
}'


Enable httpLogger:

curl http://127.0.0.1:9180/apisix/admin/routes/3 \
-H "X-API-KEY: admin123" -X PUT -d '
{
      "plugins": {
            "http-logger": {
                "uri": "http://192.168.160.6:24224"
            }
       },
      "upstream": {
           "type": "roundrobin",
           "nodes": {
               "spring-boot:8081": 1
           }
      },
      "uri": "/*"
}'


Enable log Format:

curl http://127.0.0.1:9180/apisix/admin/plugin_metadata/file-logger -H 'X-API-KEY: admin123' -X PUT -d '
{
  "log_format": {
        "remote_addr": "$remote_addr",
        "remote_user": "$remote_user",
        "@timestamp": "$time_iso8601",
        "request": "$request",
        "status": "$status",
        "body_bytes_sent": "$body_bytes_sent",
        "http_referer": "$http_referer",
        "http_user_agent": "$http_user_agent",
        "request_time": "$request_time",
        "upstream_connect_time": "$upstream_connect_time",
        "upstream_header_time": "$upstream_header_time",
        "upstream_response_time": "$upstream_response_time"
  }
}'


working 'fluet-bit.conf' on console:

```
[SERVICE]
    Flush         1
    Log_Level     info
    Daemon        off
    Parsers_File  parsers.conf

[INPUT]
    Name          http
    Port          24224
    Listen        0.0.0.0
    Tag           http.0

[OUTPUT]
    Name          stdout
    Match         http.0
```


