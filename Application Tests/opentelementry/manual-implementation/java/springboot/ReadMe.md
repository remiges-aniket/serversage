Run the project in docker using below command:
`docker-compose up --build`

Check out the sample Tomcat app below and play around with it to generate some load.

Access the services: Run both compose file from inside 1st then outside one

Application: http://localhost:8080/sample/
Jaeger UI: http://localhost:16686
Prometheus UI: http://localhost:9090
Grafana UI: http://localhost:3000

To stop the poject and remove docker containers:
`docker compose down`