

You're looking to install Grafana OnCall as a service, similar to how you have Grafana itself set up. This typically involves two main components: the Grafana OnCall **plugin** for your Grafana instance and the Grafana OnCall **engine**.

**Important Note on Grafana OnCall OSS (Open Source Software):**

As of **March 11, 2025**, Grafana OnCall OSS has entered **maintenance mode** and will be **archived on March 24, 2026**. This means no new features will be developed, and certain cloud-connected features (like mobile app push notifications, SMS, and phone calls relying on Grafana Cloud) will cease to function after the archival date.

If you are setting up a new on-call system, it is **highly recommended to consider Grafana Cloud IRM (Incident Response & Management)**, which includes OnCall as a fully managed service and continues to receive active development. The Grafana Cloud Free Tier is available for up to 3 users.

However, if you still want to proceed with installing Grafana OnCall OSS on-premise, here's a general guide.

**Understanding the Components:**

* **Grafana OnCall Plugin:** This is what you install within your existing Grafana instance. It provides the UI for managing schedules, escalation chains, integrations, etc.
* **Grafana OnCall Engine:** This is a separate backend service that handles the logic, notifications, and communication with external systems. It needs to run independently.

**Installation Steps (for Grafana OnCall OSS):**

**1. Install the Grafana OnCall Plugin in Grafana:**

This is done directly from your Grafana CLI:

* **Open your terminal or command prompt.**
* **Run the Grafana CLI command:**
    ```bash
    grafana-cli plugins install grafana-oncall-app
    ```
    This will install the plugin into your Grafana plugins directory (default is `/var/lib/grafana/plugins`).
* **Restart your Grafana service** for the plugin to be recognized. The command for this varies based on your OS:
    * **systemd (most Linux distributions):** `sudo systemctl restart grafana-server`
    * **init.d (older Linux):** `sudo /etc/init.d/grafana-server restart`
    * **Windows:** Restart the Grafana service from the Services Manager.
* **Enable the plugin in Grafana:**
    * Log into your Grafana instance as an administrator.
    * Navigate to **Plugins** (usually in the left-hand menu).
    * Go to the **Apps** tab.
    * Select **Grafana OnCall**.
    * Click the **Config** tab and click **Enable**.

**2. Install and Configure the Grafana OnCall Engine as a Service:**

This is the more complex part, as Grafana OnCall Engine isn't typically installed with a simple `grafana-cli` command as a service. The recommended way for a production-like environment is using **Docker Compose** or **Helm charts (for Kubernetes)**.

Since you've installed Grafana as a service (implying a non-containerized setup), you'll likely want to run the OnCall engine as a separate service on your server. This usually involves:

* **Prerequisites:**
    * **Python:** The OnCall engine is a Python application. Ensure you have Python 3.x installed.
    * **Redis:** OnCall uses Redis for its broker.
    * **RabbitMQ (Optional, for Celery workers):** If you plan to scale or have complex notification queues, RabbitMQ is often used with Celery workers. Otherwise, Redis can handle it for smaller setups.
    * **Database:** OnCall requires a database (e.g., PostgreSQL, SQLite for hobby/testing).

* **Manual Setup (Conceptual Steps):**
    1.  **Clone the Grafana OnCall repository:**
        ```bash
        git clone https://github.com/grafana/oncall.git
        cd oncall/engine
        ```
    2.  **Configure Environment Variables:**
        The OnCall engine relies heavily on environment variables for configuration (e.g., database connection, Redis URI, secret keys). You'll need to create an `.env` file or set these directly in your service definition. Key variables include:
        * `DATABASE_TYPE` (e.g., `postgresql`, `sqlite3`)
        * `DATABASE_URL` (connection string for your database)
        * `BROKER_TYPE` (e.g., `redis`)
        * `REDIS_URI` (e.g., `redis://localhost:6379/0`)
        * `SECRET_KEY` (a strong, random string for security)
        * `BASE_URL` (the external URL where the OnCall engine will be accessible)
        * `GRAFANA_URL` (the URL of your Grafana instance)
        * `GRAFANA_API_KEY` (an API key created in Grafana for OnCall to communicate)
        * `ONCALL_API_URL` (the internal URL where the Grafana OnCall plugin can reach the engine)

    3.  **Install Python dependencies:**
        ```bash
        apt install python3-pip # install python if not
        sudo apt-get update
        sudo apt-get install libpq-dev #For this compilation, it requires the pg_config utility, which is part of the PostgreSQL development files

        pip install -r requirements.txt
        ```

    4.  **Run Database Migrations:**
        ```bash
        python manage.py migrate
        ```
    5.  **Create a Systemd Service File (Example for Linux):**
        You'll create a `.service` file (e.g., `/etc/systemd/system/grafana-oncall-engine.service`) to run the OnCall engine as a service.

        ```ini
        [Unit]
        Description=Grafana OnCall Engine
        After=network.target redis.service # Add your database service here if applicable

        [Service]
        User=grafana-oncall # Create a dedicated user for security
        Group=grafana-oncall # Create a dedicated group
        WorkingDirectory=/path/to/your/oncall/directory # Replace with your actual path
        EnvironmentFile=/path/to/your/oncall/.env # Path to your .env file
        ExecStart=/usr/bin/python3 /path/to/your/oncall/manage.py runserver 0.0.0.0:8080 # Or use a WSGI server like Gunicorn/uWSGI for production
        Restart=always
        StandardOutput=syslog
        StandardError=syslog
        SyslogIdentifier=grafana-oncall-engine

        [Install]
        WantedBy=multi-user.target
        ```
        **Important Considerations for `ExecStart`:**
        * For a production environment, you should use a **WSGI server** like Gunicorn or uWSGI to run the Django application (OnCall engine).
        * You'll also typically run **Celery workers** as separate services for handling background tasks and notifications. This means separate `ExecStart` commands for the web server and for the Celery workers.

    6.  **Reload Systemd and Start the Service:**
        ```bash
        sudo systemctl daemon-reload
        sudo systemctl start grafana-oncall-engine
        sudo systemctl enable grafana-oncall-engine
        ```
    7.  **Monitor Logs:**
        Check the service logs for any errors:
        ```bash
        sudo journalctl -u grafana-oncall-engine -f
        ```

**3. Configure Grafana OnCall Plugin to connect to the Engine:**

Once the OnCall engine is running and accessible (e.g., on port 8080), you need to configure the Grafana OnCall plugin to point to it:

* In your Grafana instance, navigate back to the **Grafana OnCall plugin configuration** (Plugins -> Apps -> Grafana OnCall -> Config).
* You'll see fields like "OnCall API URL". Enter the URL of your Grafana OnCall engine (e.g., `http://localhost:8080` if running on the same server, or `http://<oncall_engine_ip>:8080`).
* Click **Connect** or **Save** (depending on the exact UI).

**Important Security & Production Best Practices:**

* **Dedicated User/Group:** Run the OnCall engine under a dedicated, unprivileged user and group.
* **Database:** Use a robust database like PostgreSQL for production.
* **Reverse Proxy:** For production, put a reverse proxy like Nginx or Apache in front of your OnCall engine to handle TLS/SSL, caching, and load balancing.
* **WSGI Server:** Use a WSGI server (Gunicorn, uWSGI) for the Python application.
* **Celery Workers:** Implement Celery workers for robust background task processing (notifications, syncs). These should also be run as services.
* **Monitoring:** Monitor the OnCall engine and its dependencies (Redis, database, Celery) to ensure it's healthy.
* **Firewall:** Restrict access to the OnCall engine's port (e.g., 8080) to only your Grafana instance and necessary administration tools.
* **Secret Management:** Do not hardcode sensitive information (like API keys, database passwords) directly in service files. Use environment variables, a secrets manager, or proper configuration management tools.

Given the deprecation of Grafana OnCall OSS, consider if this is the right long-term solution for your needs. Grafana Cloud IRM offers a much simpler and more maintained path for on-call management.

Enable plugin oncal commands as a service:

```sh
curl -X POST 'http://admin:R3m%21G3s%40Broads%21d3@localhost:3000/api/plugins/grafana-oncall-app/settings' -H "Content-Type: application/json" -d '{"enabled":true, "jsonData":{"stackId":5, "orgId":100, "onCallApiUrl":"http://engine:8080", "grafanaUrl":"http://localhost:3000"}}'


curl -X POST 'http://admin:admin@localhost:3000/api/plugins/grafana-oncall-app/resources/plugin/install'

curl -X POST 'http://admin:R3m%21G3s%40Broads%21d3@localhost:3000/api/plugins/grafana-oncall-app/resources/plugin/install'

---

curl -X GET 'http://admin:admin@localhost:3000/api/plugins/grafana-oncall-app/resources/plugin/status'


curl -X GET 'http://admin:R3m%21G3s%40Broads%21d3@localhost:3000/api/plugins/grafana-oncall-app/resources/plugin/status'

curl -X POST 'http://admin:R3m%21G3s%40Broads%21d3@localhost:3000/api/plugins/grafana-oncall-app/resources/plugin/sync'


```