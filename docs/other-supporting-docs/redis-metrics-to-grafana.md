
---

## Step 1: Configure Redis Datasource in Grafana  

1. Open Grafana Web UI at `http://localhost:3000`.  
2. Navigate to **Configuration** > **Data Sources**.  
3. Click **Add data source**.  
4. Search for **Redis** and select the Redis datasource plugin.  
5. Fill in your Redis connection details (e.g., host: `localhost`, port: `6379`).  
6. Click **Save & Test** to verify the connection.

---

## Step 2: Import Redis Dashboard into Grafana  

1. Download a Redis dashboard JSON file from the Grafana dashboards website or use a pre-existing one.  
2. In Grafana, go to **Dashboards** > **New** > **Import**.  
3. Upload the Redis dashboard JSON file.  
4. Select the Redis datasource you configured earlier.  
5. Click **Import**.

---

## Step 3: View Redis Metrics  

- Navigate to the imported Redis dashboard in Grafana.  
- Monitor Redis performance metrics such as memory usage, commands processed, connected clients, and more.

---

## References  
- [Monitoring Redis with Grafana - Medium Article](https://medium.com/@gargshivam1712/monitoring-redis-with-grafana-ac87ce8376ef)
