## To enable nginx metrics: 

1. go into nginx-service pod
2. open file /etc/nginx/conf.d/default.conf using vi/nano
3. add below location into :
```
server {
    listen       80;
    listen  [::]:80;
    server_name  localhost;

    # add this payload extra into it

    location /nginx_status {
        stub_status;

        access_log on;
        allow 127.0.0.1;
        allow all;
    }


   ... 
}

```

4. Now from same pod within run nginx commands below:

```sh
nginx -s reload
nginx -t

```

5. run `nginx -V 2>&1 | grep -o with-http_stub_status_module`

    responce must be `with-http_stub_status_module`

6. now check the metrics from `service/nginx-prometheus-exporter` on enabled port, in my case it is 9113, hence forwarding this port,
    `kubectl -n nginx port-forward service/nginx-prometheus-exporter 9088:9113`

    result of this must be: 
    ![alt text](image.png)