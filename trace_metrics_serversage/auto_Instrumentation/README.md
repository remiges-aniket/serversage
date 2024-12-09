# Example of Auto instrumentation of HTTP server + SQL database

This example shows a trace being generated which is composed of a http request and sql db handling -
both visible in the trace.

For testing auto instrumentation, we can use the docker compose.

To run the example, bring up the services using the command.

```
docker compose up
```

Now, you can hit the server using the below commands
```
curl localhost:8080/posts
curl localhost:8080/posts/1

```
Which will query the dummy postgres database before that make sure to run all queries from file 'posts_table_create.sql' from parent directory.

Every hit to the server should generate a trace that we can observe in [Jaeger UI](http://localhost:16686/)


Extra commands to clean and build again:
docker compose down --volumes
docker image prune -f
docker network prune -f
docker builder prune -f
docker compose up --build
