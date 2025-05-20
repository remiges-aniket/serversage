To install and configure the Prometheus PostgreSQL Exporter (`postgres_exporter`) as a systemd service on Ubuntu, follow these steps:

---

## 🧰 Prerequisites

* Ubuntu system with PostgreSQL installed
* Prometheus installed and running
* User with `sudo` privileges([Grafana Labs][1])

---

## 📥 Step 1: Download and Install `postgres_exporter`

1. **Create a directory for the exporter:**

   ```bash
   sudo mkdir -p /opt/postgres_exporter
   cd /opt/postgres_exporter
   ```

2. **Download the latest `postgres_exporter` binary:**

   Visit the [Releases page](https://github.com/prometheus-community/postgres_exporter/releases) to find the latest version. Replace `v0.17.1` with the desired version:

   ```bash
   wget https://github.com/prometheus-community/postgres_exporter/releases/download/v0.17.1/postgres_exporter-0.17.1.linux-amd64.tar.gz
   ```

3. **Extract and move the binary:**

   ```bash
    tar xvf postgres_exporter-0.17.1.linux-amd64.tar.gz
    cd postgres_exporter-0.17.1.linux-amd64
   ```

4. **Set appropriate permissions:**

   ```bash
   sudo chown root:root /usr/local/bin/postgres_exporter
   sudo chmod +x /usr/local/bin/postgres_exporter
   ```

---

## 🔐 Step 2: Configure PostgreSQL User for Exporter

1. **Create a PostgreSQL user for the exporter:**

   Connect to your PostgreSQL instance and run:

   ```sql
   CREATE USER postgres_exporter WITH PASSWORD 'yourpassword';
   ```

2. **Grant necessary permissions:**

   Still within the PostgreSQL prompt:

   ```sql
   GRANT CONNECT ON DATABASE yourdatabase TO postgres_exporter;
   GRANT USAGE ON SCHEMA public TO postgres_exporter;
   GRANT SELECT ON ALL TABLES IN SCHEMA public TO postgres_exporter;
   ```

   Adjust `yourdatabase` and schema as needed.

---

## 📝 Step 3: Set Up Environment Variables

1. **Create an environment file:**

   ```bash
   sudo nano /opt/postgres_exporter/postgres_exporter.env
   ```

2. **Add the following line:**

   ```bash
   DATA_SOURCE_NAME="postgresql://postgres_exporter:yourpassword@localhost:5432/yourdatabase?sslmode=disable"
   ```

   Replace `yourpassword` and `yourdatabase` with your actual PostgreSQL user's password and database name.

---

## 🛠️ Step 4: Create Systemd Service

1. **Create a systemd service file:**

   ```bash
   sudo nano /etc/systemd/system/postgres_exporter.service
   ```

   1.1. **Prerequisite Check:** This step applies only if the `pg_stat_statements` PostgreSQL extension is enabled and accessible to the user specified in the `.env` file.
    * For more details, refer to the PostgreSQL documentation on [`pg_stat_statements`](https://www.postgresql.org/docs/current/pgstatstatements.html).
      
      
   1.2. ***Create a custom queries file:***

      ```bash
      sudo nano /opt/postgres_exporter/queries.yaml
      ```
   1.3. ***add queries into file:***

   ```yaml
       pg_replication:
         query: |
           SELECT
             CASE
               WHEN NOT pg_is_in_recovery() THEN 0
               ELSE GREATEST(0, COALESCE(EXTRACT(EPOCH FROM (now() - pg_last_xact_replay_timestamp())), 0))
             END AS lag
         master: true
         metrics:
           - lag:
               usage: "GAUGE"
               description: "Replication lag behind master in seconds"

       pg_postmaster:
         query: |
           SELECT pg_postmaster_start_time() AS start_time_seconds
         master: true
         metrics:
           - start_time_seconds:
               usage: "GAUGE"
               description: "Time at which postmaster started"

       pg_database:
         query: |
           SELECT SUM(pg_database_size(pg_database.datname)) AS size
           FROM pg_database
         master: true
         metrics:
           - size:
               usage: "GAUGE"
               description: "Total database size in bytes"

       pg_stat_user_tables:
         query: |
           SELECT
             current_database() AS datname,
             schemaname,
             relname,
             seq_scan,
             seq_tup_read,
             idx_scan,
             idx_tup_fetch,
             n_tup_ins,
             n_tup_upd,
             n_tup_del,
             n_tup_hot_upd,
             n_live_tup,
             n_dead_tup,
             n_mod_since_analyze,
             COALESCE(last_vacuum, '1970-01-01Z') AS last_vacuum,
             COALESCE(last_autovacuum, '1970-01-01Z') AS last_autovacuum,
             COALESCE(last_analyze, '1970-01-01Z') AS last_analyze,
             COALESCE(last_autoanalyze, '1970-01-01Z') AS last_autoanalyze,
             vacuum_count,
             autovacuum_count,
             analyze_count,
             autoanalyze_count
           FROM pg_stat_user_tables
           LIMIT 100
         metrics:
           - datname:
               usage: "LABEL"
               description: "Database name"
           - schemaname:
               usage: "LABEL"
               description: "Schema name"
           - relname:
               usage: "LABEL"
               description: "Table name"
           - seq_scan:
               usage: "COUNTER"
               description: "Sequential scans initiated"
           - seq_tup_read:
               usage: "COUNTER"
               description: "Rows fetched by sequential scans"
           - idx_scan:
               usage: "COUNTER"
               description: "Index scans initiated"
           - idx_tup_fetch:
               usage: "COUNTER"
               description: "Rows fetched by index scans"
           - n_live_tup:
               usage: "GAUGE"
               description: "Estimated number of live rows"
           - n_dead_tup:
               usage: "GAUGE"
               description: "Estimated number of dead rows"
           - vacuum_count:
               usage: "COUNTER"
               description: "Manual vacuum count"
           - autovacuum_count:
               usage: "COUNTER"
               description: "Autovacuum count"

       pg_statio_user_tables:
         query: |
           SELECT
             current_database() AS datname,
             schemaname,
             relname,
             heap_blks_read,
             heap_blks_hit,
             idx_blks_read,
             idx_blks_hit,
             toast_blks_read,
             toast_blks_hit,
             tidx_blks_read,
             tidx_blks_hit
           FROM pg_statio_user_tables
           LIMIT 100
         metrics:
           - datname:
               usage: "LABEL"
               description: "Database name"
           - schemaname:
               usage: "LABEL"
               description: "Schema name"
           - relname:
               usage: "LABEL"
               description: "Table name"
           - heap_blks_read:
               usage: "COUNTER"
               description: "Disk blocks read from the table"
           - heap_blks_hit:
               usage: "COUNTER"
               description: "Buffer hits in the table"

       pg_stat_statements:
         query: |
           SELECT
             t2.rolname,
             t3.datname,
             query,
             calls,
             (total_exec_time) / 1000 AS total_time_seconds,
             (min_exec_time) / 1000 AS min_time_seconds,
             (max_exec_time) / 1000 AS max_time_seconds,
             (mean_exec_time) / 1000 AS mean_time_seconds,
             (stddev_exec_time) / 1000 AS stddev_time_seconds,
             rows,
             shared_blks_hit,
             shared_blks_read,
             shared_blks_dirtied,
             shared_blks_written
           FROM pg_stat_statements t1
           JOIN pg_roles t2 ON (t1.userid = t2.oid)
           JOIN pg_database t3 ON (t1.dbid = t3.oid)
           WHERE t2.rolname != 'rdsadmin'
           LIMIT 100
         master: true
         metrics:
           - rolname:
               usage: "LABEL"
               description: "User name"
           - datname:
               usage: "LABEL"
               description: "Database name"
           - query:
               usage: "LABEL"
               description: "SQL query"
           - calls:
               usage: "COUNTER"
               description: "Number of executions"
           - total_time_seconds:
               usage: "GAUGE"
               description: "Total execution time (seconds)"
           - shared_blks_hit:
               usage: "COUNTER"
               description: "Shared block cache hits"
           - shared_blks_read:
               usage: "COUNTER"
               description: "Shared blocks read"
           - shared_blks_written:
               usage: "COUNTER"
               description: "Shared blocks written"

       pg_never_used_indexes:
         query: |
           SELECT
             pi.schemaname,
             pi.relname,
             pi.indexrelname,
             pg_table_size(pi.indexrelid) AS index_size
           FROM pg_indexes pis
           JOIN pg_stat_user_indexes pi ON pis.schemaname = pi.schemaname
           LIMIT 100
         metrics:
           - schemaname:
               usage: "LABEL"
               description: "Schema name"
           - relname:
               usage: "LABEL"
               description: "Table name"
           - indexrelname:
               usage: "LABEL"
               description: "Index name"
           - index_size:
               usage: "GAUGE"
               description: "Size of index in bytes"

       # Commented-out queries (for reference)
       # pg_tablelocktops:
       #   query: |
       #     SELECT db.datname, relname AS tbname, mode AS locktype, COUNT(1) AS locknums
       #     FROM pg_database db
       #     JOIN pg_locks lk ON db.oid = lk.database
       #     JOIN pg_class cl ON lk.relation = cl.oid
       #     GROUP BY db.datname, relname, mode
       #     ORDER BY COUNT(1) DESC
       #     LIMIT 10
       #   metrics:
       #     - datname:
       #         usage: "LABEL"
       #         description: "Database name"
       #     - tbname:
       #         usage: "LABEL"
       #         description: "Table name"
       #     - locktype:
       #         usage: "LABEL"
       #         description: "Lock type"
       #     - locknums:
       #         usage: "COUNTER"
       #         description: "Number of locks"


   ```


2. **Add the following content:**

   ```ini
   [Unit]
   Description=Prometheus PostgreSQL Exporter
   After=network.target

   [Service]
   User=postgres
   Group=postgres
   EnvironmentFile=/opt/postgres_exporter/postgres_exporter.env
   ExecStart=/usr/local/bin/postgres_exporter  --extend.query-path=/opt/postgres_exporter/queries.yaml
   Restart=always

   [Install]
   WantedBy=multi-user.target
   ```

   Ensure that the `User` and `Group` match the PostgreSQL user.

3. **Reload systemd and start the service:**

   ```bash
   sudo systemctl daemon-reload
   sudo systemctl start postgres_exporter
   sudo systemctl enable postgres_exporter
   ```

4. **Check the service status:**

   ```bash
   sudo systemctl status postgres_exporter
   ```

---

## 📡 Step 5: Configure Prometheus to Scrape Metrics

1. **Edit Prometheus configuration:**

   ```bash
   sudo nano /etc/prometheus/prometheus.yml
   ```

2. **Add a new job under `scrape_configs`:**

   ```yaml
   scrape_configs:
     - job_name: 'postgres_exporter'
       static_configs:
         - targets: ['localhost:9187']
   ```

3. **Restart Prometheus:**

   ```bash
   sudo systemctl restart prometheus
   ```

---

## ✅ Verification

* Access the exporter metrics at [http://localhost:9187/metrics](http://localhost:9187/metrics)
* Check Prometheus targets at [http://localhost:9090/targets](http://localhost:9090/targets) to ensure the `postgres_exporter` is listed and up

---

For more detailed information and advanced configurations, refer to the official documentation:
👉 [Prometheus Community PostgreSQL Exporter](https://github.com/prometheus-community/postgres_exporter)

If you need assistance with setting up Grafana dashboards for PostgreSQL metrics visualization, feel free to ask!

[1]: https://grafana.com/oss/prometheus/exporters/postgres-exporter/?utm_source=chatgpt.com "Postgres exporter - Prometheus OSS - Grafana"
[2]: https://www.howtoforge.com/how-to-monitor-postgresql-with-prometheus-and-grafana/?utm_source=chatgpt.com "How to Monitor PostgreSQL with Prometheus and Grafana on ..."
